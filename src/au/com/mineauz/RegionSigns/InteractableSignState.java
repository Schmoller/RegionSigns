package au.com.mineauz.RegionSigns;

import org.bukkit.Location;

public abstract class InteractableSignState 
{
	private Location mSignLocation;
	
	public void load(Location location, String[] signLines) throws Exception
	{
		mSignLocation = location;
	}
	
	public Location getLocation()
	{
		return mSignLocation;
	}

	public abstract String[] getValidSignText();
	
}
