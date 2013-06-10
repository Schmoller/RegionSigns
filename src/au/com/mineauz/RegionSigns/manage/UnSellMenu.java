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
import au.com.mineauz.RegionSigns.rent.RentStatus;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class UnSellMenu extends ValidatingPrompt implements ISubMenu
{
	private Prompt mParent = null;
	
	@Override
	public void setParent( Prompt parent )
	{
		mParent = parent;
	}
	
	@Override
	protected Prompt acceptValidatedInput( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back") || input.equalsIgnoreCase("no"))
			return mParent;
		
		Location signLocation = (Location)context.getSessionData("sign");
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		
		RentStatus status = (RentStatus)context.getSessionData("status");
		if(signLocation.getBlock().getType() != Material.WALL_SIGN && signLocation.getBlock().getType() != Material.SIGN_POST)
		{
			((Player)context.getForWhom()).sendMessage(ChatColor.RED + "Unable to stop selling region, the sign was removed.");
			return mParent;
		}
		
		Sign sign = (Sign)signLocation.getBlock().getState();
		
		if(status != null)
		{
			sign.setLine(0, RegionSigns.config.rentSign[0].replaceAll("<user>", status.Tenant).replaceAll("<region>", status.Region));
			sign.setLine(1, RegionSigns.config.rentSign[1].replaceAll("<user>", status.Tenant).replaceAll("<region>", status.Region));
			sign.setLine(2, RegionSigns.config.rentSign[2].replaceAll("<user>", status.Tenant).replaceAll("<region>", status.Region));
			sign.setLine(3, RegionSigns.config.rentSign[3].replaceAll("<user>", status.Tenant).replaceAll("<region>", status.Region));
		}
		else
		{
			String primaryOwner = "Unowned";
			for(String owner : region.getOwners().getPlayers())
			{
				primaryOwner = owner;
				break;
			}
			
			sign.setLine(0, RegionSigns.config.claimSign[0].replaceAll("<user>", primaryOwner).replaceAll("<region>", region.getId()));	
			sign.setLine(1, RegionSigns.config.claimSign[1].replaceAll("<user>", primaryOwner).replaceAll("<region>", region.getId()));
			sign.setLine(2, RegionSigns.config.claimSign[2].replaceAll("<user>", primaryOwner).replaceAll("<region>", region.getId()));
			sign.setLine(3, RegionSigns.config.claimSign[3].replaceAll("<user>", primaryOwner).replaceAll("<region>", region.getId()));
		}
		
		sign.update(true);

		((Player)context.getForWhom()).sendMessage(ChatColor.YELLOW + region.getId() + ChatColor.WHITE + " is no longer for sale.");
		
		return Prompt.END_OF_CONVERSATION;
	}
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		if(input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("no"))
			return true;
		
		return false;
	}
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		return "Do you want to stop " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + " from being sold? (type yes or no) "; 
	}

}
