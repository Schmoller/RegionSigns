package au.com.mineauz.RegionSigns.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentStatus;
import au.com.mineauz.RegionSigns.rent.RentTransferConfirmation;

public class TransferCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "transfer";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "regionsigns.rent.transfer";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		if(sender instanceof Player || sender instanceof BlockCommandSender)
			return new String[] {label + ChatColor.GREEN + " [world]" + ChatColor.GOLD +  " <region> <player>"};
		else
			return new String[] {label + ChatColor.GOLD + " <world> <region> <player>"};
	}

	@Override
	public String getDescription()
	{
		return "Transfers tennantship of <region> to <player>. Both the original tenant and the new tenant must accept";
	}
	
	@Override
	public boolean canBeConsole()
	{
		return true;
	}
	
	@Override
	public boolean canBeCommandBlock()
	{
		return false;
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
		String newOwner = "";
		
		if (args.length == 2)
		{
			if (sender instanceof Player)
				world = ((Player)sender).getWorld().getName();
			else if(sender instanceof BlockCommandSender)
				world = ((BlockCommandSender)sender).getBlock().getWorld().getName();
			region = args[0];
			newOwner = args[1];
		}
		else
		{
			world = args[0];
			region = args[1];
			newOwner = args[2];
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
			Player currentTenant, newTenant;
			
			currentTenant = Bukkit.getPlayerExact(regionStatus.Tenant);
			newTenant = Bukkit.getPlayerExact(newOwner);
			
			// Check that they are online
			if(currentTenant == null || !currentTenant.isOnline())
			{
				sender.sendMessage(ChatColor.RED + regionStatus.Tenant + " is not online. They must be online to transfer their tenantship.");
				return true;
			}
			if(newTenant == null || !newTenant.isOnline())
			{
				sender.sendMessage(ChatColor.RED + newOwner + " is not online or does not exist. They must be online to transfer the tenantship.");
				return true;
			}
			
			sender.sendMessage("Both parties have been informed and are now required to accept before transfer will be complete.");
			new RentTransferConfirmation(regionStatus, currentTenant, newTenant, sender);
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
