package au.com.mineauz.RegionSigns.rent;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RentTransferConfirmation 
{
	class ConfirmationPrompt extends StringPrompt
	{
		@Override
		public String getPromptText(ConversationContext context) 
		{
			RentStatus rent = (RentStatus)context.getSessionData("rs");
			CommandSender requester = (CommandSender)context.getSessionData("req");
			Player newTenant = (Player)context.getSessionData("new");
			boolean failed = (Boolean)context.getSessionData("fail");
			
			if(failed)
			{
				return "Please enter 'yes' or 'no'";
			}
			else
			{
				switch((Integer)context.getSessionData("stage"))
				{
				case 0:
					return "A tenantship transfer of " + ChatColor.YELLOW + rent.Region + ChatColor.WHITE + " has been requested by " + 
						ChatColor.GREEN + requester.getName() + ChatColor.WHITE + ", to transfer to " + 
						ChatColor.GREEN + newTenant.getName() + ChatColor.WHITE + ".\nDo you wish to accept? (Type yes or no)";
				case 1:
					return "A tenantship transfer of " + ChatColor.YELLOW + rent.Region + ChatColor.WHITE + " to you has been requested.\n" + 
						"You will be required to pay the rent of " + ChatColor.YELLOW + Util.formatCurrency(rent.IntervalPayment) + ChatColor.WHITE + 
						" every " + ChatColor.YELLOW + Util.formatTimeDifference(rent.RentInterval, 2, true) + ChatColor.WHITE + " with your first payment due in " + 
						ChatColor.YELLOW + Util.formatTimeDifference(rent.NextIntervalEnd - Calendar.getInstance().getTimeInMillis(), 2, false) + ChatColor.WHITE +
						(RegionSigns.config.minimumRentPeriod != 0 && (Calendar.getInstance().getTimeInMillis() - rent.Date < RegionSigns.config.minimumRentPeriod) ? " and are required to rent for at least " + ChatColor.YELLOW + Util.formatTimeDifference(RegionSigns.config.minimumRentPeriod - (Calendar.getInstance().getTimeInMillis() - rent.Date), 2, true) + ChatColor.WHITE : "" ) +
						".\nDo you wish to accept? (Type yes or no)";
				}
			}
			return "Shouldnt be here";
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) 
		{
			if(input.compareToIgnoreCase("yes") == 0)
			{
				if((Integer)context.getSessionData("stage") == 0)
				{
					// Notify the requester of the status
					((CommandSender)context.getSessionData("req")).sendMessage(((Player)context.getSessionData("cur")).getName() + " has accepted the transfer request. Now waiting on " + ((Player)context.getSessionData("new")).getName() + " to accept"); 
					// Transfer the conversation to the new tenant
					((RentTransferConfirmation)context.getSessionData("rtc")).transferConvo();
					return Prompt.END_OF_CONVERSATION;
				}
				else
				{
					RentStatus rent = (RentStatus)context.getSessionData("rs");
					CommandSender requester = (CommandSender)context.getSessionData("req");
					Player curTenant = (Player)context.getSessionData("cur");
					Player newTenant = (Player)context.getSessionData("new");
					Location signLocation = (Location)context.getSessionData("sign");
					
					World world = RegionSigns.instance.getServer().getWorld(rent.World);
					
					ProtectedRegion region = Util.getRegion(world, rent.Region);
					
					if(region == null)
					{
						requester.sendMessage(ChatColor.RED + "An Error occured transfering " + rent.Region + ". The region no longer exists");
						curTenant.sendMessage(ChatColor.RED + "An Error occured transfering " + rent.Region);
						newTenant.sendMessage(ChatColor.RED + "An Error occured transfering " + rent.Region);
						return Prompt.END_OF_CONVERSATION;
					}
					
					// Transfer the actual region
					region.getMembers().removePlayer(rent.Tenant);
					region.getMembers().addPlayer(newTenant.getName());
					rent.Tenant = newTenant.getName();
					
					// Attempt to save
					if(!Util.saveRegionManager(world))
					{
						requester.sendMessage(ChatColor.RED + "An Internal Error occured transfering " + rent.Region + ".");
						curTenant.sendMessage(ChatColor.RED + "An Error occured transfering " + rent.Region);
						newTenant.sendMessage(ChatColor.RED + "An Error occured transfering " + rent.Region);
						return Prompt.END_OF_CONVERSATION;
					}
					
					if(signLocation == null)
						signLocation = rent.SignLocation;
					
					// Update the sign
					if(signLocation != null && (signLocation.getBlock().getType() == Material.WALL_SIGN || signLocation.getBlock().getType() == Material.SIGN_POST))
					{
						Sign signState = (Sign)signLocation.getBlock().getState();
						
						String[] lines = signState.getLines();
						
						for(int i = 0; i < 4; ++i)
						{
							lines[i] = RegionSigns.config.claimSign[i];
							lines[i] = lines[i].replaceAll("<user>", newTenant.getName());
							lines[i] = lines[i].replaceAll("<region>", rent.Region);
						}
						
						// Change the sign text
						for(int i = 0; i < 4; i++)
							signState.setLine(i, lines[i]);
						
						signState.update(true);
					}
					
					requester.sendMessage(rent.Region + " has now been transfered to " + ((Player)context.getSessionData("new")).getName());
					newTenant.sendMessage("You are now the tenant of " + rent.Region);
					curTenant.sendMessage("You are no longer renting " + rent.Region);

					return Prompt.END_OF_CONVERSATION;
				}
			}
			else if (input.compareToIgnoreCase("no") == 0)
			{
				if((Integer)context.getSessionData("stage") == 0)
					((CommandSender)context.getSessionData("req")).sendMessage(((Player)context.getSessionData("cur")).getName() + " has refused the transfer request.");
				else
					((CommandSender)context.getSessionData("req")).sendMessage(((Player)context.getSessionData("new")).getName() + " has refused the transfer request.");
			
				return Prompt.END_OF_CONVERSATION;
			}
			else
			{
				context.setSessionData("fail", true);
				// Invalid input
				return this;
			}
		}
		
	}
	
	RentStatus mRentStatus;
	Player mCurrentTenant;
	Player mNewTenant;
	CommandSender mRequester;
	Location mSignLocation;
	
	public RentTransferConfirmation(RentStatus rentStatus, Player currentTenant, Player newTenant, CommandSender requester)
	{
		this(rentStatus, currentTenant, newTenant, requester, null);
	}
	
	public RentTransferConfirmation(RentStatus rentStatus, Player currentTenant, Player newTenant, CommandSender requester, Location signLocation)
	{
		mRentStatus = rentStatus;
		mCurrentTenant = currentTenant;
		mNewTenant = newTenant;
		mRequester = requester;
		mSignLocation = signLocation;
		
		Map<Object, Object> sessionData = new HashMap<Object, Object>();
		sessionData.put("rs", rentStatus);
		sessionData.put("cur", currentTenant);
		sessionData.put("new", newTenant);
		sessionData.put("req", requester);
		sessionData.put("stage", 0);
		sessionData.put("rtc", this);
		sessionData.put("fail",false);
		sessionData.put("sign",mSignLocation);
		
		ConversationFactory factory = new ConversationFactory(RegionSigns.instance)
			.withTimeout(20)
			.withModality(false)
			.withInitialSessionData(sessionData)
			.withLocalEcho(false)
			.withFirstPrompt(new ConfirmationPrompt());
		
		
		factory.buildConversation(currentTenant).begin();
	}
	
	// Transfer the conversation to the new tenant
	private void transferConvo()
	{
		Map<Object, Object> sessionData = new HashMap<Object, Object>();
		sessionData.put("rs", mRentStatus);
		sessionData.put("cur", mCurrentTenant);
		sessionData.put("new", mNewTenant);
		sessionData.put("req", mRequester);
		sessionData.put("stage", 1);
		sessionData.put("rtc", this);
		sessionData.put("fail",false);
		sessionData.put("sign",mSignLocation);
		
		ConversationFactory factory = new ConversationFactory(RegionSigns.instance)
			.withTimeout(20)
			.withModality(false)
			.withInitialSessionData(sessionData)
			.withLocalEcho(false)
			.withFirstPrompt(new ConfirmationPrompt());
		
		
		factory.buildConversation(mNewTenant).begin();
	}
}
