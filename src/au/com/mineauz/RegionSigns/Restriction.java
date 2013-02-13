package au.com.mineauz.RegionSigns;

import org.bukkit.permissions.Permission;

public class Restriction 
{
	public RestrictionType type;
	public Region region;
	public Permission permission;
	public String message;
	public int maxCount;
}
