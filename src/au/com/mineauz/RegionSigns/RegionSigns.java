package au.com.mineauz.RegionSigns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.mineauz.RegionSigns.claim.ClaimSign;
import au.com.mineauz.RegionSigns.commands.*;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentSign;

import com.earth2me.essentials.Essentials;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
//import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionSigns extends JavaPlugin implements Listener 
{
	ClaimSign mClaimSigns;
	RentSign mRentSigns;
	
	private CommandDispatcher mCommandHandler;
	
	public HashMap<String,ClaimRestriction> ClaimRestrictions;
	public HashMap<String,ClaimRestriction> RentRestrictions;
	public HashMap<String,ClaimRestriction> AnyRestrictions;

	public static WorldGuardPlugin worldGuard;
	public static RegionSigns instance;
	
	
	
	public int getPlayerRegionCountIn(ProtectedRegion parent, Player player, boolean rented)
	{
		int count = 0;
		
		if(!rented)
		{
			RegionManager manager = worldGuard.getRegionManager(player.getWorld());
			if(manager == null)
				return 0;
			
			LocalPlayer p = worldGuard.wrapPlayer(player);

			// Count up the regions. dont include regions where they are members
			for(Map.Entry<String, ProtectedRegion> region : manager.getRegions().entrySet())
			{
				if(region.getValue().isOwner(p) && region.getValue().getParent() == parent)
					count++;
			}
		}
		else
		{
			count = RentManager.instance.getCountIn(parent, player);
		}
		
		return count;
	}
	public int getPlayerRegionCount(Player player, boolean rented)
	{
		int count = 0;
		
		if(!rented)
		{
			RegionManager manager = worldGuard.getRegionManager(player.getWorld());
			if(manager == null)
				return 0;
			
			LocalPlayer p = worldGuard.wrapPlayer(player);

			// Count up the regions. dont include regions where they are members
			for(Map.Entry<String, ProtectedRegion> region : manager.getRegions().entrySet())
			{
				if(region.getValue().isOwner(p))
					count++;
			}
		}
		else
		{
			count = RentManager.instance.getCount(player);
		}
		
		return count;
	}

	public int getClaimLimit()
	{
		if(getConfig().getInt("globalmax",5) == 0)
		{
			return getConfig().getInt("claim.overallmax",5);
		}
		else if(getConfig().getInt("claim.overallmax",5) == 0)
		{
			return getConfig().getInt("globalmax",5);
		}
		else
		{
			return Math.min(getConfig().getInt("globalmax",5), getConfig().getInt("claim.overallmax",5)); 
		}
	}
	
	public int getClaimChildLimit()
	{
		if(getConfig().getInt("globalchildmax",1) == 0)
		{
			return getConfig().getInt("claim.childmax",1);
		}
		else if(getConfig().getInt("claim.childmax",1) == 0)
		{
			return getConfig().getInt("globalchildmax",1);
		}
		else
		{
			return Math.min(getConfig().getInt("globalchildmax",1), getConfig().getInt("claim.childmax",1)); 
		}
	}
	
	private HashMap<String,ClaimRestriction> readRestrictions(String sectionName, boolean claim, boolean rent)
	{
		HashMap<String,ClaimRestriction> restrictions = new HashMap<String,ClaimRestriction>();

		if(getConfig().isConfigurationSection(sectionName))
		{
			ConfigurationSection section = getConfig().getConfigurationSection(sectionName);
			
			for(Map.Entry<String,Object> value : section.getValues(false).entrySet())
			{
				// Check for def
				if(section.isConfigurationSection(value.getKey()))
				{
					// Found a region def. get the world and region name
					World world;
					String regionName;
					if(value.getKey().contains("-"))
					{
						world = getServer().getWorld(value.getKey().split("(.)*-(.)*")[0]);
						regionName = value.getKey().split("(.)*-(.)*")[1];
					}
					else
					{
						world = getServer().getWorlds().get(0);
						regionName = value.getKey();
					}
					
					if(world == null)
					{
						getLogger().warning("Unable to create restriction for " + value.getKey() + " the world doent exist");
						continue;
					}
					
					getLogger().info("Claim restriction in place for '" + regionName + "' in " + world.getName());
					
					// Create the permission
					String permType = (claim && !rent ? "claim" : (rent & !claim ? "rent" : "any"));
					
					Permission perm = new Permission("regionsigns." + permType + "." + regionName);
					if(claim && rent)
						perm.setDescription("Allows a player to aquire a child region of " + regionName);
					else
						perm.setDescription("Allows a player to " + permType + " a child region of " + regionName);
					
					
					String defaultVal = ((ConfigurationSection)value.getValue()).getString("default","false");
					String message = ((ConfigurationSection)value.getValue()).getString("message","You do not have permission to claim this region");
					
					if(defaultVal.compareToIgnoreCase("true") == 0)
						perm.setDefault(PermissionDefault.TRUE);
					else if(defaultVal.compareToIgnoreCase("false") == 0)
						perm.setDefault(PermissionDefault.FALSE);
					else if(defaultVal.compareToIgnoreCase("op") == 0)
						perm.setDefault(PermissionDefault.OP);
					else if(defaultVal.compareToIgnoreCase("notop") == 0)
						perm.setDefault(PermissionDefault.NOT_OP);
					else
					{
						getLogger().warning("Invalid default value for " + ((ConfigurationSection)value.getValue()).getCurrentPath() + ". Setting to false");
						perm.setDefault(PermissionDefault.FALSE);
					}
					getServer().getPluginManager().addPermission(perm);
					
					// Create the restriction
					ClaimRestriction cr = new ClaimRestriction();
					cr.ClaimPermission = perm;
					cr.Message = message;
					
					restrictions.put(world.getName() + "-" + regionName, cr);
				}
			}
		}
		return restrictions;
	}
	public void onEnable()
	{
		instance = this;
		// Copy the config if the defaults dont exist
		File f = new File(getDataFolder(),"config.yml");
		if(!f.exists())
		{
			try
			{
				new File(f.getParent()).mkdirs();
				
				InputStream resource = getResource("config.yml");
				
				f.createNewFile();
				FileOutputStream stream = new FileOutputStream(f);
				byte[] temp = new byte[2000];
				int readCount = 0;
				do
				{
					// Read a section
					readCount = resource.read(temp);

					if(readCount != -1)
						// Now save it
						stream.write(temp,0,readCount);
					
				} while(readCount > 0);
				
				stream.close();
				resource.close();
			}
			catch(IOException e)
			{
				getLogger().severe("Failed to create config file:\n" + e.getMessage());
			}
		}

		reloadConfig();

		// Check to see if the plugins needed are present
		if(getServer().getPluginManager().getPlugin("WorldGuard") == null || !(getServer().getPluginManager().getPlugin("WorldGuard") instanceof WorldGuardPlugin))
		{
			// WorldGuard is not present
			getLogger().severe("WorldGuard is not present. Cannot start " + getName());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		worldGuard = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
		
		if(getServer().getPluginManager().getPlugin("Essentials") == null || !(getServer().getPluginManager().getPlugin("Essentials") instanceof Essentials))
		{
			// Essentials is not present
			getLogger().severe("Essentials is not present. Cannot start " + getName());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		Util.sCurrencyChar = getServer().getPluginManager().getPlugin("Essentials").getConfig().getString("currency-symbol");
		
		initializeConfig();
		
		RentManager.instance = new RentManager();
		RentManager.instance.start();
		
		mCommandHandler = new CommandDispatcher("rent", "Region Renting (part of " + getDescription().getName() + " " + getDescription().getVersion() + ") by Schmoller");
		mCommandHandler.registerCommand(new InfoCommand());
		mCommandHandler.registerCommand(new StopCommand());
		mCommandHandler.registerCommand(new ListCommand());
		mCommandHandler.registerCommand(new ForceStopCommand());
		mCommandHandler.registerCommand(new SetCommand());
		mCommandHandler.registerCommand(new TransferCommand());
		mCommandHandler.registerCommand(new ReloadCommand());
		
		getCommand("rent").setExecutor(mCommandHandler);
		getCommand("rent").setTabCompleter(mCommandHandler);
		
		//mRentSystem = new RentSystem(this,mStoredSigns, storedSignsFile);
		mClaimSigns = new ClaimSign(this);
		mRentSigns = new RentSign(this);
	}
	public void onDisable()
	{
		RentManager.instance.stop();
	}
	
	public void initializeConfig()
	{
		// Clear existing perms
		if(ClaimRestrictions != null)
		{
			for(ClaimRestriction r : ClaimRestrictions.values())
				getServer().getPluginManager().removePermission(r.ClaimPermission);
		}
		
		if(RentRestrictions != null)
		{
			for(ClaimRestriction r : RentRestrictions.values())
				getServer().getPluginManager().removePermission(r.ClaimPermission);
		}
		
		if(AnyRestrictions != null)
		{
			for(ClaimRestriction r : AnyRestrictions.values())
				getServer().getPluginManager().removePermission(r.ClaimPermission);
		}
		
		// Make the restrictions
		ClaimRestrictions = readRestrictions("claim.restrictions", true, false);
		RentRestrictions = readRestrictions("rent.restrictions", false, true);
		AnyRestrictions = readRestrictions("restrictions", true, true);
		
		try {
			RentManager.MinimumRentPeriod = Util.parseDateDiff(getConfig().getString("rent.minperiod", "2w"));
		} catch (Exception e) {
			getLogger().severe("Invalid value in rent.minperiod");
			e.printStackTrace();
		}
	}
}
