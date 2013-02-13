package au.com.mineauz.RegionSigns;

import java.io.File;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;


public class LegacyConfigUpgrader
{
	public static void upgrade()
	{
		/// Extract the values
		
		// Claim first
		RegionSigns.config.maxClaimPrice = RegionSigns.instance.getConfig().getDouble("claim.max-price",0);
		
		RegionSigns.config.claimSign = new String[] {
				RegionSigns.instance.getConfig().getString("claim.sign.line1","<user>"),
				RegionSigns.instance.getConfig().getString("claim.sign.line2",""),
				RegionSigns.instance.getConfig().getString("claim.sign.line3","<region>"),
				RegionSigns.instance.getConfig().getString("claim.sign.line4","")
		};
		
		// Rent
		RegionSigns.config.maxRentIntervalPayment = RegionSigns.instance.getConfig().getDouble("rent.max-price-interval",0);
		RegionSigns.config.maxRentUpfrontPayment = RegionSigns.instance.getConfig().getDouble("rent.max-price-upfront",0);
		
		RegionSigns.config.checkInterval = RegionSigns.instance.getConfig().getInt("rent.check-interval",1200);
		RegionSigns.config.autosaveInterval = RegionSigns.instance.getConfig().getInt("rent.autosave-interval",6000);
		
		RegionSigns.config.defaultRentPeriod = Util.parseDateDiff(RegionSigns.instance.getConfig().getString("rent.defaultperiod","1w"));
		RegionSigns.config.minimumRentPeriod = Util.parseDateDiff(RegionSigns.instance.getConfig().getString("rent.minperiod","2w"));
		
		RegionSigns.config.rentSign = new String[] {
				RegionSigns.instance.getConfig().getString("rent.sign.line1","<user>"),
				RegionSigns.instance.getConfig().getString("rent.sign.line2",""),
				RegionSigns.instance.getConfig().getString("rent.sign.line3","<region>"),
				RegionSigns.instance.getConfig().getString("rent.sign.line4","")
		};
		
		// Messages
		RegionSigns.config.messagePrefix = RegionSigns.instance.getConfig().getString("rent.messages.prefix","&a[Rent]&f ");
		
		RegionSigns.config.messageBeginRenting = RegionSigns.instance.getConfig().getString("rent.messages.begin","You have started renting &e'<region>'&f. &c<payment>&f was spent.");
		
		RegionSigns.config.messageBeginRentingFree = RegionSigns.instance.getConfig().getString("rent.messages.begin-free","You have started renting &e'<region>'&f.");
		
		RegionSigns.config.messageEvicted = RegionSigns.instance.getConfig().getString("rent.messages.evicted","&cYou have been evicted from &e'<region>'.&c Please contact an Admin or Mod to retrieve any remaining possessions.");
		
		RegionSigns.config.messageFinished = RegionSigns.instance.getConfig().getString("rent.messages.finished","You have finished renting &e'<region>'&f. Please contact an Admin or Mod to retrieve any remaining possessions.");
		
		RegionSigns.config.messageFirstPayment = RegionSigns.instance.getConfig().getString("rent.messages.first-payment","The first payment of &c<payment>&f will be deducted in &e<time>&f.");
		
		RegionSigns.config.messageNextPayment = RegionSigns.instance.getConfig().getString("rent.messages.next-payment","The next payment will be deducted in &e<time>&f.");
		
		RegionSigns.config.messagePayment = RegionSigns.instance.getConfig().getString("rent.messages.payment","&c<payment>&f has been paid for &e'<region>'&f.");
		
		RegionSigns.config.messageTerminate = RegionSigns.instance.getConfig().getString("rent.messages.terminate","You have stopped renting &e'<region>'&f. A final payment of &c<payment>&f was sent. You have &e<time>&f to move out.");
		
		RegionSigns.config.messageWarningEvict = RegionSigns.instance.getConfig().getString("rent.messages.warning-evict","&6Warning: You will be evicted from &e'<region>'&6 if payment is not received in &e<time>&6.");
		
		RegionSigns.config.messageWarningFunds = RegionSigns.instance.getConfig().getString("rent.messages.warning-funds","&6Warning: You currently have insufficient funds to pay your next rent");
		
		// Extract the restrictions
		readRestrictions("claim.restrictions", true, false);
		readRestrictions("rent.restrictions", false, true);
		readRestrictions("restrictions", true, true);
		
		RegionSigns.restrictionManager.saveRestrictions();
		if(RegionSigns.config.save())
			new File(RegionSigns.instance.getDataFolder(), "config.yml").delete();
	}
	
	private static void readRestrictions(String sectionName, boolean claim, boolean rent)
	{
		if(RegionSigns.instance.getConfig().isConfigurationSection(sectionName))
		{
			ConfigurationSection section = RegionSigns.instance.getConfig().getConfigurationSection(sectionName);
			
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
						world = Bukkit.getWorld(value.getKey().split("(.)*-(.)*")[0]);
						regionName = value.getKey().split("(.)*-(.)*")[1];
					}
					else
					{
						world = Bukkit.getWorlds().get(0);
						regionName = value.getKey();
					}
					
					if(world == null)
					{
						RegionSigns.instance.getLogger().severe("Unable to create restriction for " + value.getKey() + " the world doent exist");
						continue;
					}
					
					// Create the permission
					String permType = (claim && !rent ? "claim" : (rent & !claim ? "rent" : "any"));
					
					Permission perm = new Permission("regionsigns." + permType + "." + regionName);
					if(claim && rent)
						perm.setDescription("Allows a player to aquire a child region of " + regionName);
					else
						perm.setDescription("Allows a player to " + permType + " a child region of " + regionName);
					
					
					String defaultVal = ((ConfigurationSection)value.getValue()).getString("default","false");
					String message = ((ConfigurationSection)value.getValue()).getString("message","You do not have permission to claim this region");
					PermissionDefault def = PermissionDefault.getByName(defaultVal);
					
					if(def == null)
					{
						RegionSigns.instance.getLogger().warning("Invalid default value for " + ((ConfigurationSection)value.getValue()).getCurrentPath() + ". Setting to false");
						def = PermissionDefault.FALSE;
					}
					
					if(!RegionSigns.restrictionManager.addRestriction(new Region(world, regionName), (rent && claim ? RestrictionType.All : (rent ? RestrictionType.Rent : RestrictionType.Claim)), message, def, -1))
					{
						RegionSigns.instance.getLogger().severe("Unable to create restriction for " + value.getKey() + ". The definition is invalid.");
						continue;
					}
				}
			}
		}
	}
}
