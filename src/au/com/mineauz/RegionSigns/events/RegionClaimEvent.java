package au.com.mineauz.RegionSigns.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionClaimEvent extends RegionEvent implements Cancellable
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
	
	private boolean mIsCancelled = false;
	
	private double mAmount;
	private Player mClaimer;
	private String[] mSignText;
	
	public RegionClaimEvent(ProtectedRegion region, Player claimer, double amount, String[] signText)
	{
		super(region);
		mClaimer = claimer;
		mSignText = signText;
		mAmount = amount;
	}

	public Player getPlayer()
	{
		return mClaimer;
	}
	
	public double getPayment()
	{
		return mAmount;
	}
	
	public void setPayment(double amount)
	{
		if(amount < 0)
			throw new IllegalArgumentException("Cannot have a negative payment");
		
		mAmount = amount;
	}
	/**
	 * Gets the lines of the sign as it will turn into
	 */
	public String[] getSignLines()
	{
		return mSignText;
	}
	
	/**
	 * Sets a line on the resultant sign
	 * @param line The line number: 0-3
	 * @param text The text to set: max 16 chars
	 */
	public void setSignLine(int line, String text)
	{
		mSignText[line] = text;
	}
	
	
	@Override
	public boolean isCancelled()
	{
		return mIsCancelled;
	}

	@Override
	public void setCancelled( boolean cancelled )
	{
		mIsCancelled = cancelled; 
	}
	
	
}
