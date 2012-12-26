package com.gmail.steven.schmoll.RegionSigns;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RentConfirmation implements ConversationAbandonedListener
{
	class ConfirmationPrompt extends StringPrompt
	{
		@Override
		public String getPromptText(ConversationContext context) 
		{
			ProtectedRegion region = (ProtectedRegion)context.getSessionData("r");
			//Player newTenant = (Player)context.getSessionData("new");
			double downPayment = (Double)context.getSessionData("down");
			double intPayment = (Double)context.getSessionData("intPay");
			long interval = (Long)context.getSessionData("int");
			//InteractableSignState sign = (InteractableSignState)context.getSessionData("sign");
			boolean failed = (Boolean)context.getSessionData("fail");

			if(failed)
			{
				return "Please enter 'yes' or 'no'";
			}
			else
			{
				return "Do you wish to rent " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + "?\n" +  
						"You will be required to pay " + ChatColor.YELLOW + Util.formatCurrency(downPayment) + ChatColor.WHITE + 
						" now and the rent of " + ChatColor.YELLOW + Util.formatCurrency(intPayment) + ChatColor.WHITE + 
						" every " + ChatColor.YELLOW + Util.formatTimeDifference(interval, 2, true) + ChatColor.WHITE +
						(RentSystem.MinimumRentPeriod != 0 ? " and are required to rent for at least " + ChatColor.YELLOW + Util.formatTimeDifference(RentSystem.MinimumRentPeriod, 2, false) + ChatColor.WHITE : "" ) +
						".\n(Type yes or no)";
			}
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) 
		{
			if(input.compareToIgnoreCase("yes") == 0)
			{
				ProtectedRegion region = (ProtectedRegion)context.getSessionData("r");
				Player player = (Player)context.getSessionData("new");
				double downPayment = (Double)context.getSessionData("down");
				double intPayment = (Double)context.getSessionData("intPay");
				long interval = (Long)context.getSessionData("int");
				InteractableSignState sign = (InteractableSignState)context.getSessionData("sign");
				
				RentSign.Reason reason = RentSign.canUserRent(player, region, sign);
				
				if(reason != RentSign.Reason.Ok)
					player.sendMessage(ChatColor.RED + RentSign.getDisplayMessage(reason));
				else
				{
					if(sign.SignLocation.getBlock().getType() != Material.SIGN_POST && sign.SignLocation.getBlock().getType() != Material.WALL_SIGN)
						player.sendMessage(ChatColor.RED + "Unable to rent region. It is no longer available for rent");
					else
					{
						// Handle payment
						if(downPayment > 0)
						{
							try
							{
								// Do the payment
								Economy.subtract(player.getName(), downPayment);
							}
							catch(NoLoanPermittedException e)
							{
								// Not possible
								e.printStackTrace();
							}
							catch(UserDoesNotExistException e)
							{
								// Not possible
								e.printStackTrace();
							}
						}
					
						RentMessage beginMessage = new RentMessage();
						beginMessage.Region = region.getId();
						beginMessage.Payment = downPayment;
						beginMessage.EventCompletionTime = 0;
						beginMessage.Type = (downPayment > 0 ? RentMessageTypes.RentBegin : RentMessageTypes.RentBeginFree);
						
						RentMessage firstPaymentMessage = new RentMessage();
						firstPaymentMessage.Region = region.getId();
						firstPaymentMessage.Payment = intPayment;
						firstPaymentMessage.EventCompletionTime = Calendar.getInstance().getTimeInMillis() + interval;
						firstPaymentMessage.Type = RentMessageTypes.FirstPaymentTime;
						
						player.sendMessage(RegionSigns.instance.getRentSystem().getDisplayMessage(beginMessage));
						
						if(intPayment > 0)
						{
							player.sendMessage(RegionSigns.instance.getRentSystem().getDisplayMessage(firstPaymentMessage));
						}
						
						player.sendMessage(ChatColor.GREEN + "[Rent] " + ChatColor.WHITE + "Use '/rent help' for a list of rent commands");
						
						try
						{
							if(!Economy.hasEnough(player.getName(), intPayment))
							{
								// Send a warning
								RentMessage warning = new RentMessage();
								warning.Region = region.getId();
								warning.Payment = intPayment;
								warning.EventCompletionTime = Calendar.getInstance().getTimeInMillis() + interval;
								warning.Type = RentMessageTypes.InsufficientFunds;
								
								player.sendMessage(RegionSigns.instance.getRentSystem().getDisplayMessage(warning));
							}
						} catch(UserDoesNotExistException e) {}
						
						RentStatus status = new RentStatus();
						status.Region = region.getId();
						status.World = sign.SignLocation.getWorld().getName();
						status.Tenant = player.getName();
						status.IntervalPayment = intPayment;
						status.RentInterval = interval;
						status.NextIntervalEnd = Calendar.getInstance().getTimeInMillis() + interval;
						status.Date = Calendar.getInstance().getTimeInMillis();
						status.PendingEviction = false;
	
						RegionSigns.instance.getRentSystem().RegisterRenter(status);
						// Add the player to the 
						region.getMembers().addPlayer(player.getName());
						try {
							RegionSigns.worldGuard.getRegionManager(sign.SignLocation.getWorld()).save();
						} catch (ProtectionDatabaseException e) {
						}
						
						// Change the sign text
						Sign signState = (Sign)sign.SignLocation.getBlock().getState();
						
						String line = RegionSigns.instance.getConfig().getString("claim.sign.line1","");
						line = line.replaceAll("<user>", player.getName());
						line = line.replaceAll("<region>", region.getId());
						signState.setLine(0, line);
	
						line = RegionSigns.instance.getConfig().getString("claim.sign.line2","");
						line = line.replaceAll("<user>", player.getName());
						line = line.replaceAll("<region>", region.getId());
						signState.setLine(1, line);
						
						line = RegionSigns.instance.getConfig().getString("claim.sign.line3","");
						line = line.replaceAll("<user>", player.getName());
						line = line.replaceAll("<region>", region.getId());
						signState.setLine(2, line);
						
						line = RegionSigns.instance.getConfig().getString("claim.sign.line4","");
						line = line.replaceAll("<user>", player.getName());
						line = line.replaceAll("<region>", region.getId());
						signState.setLine(3, line);
						
						signState.update(true);
					}
				}
				context.setSessionData("end", true);
				return Prompt.END_OF_CONVERSATION;
			}
			else if (input.compareToIgnoreCase("no") == 0)
			{
				context.setSessionData("end", false);
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
	
	ProtectedRegion mRegion;
	Player mNewTenant;
	double mDownPayment;
	double mIntervalPayment;
	long mInterval;
	
	public RentConfirmation(ProtectedRegion region, Player newTenant, InteractableSignState sign)
	{
		mRegion = region;
		mNewTenant = newTenant;
		
		mDownPayment = (Double)sign.Argument2;
		
		// Parse the cost per period and the period length
		if(((String)sign.Argument3).indexOf(":") != -1)
		{
			// This does contain the period
			String perIntervalPriceString = ((String)sign.Argument3).substring(0,((String)sign.Argument3).indexOf(":"));
			String intervalLength = ((String)sign.Argument3).substring(((String)sign.Argument3).indexOf(":")+1);
			
			// There is one char at the beginning that is the currency symbol
			if(perIntervalPriceString.startsWith(RegionSigns.instance.getServer().getPluginManager().getPlugin("Essentials").getConfig().getString("currency-symbol")))
			{
				try 
				{ 
					mIntervalPayment = Double.parseDouble(perIntervalPriceString.substring(1));
				} 
				catch(NumberFormatException e){}
			}
			else
			{
				// Maybe its just the number
				try 
				{ 
					mIntervalPayment = Double.parseDouble(perIntervalPriceString);
				} 
				catch(NumberFormatException e){}
			}
			
			// attempt to parse the period
			try
			{
				mInterval = Util.parseDateDiff(intervalLength);
			}
			catch(Exception e){}
		}
		else
		{
			// Just the cost
			// There is one char at the beginning that is the currency symbol
			if(((String)sign.Argument3).startsWith(RegionSigns.instance.getServer().getPluginManager().getPlugin("Essentials").getConfig().getString("currency-symbol")))
			{
				try 
				{ 
					mIntervalPayment = Double.parseDouble(((String)sign.Argument3).substring(1));
				} 
				catch(NumberFormatException e){}
			}
			else
			{
				// Maybe its just the number
				try 
				{ 
					mIntervalPayment = Double.parseDouble(((String)sign.Argument3));
				} 
				catch(NumberFormatException e){}
			}
			
			try
			{
				mInterval = Util.parseDateDiff(RegionSigns.instance.getConfig().getString("rent.defaultperiod"));
			}
			catch(Exception e){}
		}
		
		
		Map<Object, Object> sessionData = new HashMap<Object, Object>();
		sessionData.put("r", region);
		sessionData.put("new", newTenant);
		sessionData.put("down", mDownPayment);
		sessionData.put("intPay", mIntervalPayment);
		sessionData.put("int", mInterval);
		sessionData.put("sign", sign);
		sessionData.put("fail", false);
		
		ConversationFactory factory = new ConversationFactory(RegionSigns.instance)
			.withTimeout(20)
			.withModality(false)
			.withInitialSessionData(sessionData)
			.withLocalEcho(false)
			.addConversationAbandonedListener(this)
			.withFirstPrompt(new ConfirmationPrompt());
		
		factory.buildConversation(newTenant).begin();
	}

	@Override
	public void conversationAbandoned(ConversationAbandonedEvent event) 
	{
		if(event.getContext().getSessionData("end") == null)
		{
			mNewTenant.sendMessage(ChatColor.RED + "You did not answer in 20 seconds.\n" + ChatColor.WHITE + "Please click the sign again if you still wish to rent " + ChatColor.YELLOW + ((ProtectedRegion)event.getContext().getSessionData("r")).getId());
		}
		else
		{
			if(!(Boolean)event.getContext().getSessionData("end"))
			{
				mNewTenant.sendMessage(ChatColor.WHITE + "You have decided not to rent " + ChatColor.YELLOW + ((ProtectedRegion)event.getContext().getSessionData("r")).getId());
			}
		}
	}
}
