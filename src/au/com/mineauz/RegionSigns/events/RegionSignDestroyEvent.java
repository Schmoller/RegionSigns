package au.com.mineauz.RegionSigns.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionSignDestroyEvent extends RegionEvent
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

	public RegionSignDestroyEvent( ProtectedRegion region, Location signLocation )
	{
		super(region, signLocation);
	}

}
