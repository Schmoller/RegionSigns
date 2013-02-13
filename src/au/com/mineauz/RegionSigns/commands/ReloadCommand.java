package au.com.mineauz.RegionSigns.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import au.com.mineauz.RegionSigns.RegionSigns;

public class ReloadCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "reload";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "regionsigns.config.reload";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label};
	}
	
	@Override
	public String getDescription()
	{
		return "Reloads the configuration for RegionSigns";
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
		if (args.length > 0)
			return false;
		
		if(RegionSigns.config.load())
		{
			RegionSigns.config.save();
			RegionSigns.restrictionManager.loadRestrictions();
			sender.sendMessage(ChatColor.GREEN + "Configuration Reloaded");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Failed to load configuration.");
		}
		
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
