package au.com.mineauz.RegionSigns.commands;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentStatus;

public class InfoCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "info";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "regionsigns.use.rent";
	}

	@Override
	public String getUsageString( String label, CommandSender sender)
	{
		if(sender instanceof Player || sender instanceof BlockCommandSender)
			return label + ChatColor.GREEN + " [world]" + ChatColor.GOLD +  " <region>";
		else
			return label + ChatColor.GOLD + " <world> <region>";
	}
	
	@Override
	public String getDescription()
	{
		return "Displays information about a rented region";
	}

	@Override
	public boolean canBeConsole()
	{
		return true;
	}
	
	@Override
	public boolean canBeCommandBlock()
	{
		return true;
	}

	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if(sender instanceof Player || sender instanceof BlockCommandSender)
		{
			if(args.length == 0 || args.length > 2)
				return false;
		}
		else
		{
			if(args.length != 2)
				return false;
		}
		
		String world = "";
		String region = "";
		
		if (args.length == 1)
		{
			if (sender instanceof Player)
				world = ((Player)sender).getWorld().getName();
			else if(sender instanceof BlockCommandSender)
				world = ((BlockCommandSender)sender).getBlock().getWorld().getName();
			region = args[0];
		}
		else
		{
			world = args[0];
			region = args[1];
		}
		
		RentStatus regionStatus = null;
		
		// Find a matching region
		for(RentStatus status : RentManager.instance.getRenters())
		{
			if(status.Region.equalsIgnoreCase(region) && status.World.equalsIgnoreCase(world))
			{
				// Check if they are allowed to look at others' regions
				if(!sender.hasPermission("regionsigns.rent.info.others") && (sender instanceof Player))
				{
					if(!status.Tenant.equals((Player)sender))
					{
						sender.sendMessage(ChatColor.RED + "You do not have permission to view to status of other players rent.");
						return true;
					}
				}
				
				regionStatus = status;
				break;
			}
		}
		
		if(regionStatus == null)
			sender.sendMessage(ChatColor.RED + "Unable to find a rented region with that id");
		else
		{
			sender.sendMessage("Rent Status for region '" + regionStatus.Region + "'");
			sender.sendMessage("  Tenant:          " + regionStatus.Tenant.getName());
			String status =    "  Status:          ";
			
			if(regionStatus.PendingEviction)
				status += ChatColor.RED + "Pending Eviction";
			else if(regionStatus.PendingRemoval)
				status += ChatColor.GOLD + "Ending Rent";
			else
				status += ChatColor.GREEN + "Normal";
			
			sender.sendMessage(status);
			if(regionStatus.PendingEviction)
			{
				sender.sendMessage("  Evicted In: " + Util.formatTimeDifference(regionStatus.NextIntervalEnd - regionStatus.RentInterval - Calendar.getInstance().getTimeInMillis(),2, false));
				sender.sendMessage("  Next Payment:    " + Util.formatCurrency(regionStatus.IntervalPayment));
			}
			else if(regionStatus.PendingRemoval)
			{
				sender.sendMessage("  Terminated In: " + Util.formatTimeDifference(regionStatus.NextIntervalEnd - Calendar.getInstance().getTimeInMillis(),2, false));
			}
			else
			{
				sender.sendMessage("  Next Payment In: " + Util.formatTimeDifference(regionStatus.NextIntervalEnd - Calendar.getInstance().getTimeInMillis(),2, false));
				sender.sendMessage("  Next Payment:    " + Util.formatCurrency(regionStatus.IntervalPayment));
			}
				
			
			if(regionStatus.Date != 0)
				sender.sendMessage("  Renting Since:   " + DateFormat.getDateTimeInstance().format(new Date(regionStatus.Date)));
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
