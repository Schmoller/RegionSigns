package au.com.mineauz.RegionSigns.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public abstract class RegionEvent extends Event
{

	@Override
	public HandlerList getHandlers()
	{
		return null;
	}

	private ProtectedRegion mRegion;
	private Location mLocation;
	
	public RegionEvent(ProtectedRegion region, Location signLocation)
	{
		mRegion = region;
		mLocation = signLocation;
	}
	
	/**
	 * Gets the region this sign is for
	 */
	public ProtectedRegion getRegion()
	{
		return mRegion;
	}
	
	/**
	 * Gets the location of the sign
	 */
	public Location getLocation()
	{
		return mLocation;
	}
}
