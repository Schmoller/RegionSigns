package au.com.mineauz.RegionSigns.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentMessage;
import au.com.mineauz.RegionSigns.rent.RentMessageTypes;
import au.com.mineauz.RegionSigns.rent.RentStatus;

public class ForceStopCommand implements ICommand
{

	@Override
	public String getName()
	{
		return "forcestop";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "regionsigns.rent.forcestop";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		if(sender instanceof Player || sender instanceof BlockCommandSender)
			return new String[] {label + ChatColor.GREEN + " [world]" + ChatColor.GOLD +  " <region>"};
		else
			return new String[] {label + ChatColor.GOLD + " <world> <region>"};
	}

	@Override
	public String getDescription()
	{
		return "Stops renting of <region> by the tenant and removes them immediatly from the region with no further payment.";
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
				if(!sender.hasPermission("regionsigns.rent.forcestop.others") && (sender instanceof Player))
				{
					if(!status.Tenant.equals((Player)sender))
					{
						sender.sendMessage(ChatColor.RED + "You do not have permission to forcestop the rent of other players.");
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
			// Remove the tenant
			ProtectedRegion regionObject = Util.getRegion(Bukkit.getWorld(regionStatus.World),regionStatus.Region);
			
			regionObject.setMembers(new DefaultDomain());
			
			Util.saveRegionManager(Bukkit.getWorld(regionStatus.World));
			
			// remove the rent status
			RentManager.instance.removeRent(regionStatus);
			
			// The lease is terminated now
			RentMessage msg = new RentMessage();
			msg.Type = RentMessageTypes.RentEnded;
			msg.EventCompletionTime = 0;
			msg.Region = regionStatus.Region;
			msg.Payment = 0;
			
			RentMessage msg2 = new RentMessage();
			msg2.Type = RentMessageTypes.RentEndedLandlord;
			msg2.EventCompletionTime = 0;
			msg2.Region = regionStatus.Region;
			msg2.Payment = 0;

			RentManager.instance.sendMessage(msg,Bukkit.getOfflinePlayer(regionStatus.Tenant));
			RentManager.instance.sendMessageToLandlords(msg2, regionStatus);
			sender.sendMessage(ChatColor.GREEN + regionStatus.Tenant + " finished renting '" + regionStatus.Region + "' and has been removed from it");
			
			// Update the sign
			if(regionStatus.SignLocation != null && (regionStatus.SignLocation.getBlock().getType() == Material.WALL_SIGN || regionStatus.SignLocation.getBlock().getType() == Material.SIGN_POST))
			{
				Sign sign = (Sign)regionStatus.SignLocation.getBlock().getState();
				
				for(int i = 0; i < 4; ++i)
				{
					String line = RegionSigns.config.unclaimedSign[i];
					line = line.replaceAll("<region>", regionStatus.Region);
					
					sign.setLine(i, line);
				}
				
				sign.update(true);
			}
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}
}
