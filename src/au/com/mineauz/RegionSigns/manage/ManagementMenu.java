package au.com.mineauz.RegionSigns.manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.OutOfRangeCanceller;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentStatus;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ManagementMenu implements ConversationAbandonedListener
{
	private ProtectedRegion mRegion;
	private Player mPlayer;
	private RentStatus mStatus;
	private Location mSignLocation;
	private boolean mIsForSale;
	
	public ManagementMenu(ProtectedRegion region, Location signLocation, Player player, RentStatus status, boolean forSale)
	{
		mRegion = region;
		mPlayer = player;
		mStatus = status;
		mSignLocation = signLocation;
		mIsForSale = forSale;
	}
	
	public void show()
	{
		Map<Object, Object> sessionData = new HashMap<Object, Object>();
		sessionData.put("player", mPlayer);
		sessionData.put("region", mRegion);
		sessionData.put("status", mStatus);
		sessionData.put("sign", mSignLocation);
		
		MainPrompt prompt = new MainPrompt();
		prompt.addOption("Add Player", new AddPlayerMenu());
		prompt.addOption("Remove Player", new RemovePlayerMenu());
		prompt.addOption("Clear Players", new ClearPlayerMenu());
		
		if(mStatus == null)
		{
			prompt.addOption("Unclaim Lot", new UnclaimMenu());
			if(mIsForSale)
				prompt.addOption("Stop Selling Lot", new UnSellMenu());
			else
				prompt.addOption("Sell Lot", new SellMenu());
		}
		else
		{
			prompt.addOption("Stop Renting", new StopRentingMenu());
			if(mPlayer.hasPermission("regionsigns.rent.forcestop"))
				prompt.addOption("Force Stop Renting", new ForceStopRentingMenu());
			if(mPlayer.hasPermission("regionsigns.rent.transfer"))
				prompt.addOption("Transfer", new TransferRentMenu());
			if(mPlayer.hasPermission("regionsigns.rent.set"))
				prompt.addOption("Set Rent", new SetRentMenu());
		}
		
		prompt.addOption("Change Options", new OptionMenu(mPlayer));
		
		
		
		ConversationFactory factory = new ConversationFactory(RegionSigns.instance)
			.withTimeout(20)
			.withModality(false)
			.withInitialSessionData(sessionData)
			.withLocalEcho(false)
			.withConversationCanceller(new OutOfRangeCanceller(RegionSigns.instance, mSignLocation, 5))
			.addConversationAbandonedListener(this)
			.withFirstPrompt(prompt);
		
		factory.buildConversation(mPlayer).begin();
	}
	
	static class Option
	{
		public String title;
		public ISubMenu subMenu;
	}
	
	class MainPrompt extends StringPrompt
	{
		private ArrayList<Option> mOptions = new ArrayList<Option>();
		
		public void addOption(String option, ISubMenu subMenu)
		{
			Option o = new Option();
			o.title = option;
			o.subMenu = subMenu;
			
			if(subMenu != null)
				subMenu.setParent(this);
			
			mOptions.add(o);
		}
		
		@Override
		public String getPromptText(ConversationContext context) 
		{
//			Player player = (Player)context.getSessionData("player");
//			ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
//			RentStatus status = (RentStatus)context.getSessionData("status");
			
			String output = "What would you like to do? (enter the number)\n";
			
			int index = 1;
			for(Option option : mOptions)
			{
				output += ChatColor.YELLOW + "" + index + ": " + ChatColor.WHITE + option.title + "\n";
				++index;
			}
			output += ChatColor.YELLOW + "0: " + ChatColor.WHITE + "Exit\n";
			return output;
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) 
		{
			if(input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit"))
				return Prompt.END_OF_CONVERSATION;
			
			try
			{
				int number = Integer.parseInt(input);
				
				if(number == 0)
					return Prompt.END_OF_CONVERSATION;
				
				if(number >=1 && number <= mOptions.size())
				{
					Option option = mOptions.get(number-1);
					if(option.subMenu != null)
					{
						return (Prompt)option.subMenu;
					}
				}
				
				return this;
			}
			catch(NumberFormatException e)
			{
				return this;
			}
		}
	}

	@Override
	public void conversationAbandoned( ConversationAbandonedEvent event )
	{
		Util.saveRegionManager(((Location)event.getContext().getSessionData("sign")).getWorld());
		
		mPlayer.sendMessage("You are no longer in the management menu");
	}
}
