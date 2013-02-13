package au.com.mineauz.RegionSigns;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class RestrictionManager
{
	private HashMap<Region, Restriction> mRestrictions = new HashMap<Region, Restriction>();
	
	public boolean addRestriction(Restriction restriction)
	{
		if(mRestrictions.containsKey(restriction.region))
			return false;
		
		mRestrictions.put(restriction.region, restriction);
		
		return true;
	}
	
	public boolean addRestriction(Region region, RestrictionType type, String message, PermissionDefault def, int ownLimit)
	{
		if(mRestrictions.containsKey(region))
			return false;
		
		Restriction restriction = new Restriction();
		
		restriction.type = type;
		restriction.message = message;
		restriction.maxCount = ownLimit;
		restriction.permission = new Permission("regionsigns.restriction." + region.getWorld().getName() + "-" + region.getID());
		restriction.permission.setDefault(def);
		restriction.permission.setDescription("Allows a player to aquire a child region of " + region.getID() + " in " + region.getWorld().getName());

		Bukkit.getPluginManager().addPermission(restriction.permission);
		
		mRestrictions.put(region, restriction);
		
		return true;
	}
	
	public boolean setRestrictionMessage(Region region, String message)
	{
		if(!mRestrictions.containsKey(region))
			return false;
		
		mRestrictions.get(region).message = message;
		
		return true;
	}
	
	public boolean setRestrictionDefault(Region region, PermissionDefault def)
	{
		if(!mRestrictions.containsKey(region))
			return false;
		
		mRestrictions.get(region).permission.setDefault(def);
		Bukkit.getPluginManager().recalculatePermissionDefaults(mRestrictions.get(region).permission);
		
		return true;
	}
	
	public boolean setRestrictionOwnLimit(Region region, int limit)
	{
		if(!mRestrictions.containsKey(region))
			return false;
		
		mRestrictions.get(region).maxCount = limit;
		
		return true;
	}
	
	public boolean removeRestriction(Region region)
	{
		return mRestrictions.remove(region) != null;
	}
	
	public Restriction getRestriction(Region region)
	{
		return mRestrictions.get(region);
	}
	
	public void saveRestrictions()
	{
		FileConfiguration config = new YamlConfiguration();
		
		for(Restriction restriction : mRestrictions.values())
		{
			ConfigurationSection section = config.createSection(restriction.region.getWorld().getName() + "-" + restriction.region.getID());
			section.set("type", restriction.type.toString().toLowerCase());
			section.set("message", restriction.message);
			switch(restriction.permission.getDefault())
			{
			case FALSE:
				section.set("default", "none");
				break;
			case NOT_OP:
				section.set("default", "notop");
				break;
			case OP:
				section.set("default", "op");
				break;
			case TRUE:
				section.set("default", "all");
				break;
			}
			
			if(restriction.maxCount > 0)
				section.set("maxOwned", restriction.maxCount);
		}
		
		File file = new File(RegionSigns.instance.getDataFolder(), "restrictions.yml");
		
		try
		{
			config.save(file);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void loadRestrictions()
	{
		FileConfiguration config = new YamlConfiguration();
		File file = new File(RegionSigns.instance.getDataFolder(), "restrictions.yml");
		
		try
		{
			config.load(file);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return;
		}
		catch ( InvalidConfigurationException e )
		{
			e.printStackTrace();
			return;
		}
		
		// Remove the perms
		for(Restriction restriction : mRestrictions.values())
			Bukkit.getPluginManager().removePermission(restriction.permission);
		
		mRestrictions.clear();
		
		// Parse the file
		for(String key : config.getKeys(false))
		{
			if(!config.isConfigurationSection(key) || !key.contains("-"))
			{
				RegionSigns.instance.getLogger().severe("Error reading restrictions: Restriction " + key + " is invalid. Skipping");
				continue;
			}
			
			Region region = Region.parse(key.split("-")[0], key.split("-")[1], null);
			
			if(region == null)
			{
				RegionSigns.instance.getLogger().severe("Error reading restrictions: Restriction " + key + " represents a non-existent region. Skipping");
			}
			
			ConfigurationSection section = config.getConfigurationSection(key);
			Restriction restriction = new Restriction();
			
			restriction.region = region;
			
			restriction.maxCount = section.getInt("maxOwned",-1);
			restriction.message = section.getString("message", "You do not have permission to aquire this region!");
			
			// Parse the type
			String typeString = section.getString("type", "all");
			RestrictionType type = RestrictionType.valueOf(typeString.toUpperCase());
			if(type == null)
			{
				RegionSigns.instance.getLogger().severe("Error reading restrictions: Restriction " + key + " has an invalid type of " + typeString + ". Valid types are: all, claim, rent. Skipping");
				continue;
			}
			
			restriction.type = type;
			
			// Create the permission
			Permission perm = new Permission(key);
			
			// Parse the default
			PermissionDefault def;
			String defString = section.getString("default", "none");
			
			if(defString.equalsIgnoreCase("none"))
				def = PermissionDefault.FALSE;
			else if(defString.equalsIgnoreCase("all"))
				def = PermissionDefault.TRUE;
			else
				def = PermissionDefault.getByName(defString);
			
			if(def == null)
			{
				RegionSigns.instance.getLogger().severe("Error reading restrictions: Restriction " + key + " has an invalid default permission of " + defString + ". Valid options are: none, all, op, notop. Skipping");
				continue;
			}
			
			perm.setDefault(def);
			perm.setDescription("Allows a player to aquire a child region of " + region.getID() + " in " + region.getWorld().getName());
			
			restriction.permission = perm;
			Bukkit.getPluginManager().addPermission(perm);
			
			addRestriction(restriction);
		}
	}
	public Collection<Restriction> getRestrictions()
	{
		return Collections.unmodifiableCollection(mRestrictions.values());
	}
	
}
