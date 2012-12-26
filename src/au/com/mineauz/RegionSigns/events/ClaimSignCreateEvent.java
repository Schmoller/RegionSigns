package au.com.mineauz.RegionSigns.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ClaimSignCreateEvent extends RegionSignCreateEvent
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
	
	
	private double mPrice;
	
	public ClaimSignCreateEvent(ProtectedRegion region, Location signLocation, double price)
	{
		super(region, signLocation);
		mPrice = price;
	}
	
	/**
	 * Gets the price to claim the region
	 */
	public double getPrice()
	{
		return mPrice;
	}
}
