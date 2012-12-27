package au.com.mineauz.RegionSigns.commands;

import java.util.Calendar;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentMessage;
import au.com.mineauz.RegionSigns.rent.RentMessageTypes;
import au.com.mineauz.RegionSigns.rent.RentStatus;

public class StopCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "stop";
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
	public String getUsageString( String label, CommandSender sender )
	{
		if(sender instanceof Player || sender instanceof BlockCommandSender)
			return label + ChatColor.GREEN + " [world]" + ChatColor.GOLD +  " <region>";
		else
			return label + ChatColor.GOLD + " <world> <region>";
	}

	@Override
	public String getDescription()
	{
		return "Stops renting of <region> by the tenant";
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
				// Check if they are allowed to stop others' rent
				if(!sender.hasPermission("regionsigns.rent.stop.others") && (sender instanceof Player))
				{
					if(!status.Tenant.equals((Player)sender))
					{
						sender.sendMessage(ChatColor.RED + "You do not have permission to stop the rent of other players.");
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
			if(RentManager.MinimumRentPeriod != 0 && (Calendar.getInstance().getTimeInMillis() - regionStatus.Date < RentManager.MinimumRentPeriod) && !sender.hasPermission("regionsigns.rent.nominperiod"))
			{
				if(sender instanceof Player && ((Player)sender).equals(regionStatus.Tenant))
					sender.sendMessage(ChatColor.RED + "You cannot stop renting this region yet. You are required to rent for at least " + Util.formatTimeDifference(RentManager.MinimumRentPeriod - (Calendar.getInstance().getTimeInMillis() - regionStatus.Date), 2, false) + " more");
				else
					sender.sendMessage(ChatColor.RED + regionStatus.Tenant.getName() + " cannot stop renting this region yet. They are required to rent for at least " + Util.formatTimeDifference(RentManager.MinimumRentPeriod - (Calendar.getInstance().getTimeInMillis() - regionStatus.Date), 2, false) + " more");
				
				return true;
			}
			
			if(!Util.playerHasEnough(regionStatus.Tenant, regionStatus.IntervalPayment))
			{
				if(sender instanceof Player && ((Player)sender).equals(regionStatus.Tenant))
					sender.sendMessage(ChatColor.RED + "You cannot stop renting this region yet. You cannot afford to pay your next rent.");
				else
					sender.sendMessage(ChatColor.RED + regionStatus.Tenant.getName() + " cannot stop renting this region yet. They cannot afford to pay their next rent.");
				
				return true;
			}
			
			Util.playerSubtractMoney(regionStatus.Tenant, regionStatus.IntervalPayment);
			
			// They are required to pay their next rent and will be removed from the lot at the next rent interval
			RentMessage msg = new RentMessage();
			msg.Type = RentMessageTypes.RentEnding;
			msg.EventCompletionTime = regionStatus.NextIntervalEnd;
			msg.Region = regionStatus.Region;
			msg.Payment = regionStatus.IntervalPayment;
			
			regionStatus.PendingRemoval = true;
			RentManager.instance.sendMessage(msg, regionStatus.Tenant);
			
			sender.sendMessage(ChatColor.GREEN + regionStatus.Tenant.getName() + " ended renting '" + regionStatus.Region + "'. They will be removed once their rent period is up");
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
