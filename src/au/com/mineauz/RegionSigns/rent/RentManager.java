package au.com.mineauz.RegionSigns.rent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldSaveEvent;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.mineauz.RegionSigns.Region;
import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;

public class RentManager implements Listener
{
	public static RentManager instance;
	
	private int mTaskId;
	private int mAutosaveTaskId;
	
	private TreeMap<Long, RentStatus> mRentStack;
	private HashMap<String, ArrayList<RentMessage>> mWaitingMessages;
	
	public void start()
	{
		mRentStack = new TreeMap<Long, RentStatus>();
		mWaitingMessages = new HashMap<String, ArrayList<RentMessage>>();
		
		Bukkit.getPluginManager().registerEvents(this, RegionSigns.instance);
		
		int checkInterval = RegionSigns.config.checkInterval;
		int autoSaveInterval = RegionSigns.config.autosaveInterval;
		
		load();
		
		mTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(RegionSigns.instance, new RentProcessor(), checkInterval, checkInterval);
		
		mAutosaveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(RegionSigns.instance, new Runnable() {

			@Override
			public void run()
			{
				save();
			}
			
		},autoSaveInterval, autoSaveInterval );
	}
	
	public void stop()
	{
		save();
		Bukkit.getScheduler().cancelTask(mTaskId);
		Bukkit.getScheduler().cancelTask(mAutosaveTaskId);
	}
	
	public RentStatus popNextExpired()
	{
		if(mRentStack.size() == 0)
			return null;
		
		if(mRentStack.firstKey() > Calendar.getInstance().getTimeInMillis())
			return null;
		
		return mRentStack.pollFirstEntry().getValue();
	}
	
	public void pushRent(RentStatus rent, long expiryDate)
	{
		mRentStack.put(expiryDate, rent);
	}
	
	public void removeRent (RentStatus rent)
	{
		for(Iterator<Entry<Long, RentStatus>> it = mRentStack.entrySet().iterator(); it.hasNext();)
		{
			Entry<Long, RentStatus> entry = it.next();
			
			if(entry.getValue().equals(rent))
			{
				it.remove();
				return;
			}
		}
	}
	public Collection<RentStatus> getRenters()
	{
		return Collections.unmodifiableCollection(mRentStack.values());
	}
	
	public void sendMessage(RentMessage message, OfflinePlayer target)
	{
		if(target.isOnline())
		{
			Player player = target.getPlayer();
			
			player.sendMessage(getDisplayMessage(message));
		}
		else
		{
			if(!mWaitingMessages.containsKey(target.getName()))
				mWaitingMessages.put(target.getName(), new ArrayList<RentMessage>());
			
			mWaitingMessages.get(target.getName()).add(message);
		}
	}
	
	public void sendMessageToLandlords(RentMessage message, RentStatus status)
	{
		ProtectedRegion region = new Region(Bukkit.getWorld(status.World), status.Region).getProtectedRegion();
		if(region == null)
			return;
		
		for(String owner : region.getOwners().getPlayers())
			sendMessage(message, Bukkit.getOfflinePlayer(owner));
	}
	
	private String getDisplayMessage(RentMessage msg)
	{
		String message = RegionSigns.config.messagePrefix;
		
		switch(msg.Type)
		{
		case Eviction:
			message += RegionSigns.config.messageEvicted;
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.RED + "You have been evicted from '" + msg.Region + "'.Please contact an Admin or Mod to retrieve any remaining possesions.";
		case EvictionPending:
			message += RegionSigns.config.messageWarningEvict;
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.RED + "Warning: You will be evicted from '" + msg.Region + "' if payment is not recieved in " + FormatTimeDifference(msg.EventCompletionTime - Calendar.getInstance().getTimeInMillis(),2);
		case InsufficientFunds:
			message += RegionSigns.config.messageWarningFunds;
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.GOLD + "Warning: currently you have insufficient funds to pay rent next time";
		case RentEnded:
			message += RegionSigns.config.messageFinished;
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.WHITE + "You have finished renting '" + msg.Region + "'. Please contact an Admin or Mod to retrieve any remaining possesions.";
		case RentEnding:
			message += RegionSigns.config.messageTerminate;
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.WHITE + "You have stopped renting '" + msg.Region + "'. You have " + FormatTimeDifference(msg.EventCompletionTime - Calendar.getInstance().getTimeInMillis(),2) + " to move out.";
		case NextPaymentTime:
			message += RegionSigns.config.messageNextPayment;
			break;
		case PaymentSent:
			message += RegionSigns.config.messagePayment;
			break;
		case FirstPaymentTime:
			message += RegionSigns.config.messageFirstPayment;
			break;
		case RentBegin:
			message += RegionSigns.config.messageBeginRenting;
			break;
		case RentBeginFree:
			message += RegionSigns.config.messageBeginRentingFree;
			break;
		case PaymentReceived:
			message += RegionSigns.config.messagePaymentReceived;
			break;
		case EvictionLandlord:
			message += RegionSigns.config.messageTenantEvicted;
			break;
		case RentEndedLandlord:
			message += RegionSigns.config.messageTenantFinished;
			break;
		case RentEndingLandlord:
			message += RegionSigns.config.messageTenantTerminate;
			break;
		case RentBeginLandlord:
			message += RegionSigns.config.messageTenantBegin;
			break;
		}
		
		// Replace the colour tags
		message = ChatColor.translateAlternateColorCodes('&', message);

		// Now replace all tags
		message = message.replace("<region>", msg.Region);
		message = message.replace("<payment>", Util.formatCurrency(msg.Payment));
		message = message.replace("<time>", Util.formatTimeDifference(msg.EventCompletionTime - Calendar.getInstance().getTimeInMillis(),2, false));

		return message;
	}
	
	public void load()
	{
		loadCompat();
		loadRenters();
		loadMessages();
	}
	
	public boolean save()
	{
		if(!saveRenters())
			return false;
		
		if(!saveMessages())
			return false;
		
		return true;
	}
	
	// Load legacy storage file
	private void loadCompat()
	{
		File file = new File(RegionSigns.instance.getDataFolder(), "stored.yml");
		
		if(!file.exists())
			return;
		
		FileConfiguration stored = YamlConfiguration.loadConfiguration(file);
		
		if(stored == null)
			return;
		
		ConfigurationSection section = stored.getConfigurationSection("rentStatus");
		
		List<String> keys = section.getStringList("list");
		for(int i = 0; i < keys.size(); i++)
		{
			RentStatus status = new RentStatus();
			status.Tenant = section.getString(keys.get(i) + ".tenant");
			status.Region = section.getString(keys.get(i) + ".region");
			status.World = section.getString(keys.get(i) + ".world");
			status.PendingEviction = section.getBoolean(keys.get(i) + ".evict");
			status.PendingRemoval = section.getBoolean(keys.get(i) + ".remove");
			status.IntervalPayment = section.getDouble(keys.get(i) + ".payment");
			status.RentInterval = section.getLong(keys.get(i) + ".interval");
			status.NextIntervalEnd = section.getLong(keys.get(i) + ".next");
			status.Date = section.getLong(keys.get(i) + ".date", 0);
			pushRent(status, status.NextIntervalEnd - (status.PendingEviction ? status.RentInterval / 2 : 0));
		}
		
		if(stored.isConfigurationSection("rentMessages"))
		{
			section = stored.getConfigurationSection("rentMessages");
			keys = section.getStringList("list");
			
			for(int i = 0; i < keys.size(); i++)
			{
				List<String> messageKeys = section.getStringList(keys.get(i) + ".list");
				ArrayList<RentMessage> messages = new ArrayList<RentMessage>();
				
				for(int j = 0; j < messageKeys.size(); j++)
				{
					RentMessage msg = new RentMessage();
					msg.Type = RentMessageTypes.values()[section.getInt(keys.get(i) + "." + messageKeys.get(j) + ".type")];
					msg.EventCompletionTime = section.getLong(keys.get(i) + "." + messageKeys.get(j) + ".time");
					msg.Region = section.getString(keys.get(i) + "." + messageKeys.get(j) + ".region");
					msg.Payment = section.getDouble(keys.get(i) + "." + messageKeys.get(j) + ".payment");
					
					messages.add(msg);
				}
				
				mWaitingMessages.put(keys.get(i), messages);
			}
		}
		
		// Upgrade to the new format
		if(save())
			file.delete();
	}
	
	private boolean saveRenters()
	{
		FileConfiguration renters = new YamlConfiguration();
		
		for(Entry<Long, RentStatus> entry : mRentStack.entrySet())
		{
			ConfigurationSection section = renters.createSection(entry.getKey() + "");
			
			section.set("tenant", entry.getValue().Tenant);
			section.set("region", entry.getValue().Region);
			section.set("world", entry.getValue().World);
			section.set("evict", entry.getValue().PendingEviction);
			section.set("remove", entry.getValue().PendingRemoval);
			section.set("payment", entry.getValue().IntervalPayment);
			section.set("interval", entry.getValue().RentInterval);
			section.set("next", entry.getValue().NextIntervalEnd);
			section.set("date", entry.getValue().Date);
			
			if(entry.getValue().SignLocation != null)
			{
				ConfigurationSection location = section.createSection("sign");
				location.set("world", entry.getValue().SignLocation.getWorld().getName());
				location.set("x", entry.getValue().SignLocation.getX());
				location.set("y", entry.getValue().SignLocation.getY());
				location.set("z", entry.getValue().SignLocation.getZ());
				location.set("yaw", (double)entry.getValue().SignLocation.getYaw());
				location.set("pitch", (double)entry.getValue().SignLocation.getPitch());
			}
		}
		
		try
		{
			renters.save(new File(RegionSigns.instance.getDataFolder(), "renters.yml"));
			return true;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return false;
		}
	}
	private void loadRenters()
	{
		File file = new File(RegionSigns.instance.getDataFolder(), "renters.yml");
		
		if(!file.exists())
			return;
		
		FileConfiguration renters = YamlConfiguration.loadConfiguration(file);
		
		if(renters == null)
			return;
		
		mRentStack.clear();
		
		for(String key : renters.getKeys(false))
		{
			ConfigurationSection section = renters.getConfigurationSection(key);
			
			RentStatus rent = new RentStatus();
			
			rent.Tenant = section.getString("tenant");
			
			// Fix a mistake i made
			if(rent.Tenant.startsWith("CraftOfflinePlayer") || rent.Tenant.startsWith("CraftPlayer"))
				rent.Tenant = rent.Tenant.substring(rent.Tenant.indexOf('=')+1, rent.Tenant.length()-1);
			
			rent.Region = section.getString("region");
			rent.World = section.getString("world");
			rent.PendingEviction = section.getBoolean("evict");
			rent.PendingRemoval = section.getBoolean("remove");
			rent.IntervalPayment = section.getDouble("payment");
			rent.RentInterval = section.getLong("interval");
			rent.NextIntervalEnd = section.getLong("next");
			rent.Date = section.getLong("date");
			
			if(section.isConfigurationSection("sign"))
			{
				ConfigurationSection location = section.getConfigurationSection("sign");
				rent.SignLocation = new Location(Bukkit.getWorld(location.getString("world")), location.getDouble("x"), location.getDouble("y"), location.getDouble("z"), (float)location.getDouble("yaw"), (float)location.getDouble("pitch"));
			}
			
			pushRent(rent, Long.parseLong(key));
		}
	}
	
	
	private boolean saveMessages()
	{
		FileConfiguration messages = new YamlConfiguration();
		
		for(Entry<String, ArrayList<RentMessage>> entry : mWaitingMessages.entrySet())
		{
			if(entry.getValue().size() == 0)
				continue;
			
			ConfigurationSection section = messages.createSection(entry.getKey());
			
			for(int i = 0; i < entry.getValue().size(); ++i)
			{
				ConfigurationSection msg = section.createSection("" + i);
				
				msg.set("type", entry.getValue().get(i).Type.ordinal());
				msg.set("time", entry.getValue().get(i).EventCompletionTime);
				msg.set("payment", entry.getValue().get(i).Payment);
				msg.set("region", entry.getValue().get(i).Region);
			}
		}
		
		try
		{
			messages.save(new File(RegionSigns.instance.getDataFolder(), "messages.yml"));
			return true;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return false;
		}
	}
	
	private void loadMessages()
	{
		File file = new File(RegionSigns.instance.getDataFolder(), "messages.yml");
		
		if(!file.exists())
			return;
		
		FileConfiguration messages = YamlConfiguration.loadConfiguration(file);
		
		if(messages == null)
			return;
		
		mWaitingMessages.clear();
		
		for(String playerName : messages.getKeys(false))
		{
			ConfigurationSection section = messages.getConfigurationSection(playerName);
			
			ArrayList<RentMessage> waiting = new ArrayList<RentMessage>();
			
			for(String key : section.getKeys(false))
			{
				ConfigurationSection msgSection = section.getConfigurationSection(key);
				RentMessage msg = new RentMessage();
				msg.EventCompletionTime = msgSection.getLong("time");
				msg.Region = msgSection.getString("region");
				msg.Payment = msgSection.getDouble("payment");
				msg.Type = RentMessageTypes.values()[msgSection.getInt("type")];
				waiting.add(msg);
			}
			
			mWaitingMessages.put(playerName, waiting);
		}
	}
	
	public int getCountIn(Region region, Player player)
	{
		int count = 0;

		for(RentStatus status : mRentStack.values())
		{
			if(status.Tenant.equals(player))
			{
				if(region.isSuperGlobal())
					++count;
				else
				{
					if(region.isGlobal() && region.getWorld().getName().equals(status.World))
						++count;
					else
					{
						RegionManager man = RegionSigns.worldGuard.getRegionManager(Bukkit.getWorld(status.World));
						ProtectedRegion parent = man.getRegionExact(status.Region);
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
		return count;
	}
	public int getCount(Player player) 
	{
		int count = 0;
		for(RentStatus status : mRentStack.values())
		{
			if(status.Tenant.equals(player))
				count++;
		}
		return count;
	}
	
	public RentStatus getStatus(Region region)
	{
		for(RentStatus status : mRentStack.values())
		{
			if(status.Region.equals(region.getID()))
				return status;
		}
		
		return null;
	}
			
	
	@EventHandler
	private void onWorldSave(WorldSaveEvent event)
	{
		save();
	}
	
	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		if (mWaitingMessages.containsKey(event.getPlayer().getName()))
		{
			ArrayList<RentMessage> messages = mWaitingMessages.get(event.getPlayer().getName());
			
			long time = Calendar.getInstance().getTimeInMillis();
			
			// Process the messages
			for(int i = 0; i < messages.size(); i++)
			{
				if(messages.get(i).EventCompletionTime < time && messages.get(i).EventCompletionTime != 0)
				{
					// This message is no longer relevant
					messages.remove(i);
					i--;
					continue;
				}
				else
				{
					// Display the message
					event.getPlayer().sendMessage(getDisplayMessage(messages.get(i)));
				}
				
				// Check to see which ones we can remove
				if(messages.get(i).EventCompletionTime == 0)
				{
					messages.remove(i);
					i--;
				}
			}
			
			// Remove the whole list to save space if there are no more messages left
			if(messages.size() == 0)
				mWaitingMessages.remove(event.getPlayer().getName());
		}
	}
}
