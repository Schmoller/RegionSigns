package au.com.mineauz.RegionSigns.commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;

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
					label + Util.translateColours(" %gold%create <type> <world> %gold%<region>"),
					label + Util.translateColours(" %gold%set <world> %gold%<region> default %white%(%gold%all%white%|%gold%none%white%|%gold%op%white%|%gold%notop%white%)"),
					label + Util.translateColours(" %gold%set <world> %gold%<region> message <message>"),
					label + Util.translateColours(" %gold%delete <world> %gold%<region>")
			};
		}
		else
		{
			return new String[] {
					label + Util.translateColours(" %gold%create <type> %green%[world] %gold%<region>"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> default %white%(%gold%all%white%|%gold%none%white%|%gold%op%white%|%gold%notop%white%)"),
					label + Util.translateColours(" %gold%set %green%[world] %gold%<region> message <message>"),
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
		return false;
	}
	private boolean handleSet(CommandSender sender, String[] args)
	{
		return false;
	}
	private boolean handleDelete(CommandSender sender, String[] args)
	{
		return false;
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
