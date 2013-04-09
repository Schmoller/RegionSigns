package au.com.mineauz.RegionSigns;

import java.io.File;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.mineauz.RegionSigns.claim.ClaimSign;
import au.com.mineauz.RegionSigns.commands.*;
import au.com.mineauz.RegionSigns.manage.ManagementSign;
import au.com.mineauz.RegionSigns.rent.RentManager;
import au.com.mineauz.RegionSigns.rent.RentSign;
import au.com.mineauz.RegionSigns.sale.SaleSign;

import com.earth2me.essentials.Essentials;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionSigns extends JavaPlugin implements Listener 
{
	ClaimSign mClaimSigns;
	RentSign mRentSigns;
	SaleSign mSaleSigns;
	ManagementSign mManagementSigns;
	
	private CommandDispatcher mCommandHandler;
	
	public static WorldGuardPlugin worldGuard;
	public static RegionSigns instance;
	
	public final static RestrictionManager restrictionManager = new RestrictionManager();
	public static Config config;
	
	public int getPlayerRegionCountIn(Region region, Player player, RestrictionType type)
	{
		int count = 0;
		
		if(type != RestrictionType.Rent)
		{
			if(region.isSuperGlobal())
			{
				for(World world : Bukkit.getWorlds())
				{
					RegionManager manager = worldGuard.getRegionManager(world);
					if(manager == null)
						continue;
					
					LocalPlayer p = worldGuard.wrapPlayer(player);
		
					// Count up the regions. dont include regions where they are members
					for(Map.Entry<String, ProtectedRegion> protectedRegion : manager.getRegions().entrySet())
					{
						if(!protectedRegion.getValue().isOwner(p))
							continue;
						
						if(region.isGlobal())
							++count;
						else
						{
							ProtectedRegion parent = protectedRegion.getValue();
							
							while (parent != null)
							{
								if(parent.equals(region.getProtectedRegion()))
								{
									++count;
									break;
								}
								parent = parent.getParent();
							}
						}
					}
				}
			}
			else
			{
				RegionManager manager = worldGuard.getRegionManager(region.getWorld());
				if(manager == null)
					return 0;
				
				LocalPlayer p = worldGuard.wrapPlayer(player);
	
				// Count up the regions. dont include regions where they are members
				for(Map.Entry<String, ProtectedRegion> protectedRegion : manager.getRegions().entrySet())
				{
					if(!protectedRegion.getValue().isOwner(p))
						continue;
					
					if(region.isGlobal())
						++count;
					else
					{
						ProtectedRegion parent = protectedRegion.getValue();
						
						while (parent != null)
						{
							if(parent.equals(region.getProtectedRegion()))
							{
								++count;
								break;
							}
							parent = parent.getParent();
						}
					}
				}
			}
		}
		
		if(type != RestrictionType.Claim)
			count += RentManager.instance.getCountIn(region, player);
		
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

	public void onEnable()
	{
		instance = this;

		// Check to see if the plugins needed are present
		if(getServer().getPluginManager().getPlugin("WorldGuard") == null || !(getServer().getPluginManager().getPlugin("WorldGuard") instanceof WorldGuardPlugin))
		{
			// WorldGuard is not present
			throw new RuntimeException("WorldGuard not detected. It is required for RegionSigns to function.");
		}
		
		worldGuard = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
		
		if(getServer().getPluginManager().getPlugin("Essentials") == null || !(getServer().getPluginManager().getPlugin("Essentials") instanceof Essentials))
		{
			// Essentials is not present
			throw new RuntimeException("Essentials not detected. It is required for RegionSigns to function.");
		}
		
		Util.sCurrencyChar = getServer().getPluginManager().getPlugin("Essentials").getConfig().getString("currency-symbol");
		
		
		config = new Config(new File(getDataFolder(), "settings.yml"));

		// Check for old config
		File oldConfig = new File(getDataFolder(), "config.yml");
		if(oldConfig.exists())
		{
			LegacyConfigUpgrader.upgrade();
		}
		else
		{
			config.load();
			config.save();
			
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

				@Override
				public void run()
				{
					restrictionManager.loadRestrictions();
				}
				
			});
			
		}
		
		
		
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
		mCommandHandler.registerCommand(new RestrictionCommand());
		
		getCommand("rent").setExecutor(mCommandHandler);
		getCommand("rent").setTabCompleter(mCommandHandler);
		
		//mRentSystem = new RentSystem(this,mStoredSigns, storedSignsFile);
		mClaimSigns = new ClaimSign(this);
		mRentSigns = new RentSign(this);
		mSaleSigns = new SaleSign(this);
		mManagementSigns = new ManagementSign();
	}
	public void onDisable()
	{
		RentManager.instance.stop();
	}

}
