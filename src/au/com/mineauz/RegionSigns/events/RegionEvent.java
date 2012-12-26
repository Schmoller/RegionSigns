package au.com.mineauz.RegionSigns.events;

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
	
	public RegionEvent(ProtectedRegion region)
	{
		mRegion = region;
	}
	
	/**
	 * Gets the region this sign is for
	 */
	public ProtectedRegion getRegion()
	{
		return mRegion;
	}
}
