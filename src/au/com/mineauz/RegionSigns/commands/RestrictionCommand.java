package au.com.mineauz.RegionSigns.commands;

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
					label + Util.translateColours(" %gold%delete <world> %gold%<region>")
			};
		}
		else
		{
			return new String[] {
					label + Util.translateColours(" %gold%create %green%[world] %gold%<region> <type> <default>"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> default %white%(%gold%all%white%|%gold%none%white%|%gold%op%white%|%gold%notop%white%)"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> message <message>"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> limit %white%(%gold%<amount>%white%|%gold%none%white%)"),
					label + Util.translateColours(" %gold%delete %green%[world] %gold%<region>")
			};
		}
	}

	@Override
	public String getDescription()
	{
		return "Creates, edits, and deletes region restrictions";
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
		
		if(region.getProtectedRegion() == null)
		{
			sender.sendMessage(ChatColor.RED + "That region does not exist.");
			return true;
		}
		
		
		// Get the restriction
		if(RegionSigns.restrictionManager.getRestriction(region) == null)
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
	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if (args.length == 0)
			return false;
		
		if(args[0].equalsIgnoreCase("create"))
		{
			return handleCreate(sender, Arrays.copyOfRange(args, 1, args.length));
		}
		else if(args[0].equalsIgnoreCase("set"))
		{
			return handleSet(sender, Arrays.copyOfRange(args, 1, args.length));
		}
		else if(args[0].equalsIgnoreCase("delete"))
		{
			return handleDelete(sender, Arrays.copyOfRange(args, 1, args.length));
		}
		else
			return false;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		if(args.length == 1)
		{
			if(args[0].trim().isEmpty())
				return Arrays.asList(new String[] {"create", "set", "delete"});
			
			if("create".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"create"});
			if("set".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"set"});
			if("delete".startsWith(args[0].toLowerCase()))
				return Arrays.asList(new String[] {"delete"});
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
