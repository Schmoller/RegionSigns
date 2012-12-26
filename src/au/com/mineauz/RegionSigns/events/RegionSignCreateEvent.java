package au.com.mineauz.RegionSigns.events;

import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionSignCreateEvent extends RegionEvent 
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
	
	public RegionSignCreateEvent(ProtectedRegion region)
	{
		super(region);
	}
}
