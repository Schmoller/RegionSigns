package au.com.mineauz.RegionSigns.sale;

import org.bukkit.Location;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.mineauz.RegionSigns.InteractableSignState;
import au.com.mineauz.RegionSigns.Util;

public class SaleSignState extends InteractableSignState
{
	private ProtectedRegion mRegion;
	private double mPrice;
	
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
		
		mPrice = Util.parseCurrency(signLines[3]);
		if(Double.isNaN(mPrice) || mPrice < 0)
			throw new Exception("Expected price or 'free' on line 4." + (mPrice < 0 ? " Negative prices are not allowed" : ""));
	}
	
	public ProtectedRegion getRegion()
	{
		return mRegion;
	}
	
	public double getPrice()
	{
		return mPrice;
	}
	
	@Override
	public String[] getValidSignText()
	{
		return new String[] {mRegion.getId(), "", Util.formatCurrency(mPrice)};
	}
}
