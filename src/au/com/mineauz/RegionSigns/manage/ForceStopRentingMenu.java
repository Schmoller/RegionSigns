package au.com.mineauz.RegionSigns.manage;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

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
		
		if(region.isOwner(player.getName()))
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
		
		isOwner = status.Tenant.getName().equals(player.getName());
		
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

		RentManager.instance.sendMessage(msg,status.Tenant);
		player.sendMessage(ChatColor.GREEN + status.Tenant.getName() + " finished renting '" + status.Region + "' and has been removed from it");
		
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
