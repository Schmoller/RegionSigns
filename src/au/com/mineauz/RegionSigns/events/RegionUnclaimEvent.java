package au.com.mineauz.RegionSigns.events;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionUnclaimEvent extends RegionEvent
{
	private static final HandlerList mHandlers = new HandlerList(); 
	
	@Override
	public HandlerList getHandlers() 
	{
		return mHandlers;
	}

	public static HandlerList getHandlerList()
	{
		return mHandlers;
	}
	
	// Event specific stuff:
	
	private OfflinePlayer mPlayer;
	
	public RegionUnclaimEvent(ProtectedRegion region, Location signLocation, OfflinePlayer player)
	{
		super(region, signLocation);
		mPlayer = player;
	}

	public OfflinePlayer getPlayer()
	{
		return mPlayer;
	}
}
