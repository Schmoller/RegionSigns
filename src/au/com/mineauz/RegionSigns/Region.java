package au.com.mineauz.RegionSigns;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Region
{
	private World mWorld;
	private String mID;
	
	private Region() {}
	
	public Region(World world, String ID)
	{
		mWorld = world;
		mID = ID;
	}
	
	public World getWorld()
	{
		return mWorld;
	}
	
	public String getID()
	{
		return mID;
	}
	
	public boolean isSuperGlobal()
	{
		return mWorld == null && mID.equalsIgnoreCase("__global__");
	}
	
	public boolean isGlobal()
	{
		return mID.equalsIgnoreCase("__global__");
	}
	public ProtectedRegion getProtectedRegion()
	{
		RegionManager manager = RegionSigns.worldGuard.getRegionManager(mWorld);
		
		if(manager == null)
			return null;

		return manager.getRegionExact(mID);
	}
	
	public static Region parse(String worldStr, String regionStr, CommandSender sender)
	{
		World world = null;
		
		if(worldStr != null)
			world = Bukkit.getWorld(worldStr);
		else
		{
			if(sender instanceof BlockCommandSender)
				world = ((BlockCommandSender)sender).getBlock().getWorld();
			if(sender instanceof Player)
				world = ((Player)sender).getWorld();
		}
		
		// Need to have a world by this part
		if(world == null)
			return null;
		
		Region region = new Region();
		region.mWorld = world;
		region.mID = regionStr;
		
		// Check if there is a region
		if(region.getProtectedRegion() == null)
			return null;
		
		return region;
	}
	
	public static Region parseRegion(String formatted)
	{
		String worldPart = "";
		String idPart = "";
		
		// Extract the parts
		worldPart = formatted.replaceAll("^(?:((?:'[\\w\\-]+')|\\w+)\\-)?((?:'[\\w\\-]+')|\\w+)$", "$1");
		idPart = formatted.replaceAll("^(?:((?:'[\\w\\-]+')|\\w+)\\-)?((?:'[\\w\\-]+')|\\w+)$", "$2");
		
		worldPart = worldPart.trim();
		worldPart = worldPart.replaceAll("'", "");
		
		idPart = idPart.trim();
		idPart = idPart.replaceAll("'", "");
		
		Region region = new Region();
		
		region.mID = idPart;
		
		if(!worldPart.isEmpty())
			region.mWorld = Bukkit.getWorld(worldPart);
		
		return region;
	}
	
	public String formatRegion()
	{
		String result = "";
		
		if(mWorld != null)
		{
			if(mWorld.getName().contains("-"))
				result += String.format("'%s'", mWorld.getName());
			else
				result += mWorld.getName();
			
			result += "-";
		}
		
		if(mID.contains("-"))
			result += String.format("'%s'", mID);
		else
			result += mID;
		
		return result;
	}
	
	@Override
	public int hashCode()
	{
		if(mWorld == null)
			return mID.hashCode() << 5;
		
		return mWorld.hashCode() | (mID.hashCode() << 5);
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof Region))
			return false;
		
		if(mWorld != null)
			return ((Region)obj).mWorld.equals(mWorld) && ((Region)obj).mID.equals(mID);
		else
			return ((Region)obj).mID.equals(mID);
	}
}
