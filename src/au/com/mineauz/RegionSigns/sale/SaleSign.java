package au.com.mineauz.RegionSigns.sale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import au.com.mineauz.RegionSigns.events.RegionClaimEvent;
import au.com.mineauz.RegionSigns.events.RegionUnclaimEvent;
import au.com.mineauz.RegionSigns.events.SaleSignCreateEvent;
import au.com.mineauz.RegionSigns.events.SaleSignDestroyEvent;
import au.com.mineauz.RegionSigns.manage.ManagementMenu;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentStatus;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SaleSign extends InteractableSign 
{
	public enum Reason
	{
		Ok,
		Funds,
		Limit,
		ChildLimit,
		Restrict,
		NoExist,
		NotOwned,
		Owner
	}
	
	private class SaleCallback implements Runnable
	{
		private SaleSignState mState;
		private Player mPlayer;
		
		public SaleCallback(SaleSignState state, Player player)
		{
			mState = state;
			mPlayer = player;
		}
		
		@Override
		public void run()
		{
			Reason reason = canUserClaim(mState.getRegion(), mPlayer, mState.getPrice());
			
			if(reason != SaleSign.Reason.Ok)
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
					
					Bukkit.getPluginManager().callEvent(new RegionUnclaimEvent(mState.getRegion(), mState.getLocation()));
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
					
					double amtPer = newAmount / mState.getRegion().getOwners().size();
					OfflinePlayer[] owners = new OfflinePlayer[mState.getRegion().getOwners().size()];
					
					int index = 0;
					for(String owner : mState.getRegion().getOwners().getPlayers())
					{
						owners[index] = Bukkit.getOfflinePlayer(owner); 
						Util.playerAddMoney(owners[index], amtPer);

						++index;
					}
					
					
					String message;
					
					if(newAmount > 0)
						message = ChatColor.GREEN + "Congratulations! " + mPlayer.getDisplayName() + ChatColor.WHITE + " purchased " + ChatColor.YELLOW + mState.getRegion().getId() + ChatColor.WHITE + " from you for " + ChatColor.GREEN + Util.formatCurrency(newAmount);
					else
						message = ChatColor.GREEN + "Congratulations! " + mPlayer.getDisplayName() + ChatColor.WHITE + " claimed " + ChatColor.YELLOW + mState.getRegion().getId() + ChatColor.WHITE + " from you.";
					
					for(OfflinePlayer owner : owners)
					{
						if(owner.isOnline())
						{
							owner.getPlayer().sendMessage(message);
							
							if(amtPer > 0)
							{
								String nameList = "you";

								for(OfflinePlayer other : owners)
								{
									if(other == owner)
										continue;
									
									nameList += ", ";
									
									if(other == owners[owners.length-1])
										nameList += "and ";
									
									nameList += other.getName();
								}
								
								owner.getPlayer().sendMessage(ChatColor.GREEN + Util.formatCurrency(amtPer) + " has been payed to " + nameList + (owners.length > 1 ? " each" : ""));
							}
						}
					}
					
					if(newAmount > 0)
						mPlayer.sendMessage(ChatColor.GREEN + "You have purchased " + mState.getRegion().getId() + " for " + Util.formatCurrency(newAmount));
					else
						mPlayer.sendMessage(ChatColor.GREEN + "You have claimed " + mState.getRegion().getId());
					
					// Clear the existing people from it 
					mState.getRegion().setOwners(new DefaultDomain());
					mState.getRegion().setMembers(new DefaultDomain());
					
					// Add this player to the region
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
		case NotOwned:
			return "Unable to claim region. The region does not have an owner.";
		case Restrict:
			return "Unable to claim region.\n" + sLastErrorExtraMessage;
		case Owner:
			return "Unable to claim region. You already own it"; 
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
		
		if(region.getOwners().size() == 0)
		{
			return Reason.NotOwned;
		}
		
		if(region.isOwner(player.getName()))
			return Reason.Owner;
		
		// Restriction check
		Restriction restriction = RegionSigns.restrictionManager.getRestriction(new Region(player.getWorld(), region.getId()));
		
		if(restriction != null && restriction.type != RestrictionType.Rent && !player.hasPermission("regionsigns.use.norestrict"))
		{
			// Valid restriction for claiming
			if(!player.hasPermission(restriction.permission))
			{
				sLastErrorExtraMessage = ChatColor.translateAlternateColorCodes('&', restriction.message);
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
	
	public SaleSign(RegionSigns plugin)
	{
		super(plugin,"For Sale", "regionsigns.create.sale", "regionsigns.use.sale");
	}
	
	@Override
	protected void onClick(Player player, InteractableSignState instance,Block block) 
	{
		// Possibly already clicked on it
		if(player.isConversing())
			return;
		
		SaleSignState state = (SaleSignState)instance;
		
		Reason reason = canUserClaim(state.getRegion(), player, state.getPrice());
		
		if(reason == Reason.Owner && player.hasPermission("regionsigns.use.manage"))
		{
			if(player.isConversing())
				// Possibly already in the menu
				return;
			
			DefaultDomain applicableGroup;
			
			RentStatus status = RentManager.instance.getStatus(new Region(block.getWorld(), state.getRegion().getId()));
			
			if(status != null)
				applicableGroup = state.getRegion().getMembers();
			else
				applicableGroup = state.getRegion().getOwners();
			
			// See if this player can use the sign
			if(!applicableGroup.contains(player.getName()) && !player.hasPermission("regionsigns.use.manage.others"))
				return;
			
			// Do conversation
			ManagementMenu menu = new ManagementMenu(state.getRegion(), block.getLocation(), player, status, true);
			menu.show();
			return;
		}
		else if(reason != Reason.Ok)
		{
			player.sendMessage(ChatColor.RED + getDisplayMessage(reason));
			return;
		}

		// Ask to confirm it
		SaleCallback callback = new SaleCallback(state, player);
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
		SaleSignState state = (SaleSignState)instance;
		
		// Check the region
		if(!player.hasPermission("regionsigns.use.norestrict"))
		{
			// They must own the region
			if(!state.getRegion().getOwners().contains(RegionSigns.worldGuard.wrapPlayer(player)))
				throw new Exception("You must own the region '" + state.getRegion().getId() + "' to create a sale sign.");
		}
		
		if(state.getRegion().getOwners().size() == 0)
			throw new Exception(ChatColor.GOLD + "The region '" + state.getRegion().getId() + "' has no owner.");
		
		// Check the price
		double maxPrice = RegionSigns.config.maxSalePrice;
		if(maxPrice != 0)
		{
			// Make sure the range is fine
			if(state.getPrice() > maxPrice)
				throw new Exception("The price is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.config.maxSalePrice));
		}
	}
	
	@Override
	protected void replaceInvalidSign(InteractableSignState instance, String[] lines)
	{
	}
	
	@Override
	protected void onSignCreated( InteractableSignState instance, Player creator )
	{
		SaleSignState state = (SaleSignState)instance;
		
		SaleSignCreateEvent event = new SaleSignCreateEvent(state.getRegion(), state.getLocation().clone(), state.getPrice());
		
		Bukkit.getPluginManager().callEvent(event);
	}
	
	@Override
	protected void onSignDestroyed( InteractableSignState instance )
	{
		SaleSignState state = (SaleSignState)instance;

		SaleSignDestroyEvent event = new SaleSignDestroyEvent(state.getRegion(), state.getLocation().clone());
		
		Bukkit.getPluginManager().callEvent(event);
	}

	@Override
	protected InteractableSignState getNewState()
	{
		return new SaleSignState();
	}
}
