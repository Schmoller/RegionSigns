package au.com.mineauz.RegionSigns.manage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentMessage;
import au.com.mineauz.RegionSigns.rent.RentMessageTypes;
import au.com.mineauz.RegionSigns.rent.RentStatus;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ForceStopRentingMenu extends ValidatingPrompt implements ISubMenu
{
	private Prompt mParent;
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		Player player = ((Player)context.getForWhom());
		RentStatus status = (RentStatus)context.getSessionData("status");
		
		if(status.Tenant.equals(player.getName()))
			return "Do you want to stop renting " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + "?\nYou will no longer be able to access anything in the region. (Enter yes or no)";
		else
			return "Do you want to force " + ChatColor.YELLOW + Util.makeNameList(region.getMembers().getPlayers()) + ChatColor.WHITE + " to stop renting " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + "? (Enter yes or no)";
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
		
		RentStatus status = (RentStatus)context.getSessionData("status");
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		
		boolean isOwner = false;
		Player player = ((Player)context.getForWhom());
		
		isOwner = status.Tenant.equals(player.getName());
		
		if(!isOwner && !player.hasPermission("regionsigns.rent.forcestop.others"))
		{
			player.sendMessage(ChatColor.RED + "You do not have permission to forcestop the rent of other players.");
			return mParent;
		}
		
		region.setMembers(new DefaultDomain());

		// remove the rent status
		RentManager.instance.removeRent(status);
		
		// The lease is terminated now
		RentMessage msg = new RentMessage();
		msg.Type = RentMessageTypes.RentEnded;
		msg.EventCompletionTime = 0;
		msg.Region = status.Region;
		msg.Payment = 0;
		
		RentMessage msg2 = new RentMessage();
		msg2.Type = RentMessageTypes.RentEndedLandlord;
		msg2.EventCompletionTime = 0;
		msg2.Region = status.Region;
		msg2.Payment = 0;

		RentManager.instance.sendMessage(msg,Bukkit.getOfflinePlayer(status.Tenant));
		RentManager.instance.sendMessageToLandlords(msg2,status);
		player.sendMessage(ChatColor.GREEN + status.Tenant + " finished renting '" + status.Region + "' and has been removed from it");
		
		// Update the sign
		if(status.SignLocation != null && (status.SignLocation.getBlock().getType() == Material.WALL_SIGN || status.SignLocation.getBlock().getType() == Material.SIGN_POST))
		{
			Sign sign = (Sign)status.SignLocation.getBlock().getState();
			
			for(int i = 0; i < 4; ++i)
			{
				String line = RegionSigns.config.unclaimedSign[i];
				line = line.replaceAll("<region>", status.Region);
				
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
