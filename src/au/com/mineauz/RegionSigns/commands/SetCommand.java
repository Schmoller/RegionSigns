package au.com.mineauz.RegionSigns.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentStatus;

public class SetCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "set";
	}

	@Override
	public String[] getAliases()
	{
		return new String[] {"setrent","changerent"};
	}

	@Override
	public String getPermission()
	{
		return "regionsigns.rent.set";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		if(sender instanceof Player || sender instanceof BlockCommandSender)
			return new String[] {label + ChatColor.GREEN + " [world]" + ChatColor.GOLD + " <region> <amount>"};
		else
			return new String[] {label + ChatColor.GOLD + " <world> <region> <amount>"};
	}
	
	@Override
	public String getDescription()
	{
		return "Changes the amount of rent the tenant of <region> must pay";
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
			if(args.length != 2 && args.length != 3)
				return false;
		}
		else
		{
			if(args.length != 3)
				return false;
		}
		
		String world = "";
		String region = "";
		String amountStr = "";
		
		if (args.length == 2)
		{
			if (sender instanceof Player)
				world = ((Player)sender).getWorld().getName();
			else if(sender instanceof BlockCommandSender)
				world = ((BlockCommandSender)sender).getBlock().getWorld().getName();
			region = args[0];
			amountStr = args[1];
		}
		else
		{
			world = args[0];
			region = args[1];
			amountStr = args[2];
		}
		
		RentStatus regionStatus = null;
		
		// Find a matching region
		for(RentStatus status : RentManager.instance.getRenters())
		{
			if(status.Region.equalsIgnoreCase(region) && status.World.equalsIgnoreCase(world))
			{
				regionStatus = status;
				break;
			}
		}
		
		if(regionStatus == null)
			sender.sendMessage(ChatColor.RED + "Unable to find a rented region with that id");
		else
		{
			// Try to parse the amount
			double amount = 0;
			try
			{
				amount = Util.parseCurrency(amountStr);
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(ChatColor.RED + "Price expected, String found instead.");
				return true;
			}
			
			regionStatus.IntervalPayment = amount;
			
			sender.sendMessage("Rent for region '" + regionStatus.Region + "' has been set to " + Util.formatCurrency(amount) + " per " + Util.formatTimeDifference(regionStatus.RentInterval, 1, true));
		}
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
