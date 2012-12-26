package com.gmail.steven.schmoll.RegionSigns;

public class RentStatus 
{
	// The name of the player renting the region
	public String Tenant;
	// The id of the region
	public String Region;
	// The world that the region is in
	public String World;
	// The time in ms between rent collection
	public long RentInterval;
	// the time in ms that the current interval will end
	public long NextIntervalEnd;
	// the amount of currency that will be payed the next interval
	public double IntervalPayment;
	// When true, the tenant will be evicted if they fail to pay before the next half interval( NextIntervalEnd - RentInterval / 2)
	public boolean PendingEviction;
	// When true, the tenant will be removed from the region at the end of the current interval
	public boolean PendingRemoval;
	// The date when the renting was started
	public long Date;
}
