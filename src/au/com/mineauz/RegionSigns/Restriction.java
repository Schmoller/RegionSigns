package au.com.mineauz.RegionSigns;

import org.bukkit.permissions.Permission;

public class Restriction 
{
	public RestrictionType type = RestrictionType.All;
	public Region region = null;
	public Permission permission = null;
	public String message = "You dont have permission to aquire this region!";
	public int maxCount = -1;
}
