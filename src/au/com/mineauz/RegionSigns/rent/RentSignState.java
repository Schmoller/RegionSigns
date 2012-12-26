package au.com.mineauz.RegionSigns.rent;

import org.bukkit.Location;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.mineauz.RegionSigns.InteractableSignState;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;

public class RentSignState extends InteractableSignState
{
	private ProtectedRegion mRegion;
	private double mInitialPrice;
	private double mIntervalPrice;
	private long mIntervalLength;
	
	@Override
	public void load( Location location, String[] signLines ) throws Exception
	{
		super.load(location, signLines);
		
		String regionName = signLines[1];
		
		mRegion = Util.getRegion(location.getWorld(), regionName);
		if(mRegion == null)
		{
			if(signLines[1].isEmpty())
				throw new Exception("Expected region name on line 2.");
			else
				throw new Exception("The region '" + regionName + "' does not exist.");
		}
		
		mInitialPrice = Util.parseCurrency(signLines[2]);
		if(Double.isNaN(mInitialPrice) || mInitialPrice < 0)
			throw new Exception("Expected price or 'free' on line 3." + (mInitialPrice < 0 ? " Negative prices are not allowed" : ""));
		
		String intervalPriceString = "";
		String intervalLengthString = "";
		
		if(signLines[3].contains(":"))
		{
			intervalPriceString = signLines[3].split(":")[0];
			intervalLengthString = signLines[3].split(":")[1];
		}
		else
		{
			intervalPriceString = signLines[3];
			intervalLengthString = RegionSigns.instance.getConfig().getString("rent.defaultperiod");
		}
		
		mIntervalPrice = Util.parseCurrency(intervalPriceString);
		if(Double.isNaN(mIntervalPrice) || mIntervalPrice < 0)
			throw new Exception("Expected either price or price:timediff on line 4." + (mIntervalPrice < 0 ? " Negative prices are not allowed" : ""));
		
		mIntervalLength = Util.parseDateDiff(intervalLengthString);
		
		if(mIntervalLength <= 0)
			throw new Exception("Expected either price or price:timediff on line 4. Format of time diff is like '1w 3d 2h'.");
	}
	
	public ProtectedRegion getRegion()
	{
		return mRegion;
	}
	
	public double getInitialPrice()
	{
		return mInitialPrice;
	}
	
	public double getIntervalPrice()
	{
		return mIntervalPrice;
	}
	
	public long getIntervalLength()
	{
		return mIntervalLength;
	}
	
	@Override
	public String[] getValidSignText()
	{
		boolean defaultPeriod = mIntervalLength == Util.parseDateDiff(RegionSigns.instance.getConfig().getString("rent.defaultperiod"));
		
		return new String[] {mRegion.getId(), (mInitialPrice == 0.0 && defaultPeriod && mIntervalPrice == 0.0) ? "" : Util.formatCurrency(mInitialPrice), (defaultPeriod ? Util.formatCurrency(mIntervalPrice) : Util.formatCurrency(mIntervalPrice) + ":" + Util.formatDateDiffShort(mIntervalLength))};
	}
}
