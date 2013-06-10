package au.com.mineauz.RegionSigns.manage;

import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.Region;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentMessage;
import au.com.mineauz.RegionSigns.rent.RentMessageTypes;
import au.com.mineauz.RegionSigns.rent.RentStatus;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class StopRentingMenu extends ValidatingPrompt implements ISubMenu
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
		
		boolean isOwner = false;
		Player player = ((Player)context.getForWhom());
		
		isOwner = status.Tenant.equals(player.getName());
		
		if(!isOwner && !player.hasPermission("regionsigns.rent.stop.others"))
		{
			player.sendMessage(ChatColor.RED + "You do not have permission to stop the rent of other players.");
			return mParent;
		}
		
		if(RegionSigns.config.minimumRentPeriod != 0 && (Calendar.getInstance().getTimeInMillis() - status.Date < RegionSigns.config.minimumRentPeriod) && !player.hasPermission("regionsigns.rent.nominperiod"))
		{
			if(isOwner)
				player.sendMessage(ChatColor.RED + "You cannot stop renting this region yet. You are required to rent for at least " + Util.formatTimeDifference(RegionSigns.config.minimumRentPeriod - (Calendar.getInstance().getTimeInMillis() - status.Date), 2, false) + " more");
			else
				player.sendMessage(ChatColor.RED + status.Tenant + " cannot stop renting this region yet. They are required to rent for at least " + Util.formatTimeDifference(RegionSigns.config.minimumRentPeriod - (Calendar.getInstance().getTimeInMillis() - status.Date), 2, false) + " more");
			
			return mParent;
		}
		
		if(!Util.playerHasEnough(status.Tenant, status.IntervalPayment))
		{
			if(isOwner)
				player.sendMessage(ChatColor.RED + "You cannot stop renting this region yet. You cannot afford to pay your next rent.");
			else
				player.sendMessage(ChatColor.RED + status.Tenant + " cannot stop renting this region yet. They cannot afford to pay their next rent.");
			
			return mParent;
		}
		
		Util.playerSubtractMoney(status.Tenant, status.IntervalPayment);
		
		// Give the money to the owners
		ProtectedRegion pRegion = new Region(Bukkit.getWorld(status.World), status.Region).getProtectedRegion();
		if(pRegion.getOwners().size() > 0 && status.IntervalPayment > 0)
		{
			double amountEach = status.IntervalPayment / pRegion.getOwners().size();
			
			for(String playerName : pRegion.getOwners().getPlayers())
			{
				Util.playerAddMoney(Bukkit.getOfflinePlayer(playerName), amountEach);
				RentMessage paymentReceived = new RentMessage();
				paymentReceived.Type = RentMessageTypes.PaymentReceived;
				paymentReceived.EventCompletionTime = 0;
				paymentReceived.Region = status.Region;
				paymentReceived.Payment = amountEach;
				
				RentManager.instance.sendMessage(paymentReceived, Bukkit.getOfflinePlayer(playerName));
			}
		}
		
		// They are required to pay their next rent and will be removed from the lot at the next rent interval
		RentMessage msg = new RentMessage();
		msg.Type = RentMessageTypes.RentEnding;
		msg.EventCompletionTime = status.NextIntervalEnd;
		msg.Region = status.Region;
		msg.Payment = status.IntervalPayment;
		
		RentMessage msg2 = new RentMessage();
		msg2.Type = RentMessageTypes.RentEndingLandlord;
		msg2.EventCompletionTime = status.NextIntervalEnd;
		msg2.Region = status.Region;
		msg2.Payment = status.IntervalPayment;
		
		status.PendingRemoval = true;
		RentManager.instance.sendMessage(msg, Bukkit.getOfflinePlayer(status.Tenant));
		RentManager.instance.sendMessageToLandlords(msg2, status);
		
		player.sendMessage(ChatColor.GREEN + status.Tenant + " ended renting '" + status.Region + "'. They will be removed once their rent period is up");
		
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
