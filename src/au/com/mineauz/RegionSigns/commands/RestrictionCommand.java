package au.com.mineauz.RegionSigns.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import au.com.mineauz.RegionSigns.Region;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Restriction;
import au.com.mineauz.RegionSigns.RestrictionType;
import au.com.mineauz.RegionSigns.Util;

public class RestrictionCommand implements ICommand
{
	@Override
	public String getName()
	{
		return "restriction";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "regionsigns.restrictions.edit";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		if(sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)
		{
			return new String[] {
					label + Util.translateColours(" %gold%create <world> %gold%<region> <type> <default>"),
					label + Util.translateColours(" %gold%set <world> %gold%<region> default %white%(%gold%all%white%|%gold%none%white%|%gold%op%white%|%gold%notop%white%)"),
					label + Util.translateColours(" %gold%set <world> %gold%<region> message <message>"),
					label + Util.translateColours(" %gold%set <world> %gold%<region> limit %white%(%gold%<amount>%white%|%gold%none%white%)"),
					label + Util.translateColours(" %gold%delete <world> %gold%<region>"),
					label + Util.translateColours(" %gold%list <world> %green%[page]"),
					label + Util.translateColours(" %gold%info <world> <region>")
			};
		}
		else
		{
			return new String[] {
					label + Util.translateColours(" %gold%create %green%[world] %gold%<region> <type> <default>"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> default %white%(%gold%all%white%|%gold%none%white%|%gold%op%white%|%gold%notop%white%)"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> message <message>"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> limit %white%(%gold%<amount>%white%|%gold%none%white%)"),
					label + Util.translateColours(" %gold%delete %green%[world] %gold%<region>"),
					label + Util.translateColours(" %gold%list %green%[world] %green%[page]"),
					label + Util.translateColours(" %gold%info %green%[world] %gold%<region>")
			};
		}
	}

	@Override
	public String getDescription()
	{
		return "Creates, edits, deletes, and lists region restrictions";
	}

	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean canBeCommandBlock() { return false; }

	private boolean handleCreate(CommandSender sender, String[] args)
	{
		if(args.length < 2 || args.length > 4)
			return false;
		
		World world = null;
		
		int i = 0;
		if(args.length == 4)
		{
			// World is included
			world = Bukkit.getWorld(args[i]);
			
			if(world == null)
			{
				sender.sendMessage(ChatColor.RED + "The world " + args[i] + " does not exist");
				return true;
			}
			++i;
		}
		else
		{
			if(!args[i].equalsIgnoreCase("__global__"))
			{
				if(!(sender instanceof Player))
					return false;
				
				world = ((Player)sender).getWorld();
			}
		}
		
		String regionId = args[i];
		
		RestrictionType type;
		if(args[i+1].equalsIgnoreCase("all"))
			type = RestrictionType.All;
		else if(args[i+1].equalsIgnoreCase("claim"))
			type = RestrictionType.Claim;
		else if(args[i+1].equalsIgnoreCase("rent"))
			type = RestrictionType.Rent;
		else
		{
			sender.sendMessage(ChatColor.RED + "Expected restriction type (all,claim,rent). Got " + args[i+1]);
			return true;
		}
		
		PermissionDefault def = null;
		
		if(args[i+2].equalsIgnoreCase("none"))
			def = PermissionDefault.FALSE;
		else if(args[i+2].equalsIgnoreCase("all"))
			def = PermissionDefault.TRUE;
		else
		{
			def = PermissionDefault.getByName(args[i+2].toUpperCase());
			
			if(def == null)
			{
				sender.sendMessage(ChatColor.RED + "Invalid permission type " + args[i+2] + ". Expected none,all,op, or notop");
				return true;
			}
		}
		
		Region region = new Region(world,  regionId);
		
		if(!region.isGlobal() && region.getProtectedRegion() == null)
		{
			sender.sendMessage(ChatColor.RED + "The region " + regionId + " does not exist");
			return true;
		}
		
		// Create it
		if(RegionSigns.restrictionManager.addRestriction(region, type, def))
		{
			sender.sendMessage(ChatColor.GREEN + "Restriction created. The permission for this restriction is: " + ChatColor.WHITE + "\n" + RegionSigns.restrictionManager.getRestriction(region).permission.getName() + "\n The default value is: " + def.toString());
			RegionSigns.restrictionManager.saveRestrictions();
		}
		else
		{
			if(RegionSigns.restrictionManager.getRestriction(region) != null)
				sender.sendMessage(ChatColor.RED + "Failed to create restriction because one already exists for that region.");
			else
				sender.sendMessage(ChatColor.RED + "Failed to create restriction.");
		}
		
		return true;
	}
	private boolean handleSet(CommandSender sender, String[] args)
	{
		if(args.length < 3)
			return false;
		
		World world = Bukkit.getWorld(args[0]);
		String regionId;
		
		int i = 0;
		if(world == null)
		{
			if(!args[i].equalsIgnoreCase("__global__"))
			{
				if(!(sender instanceof Player))
					return false;
				
				world = ((Player)sender).getWorld();
			}
		}
		else
			++i;
		
		regionId = args[i];
		
		Region region = new Region(world, regionId);
		
		if(!region.isGlobal() && region.getProtectedRegion() == null)
		{
			sender.sendMessage(ChatColor.RED + "That region does not exist.");
			return true;
		}
		
		
		// Get the restriction
		if(RegionSigns.restrictionManager.getRestrictionExact(region) == null)
		{
			sender.sendMessage(ChatColor.RED + "There is no restriction on that region.");
			return true;
		}
		
		
		// Apply the new values
		if(args[i+1].equalsIgnoreCase("default"))
		{
			if(args.length <= i+2)
				return false;
			
			boolean ok = false;
			if(args[i+2].equalsIgnoreCase("none"))
				ok = RegionSigns.restrictionManager.setRestrictionDefault(region, PermissionDefault.FALSE);
			else if(args[i+2].equalsIgnoreCase("all"))
				ok = RegionSigns.restrictionManager.setRestrictionDefault(region, PermissionDefault.TRUE);
			else if(args[i+2].equalsIgnoreCase("op"))
				ok = RegionSigns.restrictionManager.setRestrictionDefault(region, PermissionDefault.OP);
			else if(args[i+2].equalsIgnoreCase("notop"))
				ok = RegionSigns.restrictionManager.setRestrictionDefault(region, PermissionDefault.NOT_OP);
			else
			{
				PermissionDefault def = PermissionDefault.getByName(args[i+2].toUpperCase());
				
				if(def == null)
				{
					sender.sendMessage(ChatColor.RED + "Invalid permission type " + args[i+2] + ". Expected none,all,op, or notop");
					return true;
				}
				else
					ok = RegionSigns.restrictionManager.setRestrictionDefault(region, def);
			}
			
			if(ok)
			{
				RegionSigns.restrictionManager.saveRestrictions();
				sender.sendMessage(ChatColor.GREEN + "The default permission for " + regionId + " was changed to " + args[i+2]);
			}
			else
				sender.sendMessage(ChatColor.RED + "Failed to change the default permission");
			
		}
		else if(args[i+1].equalsIgnoreCase("message"))
		{
			String message = "";
			
			for(int c = i+2; c < args.length; ++c)
				message += args[c] + " ";
			
			message = message.trim();
			
			if(RegionSigns.restrictionManager.setRestrictionMessage(region, message))
			{
				RegionSigns.restrictionManager.saveRestrictions();
				sender.sendMessage(ChatColor.GREEN + "The deny message for " + regionId + " was changed to '" + message + "'");
			}
			else
				sender.sendMessage(ChatColor.RED + "Failed to change the deny message");
		}
		else if(args[i+1].equalsIgnoreCase("limit"))
		{
			if(args.length <= i+2)
				return false;
			
			boolean ok = false;
			if(args[i+2].equalsIgnoreCase("none"))
				ok = RegionSigns.restrictionManager.setRestrictionOwnLimit(region, -1);
			else
			{
				try
				{
					int limit = Integer.parseInt(args[i+2]);
					if(limit <= 0)
					{
						sender.sendMessage(ChatColor.RED + "Invalid limit amount " + args[i+2] + ". Expected integer greater than 0 or 'none'");
						return true;
					}
					ok = RegionSigns.restrictionManager.setRestrictionOwnLimit(region, limit);
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + "Invalid limit amount " + args[i+2] + ". Expected integer greater than 0 or 'none'");
					return true;
				}
			}
			
			if(ok)
			{
				RegionSigns.restrictionManager.saveRestrictions();
				sender.sendMessage(ChatColor.GREEN + "The ownership limit for " + regionId + " was changed to " + args[i+2]);
			}
			else
				sender.sendMessage(ChatColor.RED + "Failed to change the ownership limit");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Expected default,message, or limit as Argument " + (i+2));
		}
		
		return true;
	}
	private boolean handleDelete(CommandSender sender, String[] args)
	{
		if(args.length < 1 || args.length > 2)
			return false;
		
		World world = null;
		
		int i = 0;
		if(args.length == 2)
		{
			// World is included
			world = Bukkit.getWorld(args[0]);
			
			if(world == null)
			{
				sender.sendMessage(ChatColor.RED + "The world " + args[0] + " does not exist");
				return true;
			}
			++i;
		}
		else
		{
			if(!args[i].equalsIgnoreCase("__global__"))
			{
				if(!(sender instanceof Player))
					return false;
				
				world = ((Player)sender).getWorld();
			}
		}
		
		String regionId = args[i];
		
		Region region = new Region(world,  regionId);
		
		// Remove it
		if(RegionSigns.restrictionManager.removeRestriction(region))
		{
			sender.sendMessage(ChatColor.GREEN + "Restriction removed.");
			RegionSigns.restrictionManager.saveRestrictions();
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "There is no such restriction.");
		}
		
		return true;
	}
	
	private boolean handleList(CommandSender sender, String[] args)
	{
		if (args.length > 2)
			return false;
		
		// The number of items to show on a page
		int cItemsPerPage = 6;
		// The console can show more at a time
		if(sender instanceof ConsoleCommandSender)
			cItemsPerPage = 40;
		
		int page = 0;
		
		// Get the world
		World world = null;
		boolean all = false;
		
		if(args.length > 0)
		{
			// Could be the page number
			if(args.length != 1 || !args[0].matches("[1-9][0-9]*"))
			{
				if(args[0].equalsIgnoreCase("__global__"))
					all = true;
				else
				{
					// World is included
					world = Bukkit.getWorld(args[0]);
					
					if(world == null)
					{
						sender.sendMessage(ChatColor.RED + "The world " + args[0] + " does not exist");
						return true;
					}
				}
			}
		}
		
		if (world == null && !all)
		{
			if(sender instanceof Player)
			{
				world = ((Player)sender).getWorld();
			}
		}
		
		// Get the page number
		if(args.length > 0)
		{
			// Check its format
			if(args[args.length-1].matches("[1-9][0-9]*"))
			{
				// Parse it
				try
				{
					page = Integer.parseInt(args[args.length-1]) - 1;
				}
				catch(NumberFormatException e) {}
			}
			else if(args.length == 2)
			{
				return false;
			}
		}
		
		// Build the results list
		ArrayList<String> results = new ArrayList<String>();

		// get all the results
		for(Restriction restriction : RegionSigns.restrictionManager.getRestrictions())
		{
			if(world != null && !world.equals(restriction.region.getWorld()))
				continue;
			
			String text = "";
			if(restriction.region.isSuperGlobal())
				text += String.format("%%yellow%%%s\n", restriction.region.getID());
			else
				text += String.format("%%yellow%%%s %%red%%[%s]\n", restriction.region.getID(), restriction.region.getWorld().getName());

			text += "  %gold%Affects: %gray%";
			switch(restriction.type)
			{
			case All:
				text += "Claiming and Renting";
				break;
			case Claim:
				text += "Claiming";
				break;
			case Rent:
				text += "Renting";
				break;
			}
			
			text += "\n";
			if(restriction.permission != null)
			{
				text += String.format("  %%gold%%Permission: %%white%%%s\n", restriction.permission.getName());
				text += String.format("  %%gold%%Default: %%gray%%%s\n", restriction.permission.getDefault().toString());
			}

			if(restriction.maxCount > 0)
				text += String.format("  %%gold%%Ownership limit: %%gray%%%d", restriction.maxCount);
			
			text += "\n";
			results.add(Util.translateColours(text));
		}
		
		int totalPages = (int)Math.ceil(results.size()/cItemsPerPage); 
		// Make sure they dont specify a page that doesnt exist
		if(page > totalPages)
			page = totalPages;
		
		if(results.size() == 0)
		{
			sender.sendMessage("There are no restrictions to display");
		}
		else
		{
			// Display the results
			sender.sendMessage("----Restrictions----Page " + (page + 1) + " of " + (totalPages + 1));
			for(int i = (page * cItemsPerPage); i < (page * cItemsPerPage + cItemsPerPage) && i < results.size(); i++)
			{
				sender.sendMessage(results.get(i));
			}
			if(page != totalPages)
			{
				if(world != null)
					sender.sendMessage("Use /rent restriction list " + world.getName() + " " + (page + 2));
				else if(all)
					sender.sendMessage("Use /rent restriction list __global__ " + (page + 2));
				else
					sender.sendMessage("Use /rent restriction list " + (page + 2));
			}
		}
		
		return true;
	}
	
	private boolean handleInfo(CommandSender sender, String[] args)
	{
		if(args.length < 1 || args.length > 2)
			return false;
		
		World world = null;
		
		int i = 0;
		if(args.length == 2)
		{
			// World is included
			world = Bukkit.getWorld(args[i]);
			
			if(world == null)
			{
				sender.sendMessage(ChatColor.RED + "The world " + args[i] + " does not exist");
				return true;
			}
			++i;
		}
		else
		{
			if(!args[i].equalsIgnoreCase("__global__"))
			{
				if(!(sender instanceof Player))
					return false;
				
				world = ((Player)sender).getWorld();
			}
		}
		
		String regionId = args[i];
		Region region = new Region(world, regionId);
		
		Restriction restriction = RegionSigns.restrictionManager.getRestrictionExact(region);
		
		if(restriction == null)
		{
			sender.sendMessage(ChatColor.RED + "There is no restriction on that region.");
			return true;
		}
		
		if(restriction.region.isSuperGlobal())
			sender.sendMessage(Util.translateColours(String.format("%%yellow%%%s", restriction.region.getID())));
		else
			sender.sendMessage(Util.translateColours(String.format("%%yellow%%%s %%red%%[%s]", restriction.region.getID(), restriction.region.getWorld().getName())));
		
		String text = "  %gold%Affects: %gray%";
		switch(restriction.type)
		{
		case All:
			text += "Claiming and Renting";
			break;
		case Claim:
			text += "Claiming";
			break;
		case Rent:
			text += "Renting";
			break;
		}
		
		sender.sendMessage(Util.translateColours(text));
		
		if(restriction.permission != null)
		{
			sender.sendMessage(Util.translateColours(String.format("  %%gold%%Permission: %%white%%%s", restriction.permission.getName())));
			sender.sendMessage(Util.translateColours(String.format("  %%gold%%Default: %%gray%%%s\n", restriction.permission.getDefault().toString())));
		}

		if(restriction.maxCount > 0)
			sender.sendMessage(Util.translateColours(String.format("  %%gold%%Ownership limit: %%gray%%%d", restriction.maxCount)));
		
		return true;
	}
	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if (args.length == 0)
			return false;
		
		if(args[0].equalsIgnoreCase("create"))
			return handleCreate(sender, Arrays.copyOfRange(args, 1, args.length));
		else if(args[0].equalsIgnoreCase("set"))
			return handleSet(sender, Arrays.copyOfRange(args, 1, args.length));
		else if(args[0].equalsIgnoreCase("delete"))
			return handleDelete(sender, Arrays.copyOfRange(args, 1, args.length));
		else if(args[0].equalsIgnoreCase("list"))
			return handleList(sender, Arrays.copyOfRange(args, 1, args.length));
		else if(args[0].equalsIgnoreCase("info"))
			return handleInfo(sender, Arrays.copyOfRange(args, 1, args.length));
		else
			return false;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		if(args.length == 1)
		{
			if(args[0].trim().isEmpty())
				return Arrays.asList(new String[] {"create", "set", "delete", "list", "info"});
			
			if("create".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"create"});
			if("set".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"set"});
			if("delete".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"delete"});
			if("list".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"list"});
			if("info".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"info"});
		}
		
		if(args.length == 2 && args[0].equalsIgnoreCase("create"))
		{
			if(args[1].trim().isEmpty())
				return Arrays.asList(new String[] {"all","claim","rent"});
			
			if("all".startsWith(args[1].toLowerCase()))
				return Arrays.asList(new String[] {"all"});
			if("claim".startsWith(args[1].toLowerCase()))
				return Arrays.asList(new String[] {"claim"});
			if("rent".startsWith(args[1].toLowerCase()))
				return Arrays.asList(new String[] {"rent"});
		}
		return null;
	}

}
