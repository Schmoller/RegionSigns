package au.com.mineauz.RegionSigns.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RentSignCreateEvent extends RegionSignCreateEvent
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
	
	private double mInitialPrice;
	private double mIntervalPrice;
	private long mIntervalLength;
	
	public RentSignCreateEvent(ProtectedRegion region, Location signLocation, double initialPrice, double intervalPrice, long intervalLength)
	{
		super(region, signLocation);
		
		mInitialPrice = initialPrice;
		
		mIntervalPrice = intervalPrice;
		mIntervalLength = intervalLength;
	}

	/**
	 * Gets the price to be payed initially
	 */
	public double getInitialPrice()
	{
		return mInitialPrice;
	}
	
	/**
	 * Gets the price to be payed at each interval
	 */
	public double getIntervalPrice()
	{
		return mIntervalPrice;
	}
	
	/**
	 * Gets the interval length (time between payments) in milliseconds
	 */
	public long getIntervalLength()
	{
		return mIntervalLength;
	}
	
}
