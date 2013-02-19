package au.com.mineauz.RegionSigns.rent;

import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;

import au.com.mineauz.RegionSigns.Confirmation;
import au.com.mineauz.RegionSigns.InteractableSign;
import au.com.mineauz.RegionSigns.InteractableSignState;
import au.com.mineauz.RegionSigns.Region;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Restriction;
import au.com.mineauz.RegionSigns.RestrictionType;
import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.events.RegionRentEvent;
import au.com.mineauz.RegionSigns.events.RentSignCreateEvent;
import au.com.mineauz.RegionSigns.events.RentSignDestroyEvent;

public class RentSign extends InteractableSign
{
	public enum Reason
	{
		Ok,
		NoExist,
		Owned,
		Limit,
		Restrict,
		ChildLimit,
		Funds
	}
	private static String sLastErrorExtraMessage;
	
	private class RentCallback implements Runnable
	{
		private RentSignState mState;
		private Player mPlayer;
		
		public RentCallback(RentSignState state, Player player)
		{
			mState = state;
			mPlayer = player;
		}
		
		@Override
		public void run()
		{
			Reason reason = canUserRent(mPlayer, mState);
			
			if(reason != RentSign.Reason.Ok)
				mPlayer.sendMessage(ChatColor.RED + RentSign.getDisplayMessage(reason));
			else
			{
				if(mState.getLocation().getBlock().getType() != Material.SIGN_POST && mState.getLocation().getBlock().getType() != Material.WALL_SIGN)
					mPlayer.sendMessage(ChatColor.RED + "Unable to rent region. It is no longer available for rent");
				else
				{
					Sign signState = (Sign)mState.getLocation().getBlock().getState();
					
					String[] lines = new String[4];
					
					for(int i = 0; i < 4; ++i)
					{
						lines[i] = RegionSigns.config.rentSign[i];
						lines[i] = lines[i].replaceAll("<user>", mPlayer.getName());
						lines[i] = lines[i].replaceAll("<region>", mState.getRegion().getId());
					}
					
					RegionRentEvent event = new RegionRentEvent(mState.getRegion(), mState.getLocation().clone(), mPlayer, mState.getInitialPrice(), lines);
					
					if (!Util.playerHasEnough(mPlayer, mState.getInitialPrice()))
					{
						mPlayer.sendMessage(ChatColor.RED + "Unable to rent region. Insufficient Funds.");
						return;
					}
					
					Bukkit.getPluginManager().callEvent(event);
					
					if(event.isCancelled())
					{
						mPlayer.sendMessage(ChatColor.RED + "Unable to rent region. Your request was denied.");
						return;
					}
					
					double newPayment = event.getPayment();

					if(!Util.playerSubtractMoney(mPlayer, newPayment))
					{
						mPlayer.sendMessage(ChatColor.RED + "Unable to rent region. Insufficient Funds.");
						return;
					}
				
					RentMessage beginMessage = new RentMessage();
					beginMessage.Region = mState.getRegion().getId();
					beginMessage.Payment = newPayment;
					beginMessage.EventCompletionTime = 0;
					beginMessage.Type = (newPayment > 0 ? RentMessageTypes.RentBegin : RentMessageTypes.RentBeginFree);
					
					RentMessage firstPaymentMessage = new RentMessage();
					firstPaymentMessage.Region = mState.getRegion().getId();
					firstPaymentMessage.Payment = mState.getIntervalPrice();
					firstPaymentMessage.EventCompletionTime = Calendar.getInstance().getTimeInMillis() + mState.getIntervalLength();
					firstPaymentMessage.Type = RentMessageTypes.FirstPaymentTime;
					
					RentManager.instance.sendMessage(beginMessage, mPlayer);
					
					if(mState.getIntervalPrice() > 0)
						RentManager.instance.sendMessage(firstPaymentMessage, mPlayer);
					
					mPlayer.sendMessage(ChatColor.GREEN + "[Rent] " + ChatColor.WHITE + "Use '/rent help' for a list of rent commands");
					
					if(!Util.playerHasEnough(mPlayer, mState.getIntervalPrice()))
					{
						// Send a warning
						RentMessage warning = new RentMessage();
						warning.Region = mState.getRegion().getId();
						warning.Payment = mState.getIntervalPrice();
						warning.EventCompletionTime = Calendar.getInstance().getTimeInMillis() + mState.getIntervalLength();
						warning.Type = RentMessageTypes.InsufficientFunds;
						
						RentManager.instance.sendMessage(warning, mPlayer);
					}
					
					RentStatus status = new RentStatus();
					status.Region = mState.getRegion().getId();
					status.World = mState.getLocation().getWorld().getName();
					status.Tenant = mPlayer;
					status.IntervalPayment = mState.getIntervalPrice();
					status.RentInterval = mState.getIntervalLength();
					status.NextIntervalEnd = Calendar.getInstance().getTimeInMillis() + mState.getIntervalLength();
					status.Date = Calendar.getInstance().getTimeInMillis();
					status.PendingEviction = false;

					RentManager.instance.pushRent(status, status.NextIntervalEnd);
					
					// Add the player to the 
					mState.getRegion().getMembers().addPlayer(mPlayer.getName());
					try {
						RegionSigns.worldGuard.getRegionManager(mState.getLocation().getWorld()).save();
					} catch (ProtectionDatabaseException e) {
					}
					
					// Change the sign text
					for(int i = 0; i < 4; ++i)
						signState.setLine(i, event.getSignLines()[i]);
					
					signState.update(true);
				}
			}
		}
		
	}
	
	public static Reason canUserRent(Player player, RentSignState state)
	{
		if(state.getRegion() == null)
			return Reason.NoExist;
		
		if(state.getRegion().getMembers().size() != 0)
			return Reason.Owned;

		// Restriction check
		Restriction restriction = RegionSigns.restrictionManager.getRestriction(new Region(player.getWorld(), state.getRegion().getId()));
		
		if(restriction != null && restriction.type != RestrictionType.Claim && !player.hasPermission("regionsigns.use.norestrict"))
		{
			// Valid restriction for claiming
			if(!player.hasPermission(restriction.permission))
			{
				sLastErrorExtraMessage = restriction.message.replaceAll("&", "" + ChatColor.COLOR_CHAR);
				return Reason.Restrict;
			}
			
			// Check the own limit
			if(restriction.maxCount > 0 && !player.hasPermission("regionsigns.use.nolimit"))
			{
				int regionCount = RegionSigns.instance.getPlayerRegionCountIn(restriction.region, player, restriction.type);
				
				if(regionCount >= restriction.maxCount)
				{
					// Cant claim another region, Too many are owned
					if(restriction.region.getID().equalsIgnoreCase("__global__"))
					{
						if(restriction.region.isSuperGlobal())
						{
							sLastErrorExtraMessage = null;
							return Reason.Limit;
						}
						else
						{
							sLastErrorExtraMessage = restriction.region.getWorld().getName();
							return Reason.Limit;
						}
					}
						
					sLastErrorExtraMessage = restriction.region.getID();
					return Reason.ChildLimit;
				}
			}
		}
		
		// Check that they can afford it
		
		if(!Util.playerHasEnough(player, state.getInitialPrice()))
			return Reason.Funds;
		
		return Reason.Ok;
	}
	public static String getDisplayMessage(Reason reason)
	{
		switch(reason)
		{
		case Funds:
			return "Unable to rent region. Insufficient funds";
		case ChildLimit:
			return "Unable to rent region. You own too many regions in " + sLastErrorExtraMessage;
		case Limit:
			return "Unable to rent region. You own too many regions.";
		case NoExist:
			return "Unable to rent region. The region doesnt exist.";
		case Owned:
			return "Unable to rent region. The region already has an tenant.";
		case Restrict:
			return "Unable to rent region.\n" + sLastErrorExtraMessage;
		default:
			return "";
		}
	}
	
	public RentSign(RegionSigns plugin)
	{
		super(plugin, "Rent", "regionsigns.create.rent", "regionsigns.use.rent");
	}

	@Override
	protected void onClick(Player player, InteractableSignState instance, Block block) 
	{
		RentSignState state = (RentSignState)instance;
		
		// Possibly already clicked on it
		if(player.isConversing())
			return;
		
		Reason reason = canUserRent(player, state);
		
		if(reason != Reason.Ok)
		{
			player.sendMessage(ChatColor.RED + getDisplayMessage(reason));
			return;
		}
		
		// Ask to confirm it
		RentCallback callback = new RentCallback(state, player);
		String prompt = "";
		prompt = "Do you wish to rent " + ChatColor.YELLOW + state.getRegion().getId() + ChatColor.WHITE + "?";
		
		if(state.getInitialPrice() > 0 && state.getIntervalPrice() > 0)
		{
			prompt += "\nYou will be required to pay " + ChatColor.YELLOW + Util.formatCurrency(state.getInitialPrice()) + ChatColor.WHITE + 
				" now and the rent of " + ChatColor.YELLOW + Util.formatCurrency(state.getIntervalPrice()) + ChatColor.WHITE + 
				" every " + ChatColor.YELLOW + Util.formatTimeDifference(state.getIntervalLength(), 2, true) + ChatColor.WHITE + ".";
		}
		else if(state.getIntervalPrice() > 0)
		{
			prompt += "\nYou will be required to pay " + ChatColor.YELLOW + "nothing" + ChatColor.WHITE + 
					" now but rent of " + ChatColor.YELLOW + Util.formatCurrency(state.getIntervalPrice()) + ChatColor.WHITE + 
					" every " + ChatColor.YELLOW + Util.formatTimeDifference(state.getIntervalLength(), 2, true) + ChatColor.WHITE + ".";
		}
		else if(state.getInitialPrice() > 0)
		{
			prompt += "\nYou will be required to pay " + ChatColor.YELLOW + Util.formatCurrency(state.getInitialPrice()) + ChatColor.WHITE + " now only.";
		}
		else
		{
			prompt += "\nYou will be charged nothing for the entire time you rent " + state.getRegion().getId();
		}
		
		if(RegionSigns.config.minimumRentPeriod != 0)
			prompt += "\nYou are required to rent for at least " + ChatColor.YELLOW + Util.formatTimeDifference(RegionSigns.config.minimumRentPeriod, 2, false) + ChatColor.WHITE;

		prompt += "\n(Type yes or no)";
		
		String timeoutText = "Please click the sign again if you still wish to rent " + ChatColor.YELLOW + state.getRegion().getId();
		String cancelText = "You have decided not to rent " + ChatColor.YELLOW + state.getRegion().getId();
		
		Confirmation c = new Confirmation()
			.setCancelText(cancelText)
			.setPromptText(prompt)
			.setTimeoutText(timeoutText)
			.withSuccessCallback(callback);
		
		c.askPlayer(player);
	}

	@Override
	protected void validateState(InteractableSignState instance, Player player) throws Exception 
	{
		RentSignState state = (RentSignState)instance;
		
		// Check the region
		if(player.hasPermission("regionsigns.create.rent.ownregion"))
		{
			// They must own the region
			if(!state.getRegion().getOwners().contains(RegionSigns.worldGuard.wrapPlayer(player)))
				throw new Exception("You must own '" + state.getRegion().getId() + "' before you can rent it out.");
		}
		
		if(state.getRegion().getMembers().size() != 0)
			throw new Exception(ChatColor.GOLD + "The region '" + state.getRegion().getId() + "' already has a tennant.");
		

		// Check the initial cost
		// Make sure its not too high
		double maxPrice = RegionSigns.config.maxRentUpfrontPayment;
		if(maxPrice != 0)
		{
			// Make sure the range is fine
			if(state.getInitialPrice() > maxPrice)
				throw new Exception("The upfront price is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.config.maxRentUpfrontPayment));
		}
		
		// Check the interval cost
		maxPrice = RegionSigns.config.maxRentIntervalPayment;
		if(maxPrice != 0)
		{
			// Make sure the range is fine
			if(state.getIntervalPrice() > maxPrice)
				throw new Exception("The interval price is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.config.maxRentIntervalPayment));
		}
	}

	@Override
	protected void replaceInvalidSign(InteractableSignState instance, String[] lines) 
	{
		RentSignState state = (RentSignState)instance;
		
		if(state.getRegion() == null)
			return;
		
		if(state.getRegion().getMembers().size() != 0)
		{
			String owner = (String)state.getRegion().getMembers().getPlayers().toArray()[0];
			
			for(int i = 0; i < 4; ++i)
			{
				lines[i] = RegionSigns.config.rentSign[i];
				lines[i] = lines[i].replaceAll("<user>", owner);
				lines[i] = lines[i].replaceAll("<region>", state.getRegion().getId());
			}
		}
	}
	
	@Override
	protected void onSignCreated( InteractableSignState instance )
	{
		RentSignState state = (RentSignState)instance;
		
		RentSignCreateEvent event = new RentSignCreateEvent(state.getRegion(), state.getLocation().clone(), state.getInitialPrice(), state.getIntervalPrice(), state.getIntervalLength());
		
		Bukkit.getPluginManager().callEvent(event);
	}
	
	@Override
	protected void onSignDestroyed( InteractableSignState instance )
	{
		RentSignState state = (RentSignState)instance;
		
		RentSignDestroyEvent event = new RentSignDestroyEvent(state.getRegion(), state.getLocation().clone());
		
		Bukkit.getPluginManager().callEvent(event);
	}
	@Override
	protected InteractableSignState getNewState()
	{
		return new RentSignState();
	}
}