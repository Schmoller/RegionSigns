package au.com.mineauz.RegionSigns.manage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.mineauz.RegionSigns.Region;
import au.com.mineauz.RegionSigns.RegionSigns;

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
		
		Sign clickedBlock = (Sign)event.getClickedBlock().getState();

		if(!matchesSign(clickedBlock.getLines()))
			return;
		
		String regionName = getRegionName(clickedBlock.getLines());
		ProtectedRegion region = new Region(clickedBlock.getWorld(), regionName).getProtectedRegion();
		
		if(region == null)
			return;
		
		// See if this player can use the sign
		if(!region.isOwner(event.getPlayer().getName()) && !event.getPlayer().hasPermission("regionsigns.use.manage.others"))
			return;
		
		String playerName = getPlayerName(clickedBlock.getLines());
		
		// Make sure the name on the sign matches one of the owners
		boolean ok = false;
		for(String name : region.getOwners().getPlayers())
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
		
		event.getPlayer().sendMessage("Checks passed");
		
		event.setCancelled(true);
	}
}
