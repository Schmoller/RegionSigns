package au.com.mineauz.RegionSigns.claim;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.Confirmation;
import au.com.mineauz.RegionSigns.InteractableSign;
import au.com.mineauz.RegionSigns.InteractableSignState;
import au.com.mineauz.RegionSigns.Region;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Restriction;
import au.com.mineauz.RegionSigns.RestrictionType;
import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.events.ClaimSignCreateEvent;
import au.com.mineauz.RegionSigns.events.ClaimSignDestroyEvent;
import au.com.mineauz.RegionSigns.events.RegionClaimEvent;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ClaimSign extends InteractableSign 
{
	public enum Reason
	{
		Ok,
		Funds,
		Limit,
		ChildLimit,
		Restrict,
		NoExist,
		Owned
	}
	
	private class ClaimCallback implements Runnable
	{
		private ClaimSignState mState;
		private Player mPlayer;
		
		public ClaimCallback(ClaimSignState state, Player player)
		{
			mState = state;
			mPlayer = player;
		}
		
		@Override
		public void run()
		{
			Reason reason = canUserClaim(mState.getRegion(), mPlayer, mState.getPrice());
			
			if(reason != ClaimSign.Reason.Ok)
				mPlayer.sendMessage(ChatColor.RED + getDisplayMessage(reason));
			else
			{
				// Check that the sign still exists
				if(mState.getLocation().getBlock().getType() != Material.SIGN_POST && mState.getLocation().getBlock().getType() != Material.WALL_SIGN)
					mPlayer.sendMessage(ChatColor.RED + "Unable to claim region. It is no longer available to claim");
				else
				{
					Sign signState = (Sign)mState.getLocation().getBlock().getState();
					
					String[] lines = signState.getLines();
					
					for(int i = 0; i < 4; ++i)
					{
						lines[i] = RegionSigns.config.claimSign[i];
						lines[i] = lines[i].replaceAll("<user>", mPlayer.getName());
						lines[i] = lines[i].replaceAll("<region>", mState.getRegion().getId());
					}
					
					RegionClaimEvent event = new RegionClaimEvent(mState.getRegion(), mState.getLocation().clone(), mPlayer, mState.getPrice(), lines);
					
					// Pre-check
					if(!Util.playerHasEnough(mPlayer, mState.getPrice()))
					{
						mPlayer.sendMessage(ChatColor.RED + "Unable to claim region. Insufficient funds");
						return;
					}
					
					Bukkit.getPluginManager().callEvent(event);
					
					if(event.isCancelled())
					{
						mPlayer.sendMessage(ChatColor.RED + "Unable to claim region. Your claim was denied.");
						return;
					}
					
					double newAmount = event.getPayment();
					
					// Handle payment
					if(!Util.playerSubtractMoney(mPlayer, newAmount))
					{
						mPlayer.sendMessage(ChatColor.RED + "Unable to claim region. You require " + Util.formatCurrency(newAmount));
					}
					
					if(newAmount > 0)
						mPlayer.sendMessage(ChatColor.GREEN + "You have purchased " + mState.getRegion().getId() + " for " + Util.formatCurrency(newAmount));
					else
						mPlayer.sendMessage(ChatColor.GREEN + "You have claimed " + mState.getRegion().getId());
					
					// Add the player to the 
					mState.getRegion().getOwners().addPlayer(mPlayer.getName());
					
					Util.saveRegionManager(mState.getLocation().getWorld());
					
					// Change the sign text
					for(int i = 0; i < 4; i++)
						signState.setLine(i, event.getSignLines()[i]);
					
					signState.update(true);
				}	
			}
		}
		
	}
	
	private static String sLastErrorExtraMessage;
	public static String getDisplayMessage(Reason reason)
	{
		switch(reason)
		{
		case Funds:
			return "Unable to claim region. Insufficient funds";
		case ChildLimit:
			return "Unable to claim region. You own too many regions in " + sLastErrorExtraMessage;
		case Limit:
			return "Unable to claim region. You own too many regions.";
		case NoExist:
			return "Unable to claim region. The region doesnt exist.";
		case Owned:
			return "Unable to claim region. The region already has an owner.";
		case Restrict:
			return "Unable to claim region.\n" + sLastErrorExtraMessage;
		default:
			return "";
		}
	}
	
	public static Reason canUserClaim(ProtectedRegion region, Player player, double cost)
	{
		if(region == null)
		{
			return Reason.NoExist;
		}
		
		if(region.getOwners().size() != 0)
		{
			return Reason.Owned;
		}
		
		// Restriction check
		Restriction restriction = RegionSigns.restrictionManager.getRestriction(new Region(player.getWorld(), region.getId()));
		
		if(restriction != null && restriction.type != RestrictionType.Rent && !player.hasPermission("regionsigns.use.norestrict"))
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
		
		// Check for funds
		if(cost > 0)
		{
			try
			{
				if(Economy.playerExists(player.getName()))
				{
					if(!Economy.hasEnough(player.getName(), cost))
						return Reason.Funds;
				}
				else
					return Reason.Funds;
			}
			catch(UserDoesNotExistException e)
			{
				return Reason.Funds;
			}
		}
		
		return Reason.Ok;
	}
	
	public ClaimSign(RegionSigns plugin)
	{
		super(plugin,"Claim", "regionsigns.create.claim", "regionsigns.use.claim");
	}
	
	@Override
	protected void onClick(Player player, InteractableSignState instance,Block block) 
	{
		// Possibly already clicked on it
		if(player.isConversing())
			return;
		
		ClaimSignState state = (ClaimSignState)instance;
		
		Reason reason = canUserClaim(state.getRegion(), player, state.getPrice());
		
		if(reason != Reason.Ok)
		{
			player.sendMessage(ChatColor.RED + getDisplayMessage(reason));
			return;
		}

		// Ask to confirm it
		ClaimCallback callback = new ClaimCallback(state, player);
		String prompt = "";
		
		if(state.getPrice() == 0)
			prompt = "Do you wish to claim " + ChatColor.YELLOW + state.getRegion().getId() + ChatColor.WHITE + " as your own? (Type yes or no)";
		else
			prompt = "Do you wish to purchase " + ChatColor.YELLOW + state.getRegion().getId() + ChatColor.WHITE + " for " + ChatColor.GREEN + Util.formatCurrency(state.getPrice()) + ChatColor.WHITE  + "? (Type yes or no)";
		
		String timeoutText = "Please click the sign again if you still wish to claim " + ChatColor.YELLOW + state.getRegion().getId();
		String cancelText = "You have decided not to claim " + ChatColor.YELLOW + state.getRegion().getId();
		
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
		ClaimSignState state = (ClaimSignState)instance;
		
		// Check the region
		if(player.hasPermission("regionsigns.create.claim.ownparent"))
		{
			// They must own the parent region and there must be a parent region
			if(state.getRegion().getParent() == null)
				throw new Exception("You must own the parent region of '" + state.getRegion().getId() + "' to create a claim sign for it but there isn't one.");
			else if(!state.getRegion().getParent().getOwners().contains(RegionSigns.worldGuard.wrapPlayer(player)))
				throw new Exception("You must own the region '" + state.getRegion().getParent().getId() + "' to create a claim sign for '" + state.getRegion().getId() + "'");
		}
		
		if(state.getRegion().getOwners().size() != 0)
			throw new Exception(ChatColor.GOLD + "The region '" + state.getRegion().getId() + "' already has an owner.");
		
		// Check the price
		double maxPrice = RegionSigns.config.maxClaimPrice;
		if(maxPrice != 0)
		{
			// Make sure the range is fine
			if(state.getPrice() > maxPrice)
				throw new Exception("The price is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.config.maxClaimPrice));
		}
	}
	
	@Override
	protected void replaceInvalidSign(InteractableSignState instance, String[] lines)
	{
		ClaimSignState state = (ClaimSignState)instance;
		
		if(state.getRegion() == null)
			return;
		
		if(state.getRegion().getOwners().size() != 0)
		{
			String owner = (String)state.getRegion().getOwners().getPlayers().toArray()[0];
			
			for(int i = 0; i < 4; ++i)
			{
				lines[i] = RegionSigns.config.claimSign[i];
				lines[i] = lines[i].replaceAll("<user>", owner);
				lines[i] = lines[i].replaceAll("<region>", state.getRegion().getId());
			}
		}
	}
	
	@Override
	protected void onSignCreated( InteractableSignState instance )
	{
		ClaimSignState state = (ClaimSignState)instance;
		
		ClaimSignCreateEvent event = new ClaimSignCreateEvent(state.getRegion(), state.getLocation().clone(), state.getPrice());
		
		Bukkit.getPluginManager().callEvent(event);
	}
	
	@Override
	protected void onSignDestroyed( InteractableSignState instance )
	{
		ClaimSignState state = (ClaimSignState)instance;

		ClaimSignDestroyEvent event = new ClaimSignDestroyEvent(state.getRegion(), state.getLocation().clone());
		
		Bukkit.getPluginManager().callEvent(event);
	}

	@Override
	protected InteractableSignState getNewState()
	{
		return new ClaimSignState();
	}
}
