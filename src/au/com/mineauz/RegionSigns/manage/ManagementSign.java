package au.com.mineauz.RegionSigns.manage;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.mineauz.RegionSigns.Region;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentStatus;

public class ManagementSign implements Listener
{
	public ManagementSign()
	{
		Bukkit.getPluginManager().registerEvents(this, RegionSigns.instance);
	}
	
	private String[] matchesSign(String[] lines, String[] template)
	{
		String player = "";
		String region = "";
		
		for(int i = 0; i < 4; ++i)
		{
			int userPos = template[i].indexOf("<user>");
			int regionPos = template[i].indexOf("<region>");
			
			String patLine = template[i].replaceAll("([^\\w\\s\\d\\<\\>])", "\\\\$1").replaceAll("<user>", "([\\\\w\\\\d]+)").replaceAll("<region>", "([\\\\w\\\\d]+)");
			Pattern pat = Pattern.compile(patLine);
			
			Matcher m = pat.matcher(lines[i]);
			if(!m.matches())
				return null;
			else
			{
				if(userPos != -1 && regionPos != -1)
				{
					if(userPos < regionPos)
					{
						player = m.group(1);
						region = m.group(2);
					}
					else
					{
						player = m.group(2);
						region = m.group(1);
					}
				}
				else if(userPos != -1)
				{
					player = m.group(1);
				}
				else if(regionPos != -1)
				{
					region = m.group(1);
				}
			}
		}
		
		return new String[] {player, region};
	}
	
	public boolean matchesSign(String[] lines)
	{
		String[] claimStyle = RegionSigns.config.claimSign;
		String[] rentStyle = RegionSigns.config.rentSign;
		
		if(matchesSign(lines, claimStyle) != null)
			return true;
		return matchesSign(lines, rentStyle) != null;
	}
	
	private String getRegionName(String[] lines)
	{
		String[] claimStyle = RegionSigns.config.claimSign;
		String[] rentStyle = RegionSigns.config.rentSign;
		
		String[] values;
		values = matchesSign(lines, claimStyle);
		
		if(values == null)
			values = matchesSign(lines, rentStyle);
		
		return values[1];
	}
	
	private String getPlayerName(String[] lines)
	{
		String[] claimStyle = RegionSigns.config.claimSign;
		String[] rentStyle = RegionSigns.config.rentSign;
		
		String[] values;
		values = matchesSign(lines, claimStyle);
		
		if(values == null)
			values = matchesSign(lines, rentStyle);
		
		return values[0];
	}
	
	public static void displayStatus(Player player, ProtectedRegion region, DefaultDomain owners, RentStatus status)
	{
		ArrayList<String> messages = new ArrayList<String>();
		
		messages.add("Region Status: ");
		messages.add("  Region: " + ChatColor.YELLOW + region.getId());
		
		String ownerList = Util.makeNameList(owners.getPlayers(), player.getName(), "You");
		
		if(status != null)
			messages.add("  Tenants: " + ChatColor.YELLOW + ownerList);
		else
			messages.add("  Owners: " + ChatColor.YELLOW + ownerList);
		
		if(owners != region.getMembers())
		{
			String memberList = Util.makeNameList(region.getMembers().getPlayers(), player.getName(), "You");
			
			if(memberList.isEmpty())
				memberList = "Nobody";
			
			messages.add("  Members: " + ChatColor.YELLOW + memberList);
		}
		
		if(status != null)
		{
			if(status.Tenant.equals(player.getName()))
				messages.add("  Main Tenant: " + ChatColor.YELLOW + "You");
			else
				messages.add("  Main Tenant: " + ChatColor.YELLOW + status.Tenant);
			
			String rentStatus =    "  Status: ";
			
			if(status.PendingEviction)
				rentStatus += ChatColor.RED + "Pending Eviction";
			else if(status.PendingRemoval)
				rentStatus += ChatColor.GOLD + "Ending Rent";
			else
				rentStatus += ChatColor.GREEN + "Normal";
			
			messages.add(rentStatus);
			
			if(status.PendingEviction)
			{
				messages.add("  Evicted In: " + ChatColor.YELLOW + Util.formatTimeDifference(status.NextIntervalEnd - status.RentInterval - Calendar.getInstance().getTimeInMillis(),2, false));
				messages.add("  Next Payment: " + ChatColor.YELLOW + Util.formatCurrency(status.IntervalPayment));
			}
			else if(status.PendingRemoval)
			{
				messages.add("  Terminated In: " + ChatColor.YELLOW + Util.formatTimeDifference(status.NextIntervalEnd - Calendar.getInstance().getTimeInMillis(),2, false));
			}
			else
			{
				messages.add("  Next Payment In: " + ChatColor.YELLOW + Util.formatTimeDifference(status.NextIntervalEnd - Calendar.getInstance().getTimeInMillis(),2, false));
				messages.add("  Next Payment: " + ChatColor.YELLOW + Util.formatCurrency(status.IntervalPayment));
			}
				
			
			if(status.Date != 0)
				messages.add("  Renting Since: " + ChatColor.YELLOW + DateFormat.getDateTimeInstance().format(new Date(status.Date)));
		}
		
		String[] msgs = messages.toArray(new String[messages.size()]);
		player.sendMessage(msgs);
	}
	
	@EventHandler(priority= EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerInteract(PlayerInteractEvent event)
	{
		if(!event.hasBlock() || (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK))
			return;
		
		if(!event.getPlayer().hasPermission("regionsigns.use.manage"))
			// Does not have permission to use the sign
			return;
		
		if(event.getClickedBlock().getType() != Material.SIGN_POST && event.getClickedBlock().getType() != Material.WALL_SIGN)
			// Not a sign
			return;
		
		if(event.getPlayer().isConversing())
			// Possibly already in the menu
			return;
		
		// Stop block placing. Not cancelled on left click so you can break it
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
			event.setCancelled(true);
		
		Sign clickedBlock = (Sign)event.getClickedBlock().getState();

		if(!matchesSign(clickedBlock.getLines()))
			return;
		
		String regionName = getRegionName(clickedBlock.getLines());
		ProtectedRegion region = new Region(clickedBlock.getWorld(), regionName).getProtectedRegion();
		
		if(region == null)
			return;
		
		DefaultDomain applicableGroup;
		
		RentStatus status = RentManager.instance.getStatus(new Region(clickedBlock.getWorld(), regionName));
		
		if(status != null)
			applicableGroup = region.getMembers();
		else
			applicableGroup = region.getOwners();
		
		// See if this player can use the sign
		if(!applicableGroup.contains(event.getPlayer().getName()) && !event.getPlayer().hasPermission("regionsigns.use.manage.others"))
			return;
		
		String playerName = getPlayerName(clickedBlock.getLines());
		
		// Make sure the name on the sign matches one of the owners
		boolean ok = false;
		for(String name : applicableGroup.getPlayers())
		{
			if(name.toLowerCase().startsWith(playerName.toLowerCase()))
			{
				ok = true;
				break;
			}
		}
		
		if(!ok)
			return;
		
		// All checks done, now the sign can be used
		
		if(event.getAction() == Action.LEFT_CLICK_BLOCK)
			displayStatus(event.getPlayer(), region, applicableGroup, status);
		else
		{
			// Do conversation
			ManagementMenu menu = new ManagementMenu(region, clickedBlock.getLocation(), event.getPlayer(), status, false);
			menu.show();
		}
	}
}
