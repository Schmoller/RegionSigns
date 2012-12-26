package au.com.mineauz.RegionSigns;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.events.RentSignCreateEvent;
import au.com.mineauz.RegionSigns.events.RentSignDestroyEvent;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

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
	
	public static Reason canUserRent(Player player, ProtectedRegion region, InteractableSignState sign)
	{
		if(region == null)
			return Reason.NoExist;
		
		if(region.getMembers().size() != 0)
			return Reason.Owned;
		
		// Check that this player can actually rent the region
		if(!player.hasPermission("regionsigns.use.nolimit") && RegionSigns.instance.getConfig().getInt("rent.overallmax",5) != 0)
		{
			// Check to see if they have too many
			if(RegionSigns.instance.getPlayerRegionCount(player, true) >= RegionSigns.instance.getConfig().getInt("rent.overallmax",5))
				return Reason.Limit;
		}
		
		if(region.getParent() != null)
		{
			String name = sign.SignLocation.getWorld().getName() + "-" + region.getParent().getId();
			
			// Check for claim restrictions
			if(((RegionSigns)RegionSigns.instance).RentRestrictions.containsKey(name) && !player.hasPermission("regionsigns.use.norestrict"))
			{
				// Claiming is restricted. Check the perm
				if(!player.hasPermission(((RegionSigns)RegionSigns.instance).RentRestrictions.get(name).ClaimPermission))
				{
					sLastErrorExtraMessage = ((RegionSigns)RegionSigns.instance).RentRestrictions.get(name).Message;
					return Reason.Restrict;
				}
			}
			// Check for other claim restrictions
			if(((RegionSigns)RegionSigns.instance).AnyRestrictions.containsKey(name) && !player.hasPermission("regionsigns.use.norestrict"))
			{
				// Claiming is restricted. Check the perm
				if(!player.hasPermission(((RegionSigns)RegionSigns.instance).AnyRestrictions.get(name).ClaimPermission))
				{
					sLastErrorExtraMessage = ((RegionSigns)RegionSigns.instance).AnyRestrictions.get(name).Message;
					return Reason.Restrict;
				}
			}
			
			// Run a search to find the number of regions they have for this parent

			int regionCount = RegionSigns.instance.getPlayerRegionCountIn(region.getParent(), player, true);
			
			if(!player.hasPermission("regionsigns.use.nolimit") && RegionSigns.instance.getConfig().getInt("rent.childmax",1) != 0)
			{
				// Check to see if they have too many
				if(regionCount >= RegionSigns.instance.getConfig().getInt("rent.childmax",1))
				{
					sLastErrorExtraMessage = region.getParent().getId();
					return Reason.ChildLimit;
				}
			}
		}
		
		double downPayment = (Double)sign.Argument2;
		
		// Check that they can afford it
		// Handle payment
		if(downPayment > 0)
		{
			try
			{
				if(Economy.playerExists(player.getName()))
				{
					if(!Economy.hasEnough(player.getName(), downPayment))
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

		// Set the sign arguments
		SetArgument1(ArgumentTypes.String, true,null);
		SetArgument2(ArgumentTypes.Currency, false,0.0);
		SetArgument3(ArgumentTypes.String, false,"$0");
	}

	@Override
	protected void onClick(Player player, InteractableSignState instance, Block block) 
	{
		
		if(!(instance.Argument1 instanceof String) || !(instance.Argument2 instanceof Double) || !(instance.Argument3 instanceof String))
			// Safeguarding against plugin based edits
			return;
		
		if(RegionSigns.worldGuard.getRegionManager(block.getWorld()) == null)
			return;
		
		// Safeguard against plugin based edits. Check the last param
		String lastArg = (String)instance.Argument3;
		if(lastArg.indexOf(":") != -1)
		{
			// This does contain the period
			String perIntervalPriceString = lastArg.substring(0,lastArg.indexOf(":"));
			String intervalLength = lastArg.substring(lastArg.indexOf(":")+1);
			
			// There is one char at the beginning that is the currency symbol
			if(perIntervalPriceString.startsWith(Util.sCurrencyChar))
			{
				try 
				{ 
					double val = Double.parseDouble(perIntervalPriceString.substring(1));
					if(val >= 0)
					{ 
						if(!(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0))
							return;
					}
				} 
				catch(NumberFormatException e){ return; }
			}
			else
			{
				// Maybe its just the number
				try 
				{ 
					double val = Double.parseDouble(perIntervalPriceString);
					if(val >= 0)
					{ 
						if(!(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0))
							return;
					}
				} 
				catch(NumberFormatException e){ return; }
			}
			
			// attempt to parse the period
			try
			{
				Util.parseDateDiff(intervalLength);
			}
			catch(Exception e)
			{
				return;
			}
		}
		else
		{
			// Just the cost
			// There is one char at the beginning that is the currency symbol
			if(lastArg.startsWith(Util.sCurrencyChar))
			{
				try 
				{ 
					double val = Double.parseDouble(lastArg.substring(1));
					if(val >= 0)
					{ 
						if(!(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0))
							return;
					}
				} 
				catch(NumberFormatException e){return;}
			}
			else
			{
				// Maybe its just the number
				try 
				{ 
					double val = Double.parseDouble(lastArg);
					if(val >= 0)
					{ 
						if(!(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0))
							return;
					}
				} 
				catch(NumberFormatException e){return;}
			}
		}
		
		ProtectedRegion region = RegionSigns.worldGuard.getRegionManager(block.getWorld()).getRegion((String)instance.Argument1);

		Reason reason = canUserRent(player, region, instance);
		
		if(reason != Reason.Ok)
		{
			player.sendMessage(ChatColor.RED + getDisplayMessage(reason));
			return;
		}
		
		new RentConfirmation(region, player, instance);
	}

	@Override
	protected boolean validateArgument(int argumentId, Object argumentValue, Location location, Player player) 
	{
		String currencySymbol = RegionSigns.instance.getServer().getPluginManager().getPlugin("Essentials").getConfig().getString("currency-symbol");
		
		switch(argumentId)
		{
		case 1:
			// This must be a valid region
			if(RegionSigns.worldGuard.getRegionManager(location.getWorld()) == null)
				return false;
			if(RegionSigns.worldGuard.getRegionManager(location.getWorld()).hasRegion((String)argumentValue))
			{
				ProtectedRegion region = RegionSigns.worldGuard.getRegionManager(location.getWorld()).getRegion((String)argumentValue);
			
				// Check permissions
				if(player.hasPermission("regionsigns.create.rent.ownregion"))
				{
					// They must own the region
					if(!region.getOwners().contains(RegionSigns.worldGuard.wrapPlayer(player)))
					{
						player.sendMessage(ChatColor.RED + "You are restricted to renting out regions you own");
						return false;
					}
				}
				if(region.getMembers().size() != 0)
				{
					// Output a warning to say this region can't be rented
					//player.sendMessage(ChatColor.GOLD + "The region '" + region.getId() + "' already has a tennant. It will not be able to be rented until they are removed");
					player.sendMessage(ChatColor.GOLD + "The region '" + region.getId() + "' already has a tennant.");
					return false;
				}
				return true;
			}
			else
			{
				player.sendMessage(ChatColor.RED + "The region '" + (String)argumentValue + "' does not exist");
			}
			return false;
		case 2:
			// The initial cost
			// Make sure its not too high
			double maxPrice = RegionSigns.instance.getConfig().getDouble("rent.max-price-upfront");
			if(maxPrice == 0)
				return true; // Any amount is fine
			else
			{
				// Make sure the range is fine
				if((Double)argumentValue <= maxPrice)
					return true;
				else
				{
					player.sendMessage(ChatColor.RED + "The upfront price is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.instance.getConfig().getDouble("rent.max-price-upfront")));
					return false;
				}
			}
		case 3:
			// The cost per period and the period length
			if(((String)argumentValue).indexOf(":") != -1)
			{
				// This does contain the period
				String perIntervalPriceString = ((String)argumentValue).substring(0,((String)argumentValue).indexOf(":"));
				String intervalLength = ((String)argumentValue).substring(((String)argumentValue).indexOf(":")+1);
				
				// There is one char at the beginning that is the currency symbol
				if(perIntervalPriceString.startsWith(currencySymbol))
				{
					try 
					{ 
						double val = Double.parseDouble(perIntervalPriceString.substring(1));
						if(val >= 0)
						{ 
							if(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0)
								return true;
							else
								player.sendMessage(ChatColor.RED + "The price per interval is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.instance.getConfig().getDouble("rent.max-price-interval")));
						}
					} 
					catch(NumberFormatException e){}
				}
				else
				{
					// Maybe its just the number
					try 
					{ 
						double val = Double.parseDouble(perIntervalPriceString);
						if(val >= 0)
						{ 
							if(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0)
								return true;
							else
								player.sendMessage(ChatColor.RED + "The price per interval is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.instance.getConfig().getDouble("rent.max-price-interval")));
						}
					} 
					catch(NumberFormatException e){}
				}
				
				// attempt to parse the period
				try
				{
					Util.parseDateDiff(intervalLength);
				}
				catch(Exception e)
				{
					player.sendMessage(ChatColor.RED + "Incorrect interval format. Should be like '1w 2d' or '1mo'");
					return false;
				}
			}
			else
			{
				// Just the cost
				// There is one char at the beginning that is the currency symbol
				if(((String)argumentValue).startsWith(currencySymbol))
				{
					try 
					{ 
						double val = Double.parseDouble(((String)argumentValue).substring(1));
						if(val >= 0)
						{ 
							if(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0)
								return true;
							else
								player.sendMessage(ChatColor.RED + "The price per interval is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.instance.getConfig().getDouble("rent.max-price-interval")));
						}
					} 
					catch(NumberFormatException e){}
				}
				else
				{
					// Maybe its just the number
					try 
					{ 
						double val = Double.parseDouble(((String)argumentValue));
						if(val >= 0)
						{ 
							if(val <= RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") || RegionSigns.instance.getConfig().getDouble("rent.max-price-interval") == 0)
								return true;
							else
								player.sendMessage(ChatColor.RED + "The price per interval is too high. The maximum allowed price is " + Util.formatCurrency(RegionSigns.instance.getConfig().getDouble("rent.max-price-interval")));
						}
					} 
					catch(NumberFormatException e){}
				}
			}
			break;
		}
		return false;
	}

	@Override
	protected void replaceSign(InteractableSignState state, boolean valid, String[] lines) 
	{
		if(!valid)
		{
			if(RegionSigns.worldGuard.getRegionManager(state.SignLocation.getWorld()) == null)
				return;
			if(RegionSigns.worldGuard.getRegionManager(state.SignLocation.getWorld()).hasRegion((String)state.Argument1))
			{
				ProtectedRegion region = RegionSigns.worldGuard.getRegionManager(state.SignLocation.getWorld()).getRegion((String)state.Argument1);
				
				if(region.getMembers().size() != 0)
				{
					String owner = (String)region.getMembers().getPlayers().toArray()[0];
					String line = RegionSigns.instance.getConfig().getString("rent.sign.line1","");
					line = line.replaceAll("<user>", owner);
					line = line.replaceAll("<region>", region.getId());
					lines[0] = line;

					line = RegionSigns.instance.getConfig().getString("rent.sign.line2","");
					line = line.replaceAll("<user>", owner);
					line = line.replaceAll("<region>", region.getId());
					lines[1] = line;
					
					line = RegionSigns.instance.getConfig().getString("rent.sign.line3","");
					line = line.replaceAll("<user>", owner);
					line = line.replaceAll("<region>", region.getId());
					lines[2] = line;
					
					line = RegionSigns.instance.getConfig().getString("rent.sign.line4","");
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
		double downPayment = (Double)state.Argument2;
		
		double intervalPayment = 0;
		long intervalLength = 0;
		
		// Parse the cost per period and the period length
		if(((String)state.Argument3).indexOf(":") != -1)
		{
			// This does contain the period
			String perIntervalPriceString = ((String)state.Argument3).substring(0,((String)state.Argument3).indexOf(":"));
			String intervalLengthString = ((String)state.Argument3).substring(((String)state.Argument3).indexOf(":")+1);
			
			// There is one char at the beginning that is the currency symbol
			if(perIntervalPriceString.startsWith(Util.sCurrencyChar))
			{
				try 
				{ 
					intervalPayment = Double.parseDouble(perIntervalPriceString.substring(1));
				} 
				catch(NumberFormatException e){}
			}
			else
			{
				// Maybe its just the number
				try 
				{ 
					intervalPayment = Double.parseDouble(perIntervalPriceString);
				} 
				catch(NumberFormatException e){}
			}
			
			// attempt to parse the period
			try
			{
				intervalLength = Util.parseDateDiff(intervalLengthString);
			}
			catch(Exception e){}
		}
		else
		{
			// Just the cost
			// There is one char at the beginning that is the currency symbol
			if(((String)state.Argument3).startsWith(Util.sCurrencyChar))
			{
				try 
				{ 
					intervalPayment = Double.parseDouble(((String)state.Argument3).substring(1));
				} 
				catch(NumberFormatException e){}
			}
			else
			{
				// Maybe its just the number
				try 
				{ 
					intervalPayment = Double.parseDouble(((String)state.Argument3));
				} 
				catch(NumberFormatException e){}
			}
			
			try
			{
				intervalLength = Util.parseDateDiff(RegionSigns.instance.getConfig().getString("rent.defaultperiod"));
			}
			catch(Exception e){}
		}
		
		if(region == null)
			return;
		
		RentSignCreateEvent event = new RentSignCreateEvent(region, state.SignLocation.clone(), downPayment, intervalPayment, intervalLength);
		
		Bukkit.getPluginManager().callEvent(event);
	}
	
	@Override
	protected void onSignDestroyed( InteractableSignState state )
	{
		ProtectedRegion region = Util.getRegion(state.SignLocation.getWorld(), (String)state.Argument1);
		
		if(region == null)
			return;
		
		RentSignDestroyEvent event = new RentSignDestroyEvent(region, state.SignLocation.clone());
		
		Bukkit.getPluginManager().callEvent(event);
	}
}