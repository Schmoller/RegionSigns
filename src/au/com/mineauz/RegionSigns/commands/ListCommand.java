package au.com.mineauz.RegionSigns.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentStatus;

public class ListCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "list";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "regionsigns.rent.list.me";
	}

	@Override
	public String getUsageString( String label, CommandSender sender )
	{
		return label + ChatColor.GOLD + " <who>" + ChatColor.GREEN + " [page]";
	}

	@Override
	public String getDescription()
	{
		return  "Lists all the regions rented by the target.\n" + 
			    "<who> can be:\n" + 
				" - me: Lists all your rented regions\n" + 
			    " - all: Lists all the rented regions\n" + 
				" - .<player>: Lists <player>'s rented regions";
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
		if (args.length != 1 && args.length != 2)
			return false;
		
		// The number of items to show on a page
		int cItemsPerPage = 6;
		// The console can show more at a time
		if(sender instanceof ConsoleCommandSender)
			cItemsPerPage = 40;
		
		int page = 0;
		String mode = args[0];
		
		// Get the page number
		if(args.length == 2)
		{
			// Check its format
			if(!args[1].matches("[0-9]+"))
				return false;
			
			// Parse it
			try
			{
				page = Integer.parseInt(args[1]) - 1;
			}
			catch(NumberFormatException e) {}
			
			if(page == 0)
				return false;
		}
		
		// Build the results list
		ArrayList<String> results = new ArrayList<String>();

		// All results are requested
		if(mode.compareToIgnoreCase("all") == 0)
		{
			// Check permissions
			if(!sender.hasPermission("regionsigns.rent.list.all"))
			{
				sender.sendMessage(ChatColor.RED + "You dont have permission to use that");
				return true;
			}
			
			// get all the results
			for(RentStatus status : RentManager.instance.getRenters())
			{
				results.add(String.format("%-30s[%s] %s", status.Region,status.World, status.Tenant.getName()));
			}
		}
		// Only regions that the calling player rents
		else if(mode.compareToIgnoreCase("me") == 0)
		{
			if(!(sender instanceof Player))
			{
				sender.sendMessage("A player is required to use this command");
				return true;
			}

			// get all the results
			for(RentStatus status : RentManager.instance.getRenters())
			{
				if(status.Tenant.equals((Player)sender))
						results.add(String.format("%-30s[%s] %s", status.Region,status.World, status.Tenant.getName()));
			}
		}
		else if(mode.startsWith("."))
		{
			if(!sender.hasPermission("regionsigns.rent.list.others"))
			{
				sender.sendMessage(ChatColor.RED + "You dont have permission to use that");
				return true;
			}
			
			OfflinePlayer player = Bukkit.getOfflinePlayer(mode.substring(1));
			
			// get all the results
			
			for(RentStatus status : RentManager.instance.getRenters())
			{
				if(status.Tenant.equals(player))
						results.add(String.format("%-30s[%s] %s", status.Region,status.World, status.Tenant.getName()));
			}
		}
		
		int totalPages = (int)Math.ceil(results.size()/cItemsPerPage); 
		// Make sure they dont specify a page that doesnt exist
		if(page > totalPages)
			page = totalPages;
		
		// Display the results
		sender.sendMessage("----Rented Regions----Page " + (page + 1) + " of " + (totalPages + 1));
		for(int i = (page * cItemsPerPage); i < (page * cItemsPerPage + cItemsPerPage) && i < results.size(); i++)
		{
			sender.sendMessage(results.get(i));
		}
		if(page != totalPages)
			sender.sendMessage("Use /rent list " + mode + " " + (page + 2));
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
