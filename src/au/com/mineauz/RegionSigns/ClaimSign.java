package au.com.mineauz.RegionSigns;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.events.ClaimSignCreateEvent;
import au.com.mineauz.RegionSigns.events.ClaimSignDestroyEvent;

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
		
		// Check that this player can actually claim the region
		if(!player.hasPermission("regionsigns.use.nolimit") && RegionSigns.instance.getClaimLimit() != 0)
		{
			// Check to see if they have too many
			if(RegionSigns.instance.getPlayerRegionCount(player, false) >= RegionSigns.instance.getClaimLimit())
				return Reason.Limit;
		}
		
		if(region.getParent() != null)
		{
			String name = player.getWorld().getName() + "-" + region.getParent().getId();
			
			// Check for claim restrictions
			if(RegionSigns.instance.ClaimRestrictions.containsKey(name) && !player.hasPermission("regionsigns.use.norestrict"))
			{
				// Claiming is restricted. Check the perm
				if(!player.hasPermission(RegionSigns.instance.ClaimRestrictions.get(name).ClaimPermission))
				{
					sLastErrorExtraMessage = RegionSigns.instance.ClaimRestrictions.get(name).Message;
					return Reason.Restrict;
				}
			}
			// Check for other claim restrictions
			if(RegionSigns.instance.AnyRestrictions.containsKey(name) && !player.hasPermission("regionsigns.use.norestrict"))
			{
				// Claiming is restricted. Check the perm
				if(!player.hasPermission(RegionSigns.instance.AnyRestrictions.get(name).ClaimPermission))
				{
					sLastErrorExtraMessage = RegionSigns.instance.AnyRestrictions.get(name).Message;
					return Reason.Restrict;
				}
			}
			
			int regionCount = RegionSigns.instance.getPlayerRegionCountIn(region.getParent(), player, false);
			
			if(!player.hasPermission("regionsigns.use.nolimit") && RegionSigns.instance.getClaimChildLimit() != 0)
			{
				// Check to see if they have too many
				if(regionCount >= RegionSigns.instance.getClaimChildLimit())
				{
					sLastErrorExtraMessage = region.getParent().getId();
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
		
		SetArgument1(ArgumentTypes.String, true, null);
		SetArgument2(ArgumentTypes.None, true, null);
		SetArgument3(ArgumentTypes.Currency, false, 0.0);
	}
	
	@Override
	protected void onClick(Player player, InteractableSignState instance,Block block) 
	{
		// Possibly already clicked on it
		if(player.isConversing())
			return;
		
		if(RegionSigns.worldGuard.getRegionManager(block.getLocation().getWorld()) == null)
			return;

		if(!(instance.Argument1 instanceof String) || !(instance.Argument3 instanceof Double))
			// Save guarding against plugin based edits
			return;
		
		ProtectedRegion region = RegionSigns.worldGuard.getRegionManager(block.getLocation().getWorld()).getRegion((String)instance.Argument1);
		Reason reason = canUserClaim(region, player, (Double)instance.Argument3);
		
		if(reason != Reason.Ok)
		{
			player.sendMessage(ChatColor.RED + getDisplayMessage(reason));
			return;
		}

		// Ask to confirm it
		new ClaimConfirmation(region,player,(Double)instance.Argument3, instance);
	}

	@Override
	protected boolean validateArgument(int argumentId, Object argumentValue, Location location, Player player) 
	{
		switch(argumentId)
		{
		case 1:
			// This must be a valid region
			if(RegionSigns.worldGuard.getRegionManager(location.getWorld()) == null)
				return false;
			if(RegionSigns.worldGuard.getRegionManager(location.getWorld()).hasRegion((String)argumentValue))
			{
				ProtectedRegion region = RegionSigns.worldGuard.getRegionManager(location.getWorld()).getRegion((String)argumentValue);
				// Now check permissions
				if(player.hasPermission("regionsigns.create.claim.ownparent"))
				{
					// They must own the parent region and there must be a parent region
					if(region.getParent() == null)
					{
						player.sendMessage(ChatColor.RED + "You are restricted to creating claim signs of regions you own the parent of");
						return false;
					}
					else if(!region.getParent().getOwners().contains(RegionSigns.worldGuard.wrapPlayer(player)))
					{
						player.sendMessage(ChatColor.RED + "You are restricted to creating claim signs of regions you own the parent of");
						return false;
					}
				}
				
				if(region.getOwners().size() != 0)
				{
					// Output a warning to say this region can't be claimed
					//player.sendMessage(ChatColor.GOLD + "The region '" + region.getId() + "' already has an owner. It will not be able to be claimed until the owner is removed");
					player.sendMessage(ChatColor.GOLD + "The region '" + region.getId() + "' already has an owner.");
					return false;
				}
				return true;
			}
			else
			{
				player.sendMessage(ChatColor.RED + "The region '" + (String)argumentValue + "' does not exist");
			}
			return false;
		case 3:
			// Make sure its not too high
			double maxPrice = RegionSigns.instance.getConfig().getDouble("claim.max-price");
			if(maxPrice == 0)
				return true; // Any amount is fine
			else
			{
				// Make sure the range is fine
				if((Double)argumentValue <= maxPrice)
					return true;
				else
				{
					player.sendMessage(ChatColor.RED + "The price is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.instance.getConfig().getDouble("claim.max-price")));
					return false;
				}
			}
		}
		return false;
	}
	
	protected void replaceSign(InteractableSignState state, boolean valid, String[] lines)
	{
		if(!valid)
		{
			if(RegionSigns.worldGuard.getRegionManager(state.SignLocation.getWorld()) == null)
				return;
			if(RegionSigns.worldGuard.getRegionManager(state.SignLocation.getWorld()).hasRegion((String)state.Argument1))
			{
				ProtectedRegion region = RegionSigns.worldGuard.getRegionManager(state.SignLocation.getWorld()).getRegion((String)state.Argument1);
				
				if(region.getOwners().size() != 0)
				{
					String owner = (String)region.getOwners().getPlayers().toArray()[0];
					String line = RegionSigns.instance.getConfig().getString("claim.sign.line1","");
					line = line.replaceAll("<user>", owner);
					line = line.replaceAll("<region>", region.getId());
					lines[0] = line;

					line = RegionSigns.instance.getConfig().getString("claim.sign.line2","");
					line = line.replaceAll("<user>", owner);
					line = line.replaceAll("<region>", region.getId());
					lines[1] = line;
					
					line = RegionSigns.instance.getConfig().getString("claim.sign.line3","");
					line = line.replaceAll("<user>", owner);
					line = line.replaceAll("<region>", region.getId());
					lines[2] = line;
					
					line = RegionSigns.instance.getConfig().getString("claim.sign.line4","");
					line = line.replaceAll("<user>", owner);
					line = line.replaceAll("<region>", region.getId());
					lines[3] = line;
				}
			}
		}
	}
	
	@Override
	protected void onSignCreated( InteractableSignState state )
	{
		ProtectedRegion region = Util.getRegion(state.SignLocation.getWorld(), (String)state.Argument1);
		double amount = (Double)state.Argument3;
		
		if(region == null)
			return;
		
		ClaimSignCreateEvent event = new ClaimSignCreateEvent(region, state.SignLocation.clone(), amount);
		
		Bukkit.getPluginManager().callEvent(event);
		
	}
	
	@Override
	protected void onSignDestroyed( InteractableSignState state )
	{
		ProtectedRegion region = Util.getRegion(state.SignLocation.getWorld(), (String)state.Argument1);
		
		if(region == null)
			return;
		
		ClaimSignDestroyEvent event = new ClaimSignDestroyEvent(region, state.SignLocation.clone());
		
		Bukkit.getPluginManager().callEvent(event);
	}
}
