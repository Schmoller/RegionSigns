package au.com.mineauz.RegionSigns.manage;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class UnclaimMenu extends ValidatingPrompt implements ISubMenu
{
	private Prompt mParent;
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		Player player = ((Player)context.getForWhom());
		
		if(region.isOwner(player.getName()))
			return "Do you want to unclaim " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + "?\nYou will no longer be able to access anything in the region. (Enter yes or no)";
		else
			return "Do you want to force " + ChatColor.YELLOW + Util.makeNameList(region.getOwners().getPlayers()) + ChatColor.WHITE + " to unclaim " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + "? (Enter yes or no)";
	}

	@Override
	public void setParent( Prompt parent )
	{
		mParent = parent;
	}

	@Override
	protected Prompt acceptValidatedInput( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return mParent;
		
		if(input.equalsIgnoreCase("no"))
			return mParent;
		
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		boolean isOwner = false;
		Player player = ((Player)context.getForWhom());
		
		isOwner = region.isOwner(player.getName());
		
		region.setOwners(new DefaultDomain());
		region.setMembers(new DefaultDomain());
		
		if(isOwner)
			player.sendMessage("You have unclaimed " + ChatColor.YELLOW + region.getId());
		else
			player.sendMessage(ChatColor.YELLOW + region.getId() + ChatColor.WHITE + " has been unclaimed");
		
		// Replace the sign
		Location signLocation = (Location)context.getSessionData("sign");
		
		if(signLocation.getBlock().getType() == Material.WALL_SIGN || signLocation.getBlock().getType() == Material.SIGN_POST)
		{
			Sign sign = (Sign)signLocation.getBlock().getState();
			
			for(int i = 0; i < 4 && i < RegionSigns.config.unclaimedSign.length; ++i)
			{
				String line = RegionSigns.config.unclaimedSign[i];
				line = line.replaceAll("<region>", region.getId());
				
				sign.setLine(i, line);
			}
			
			sign.update(true);
		}
		
		return Prompt.END_OF_CONVERSATION;
	}

	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		if(input.equalsIgnoreCase("no"))
			return true;
		
		if(input.equalsIgnoreCase("yes"))
			return true;
		
		return false;
	}

}
