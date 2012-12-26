package com.gmail.steven.schmoll.RegionSigns;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RentSystem implements Listener
{
	// Public Methods
	public static long MinimumRentPeriod;
	
	public RentSystem(final RegionSigns plugin, FileConfiguration storedSigns, File storedSignsFile)
	{
		final int checkInterval = plugin.getConfig().getInt("rent.check-interval",1200);

		mStoredSigns = storedSigns;
		mStoredSignsFile = storedSignsFile;
		
		mLogger = plugin.getLogger();
		mServer = plugin.getServer();
		mConfig = plugin.getConfig();
		
		// load the worldguard plugin
		mWorldGuard = (WorldGuardPlugin)mServer.getPluginManager().getPlugin("WorldGuard");
		
		mServer.getPluginManager().registerEvents(this,plugin);
		
		LoadRenters();
		mLogger.info("Loaded " + mRenters.size() + " current leases.");
		
		mLastAutosave = 0;
		
		// Schedule the checking of rent due dates 
		mServer.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				// Whether an entry was removed
				boolean changed = false;
				// Whether the list was reordered
				boolean softChanged = false;
				if(mRenters.size() != 0)
				{
					// The current time
					long time = Calendar.getInstance().getTimeInMillis();
					
					// Keep checking until none have expired
					while(true)
					{
						// in case the last one was removed
						if(mRenters.size() == 0)
							break;
						
						// Exit when the event hasnt expired
						if(mRenters.firstKey() > time)
							break;
						
						RentStatus status = mRenters.firstEntry().getValue();
						
						// If the player cant use economy, then they cant be charged :(, but they shouldnt have got one of the first place
						if(!Economy.playerExists(status.Tenant))
						{
							mRenters.remove(mRenters.firstKey());
							changed = true;
							continue;
						}
						
						// See if the player is online
						Player player = mServer.getPlayerExact(status.Tenant);
						
						// Check if its time to remove it
						if(status.PendingRemoval)
						{
							mLogger.info(status.Tenant + " finished renting '" + status.Region + "' and has been removed from it");
							ProtectedRegion region = mWorldGuard.getRegionManager(mServer.getWorld(status.World)).getRegion(status.Region); 
							// Remove the tenant(s)
							region.setMembers(new DefaultDomain());
							
							try 
							{
								mWorldGuard.getRegionManager(mServer.getWorld(status.World)).save();
							} 
							catch (ProtectionDatabaseException e) {}
							
							// remove the rent status
							mRenters.remove(mRenters.firstKey());
							changed = true;
							
							RentMessage finishedMsg = new RentMessage();
							finishedMsg.Type = RentMessageTypes.RentEnded;
							finishedMsg.Region = status.Region;
							finishedMsg.EventCompletionTime = 0;
							finishedMsg.Payment = 0;
							
							// Notify the tenant
							if(player != null)
								player.sendMessage(getDisplayMessage(finishedMsg));
							else
							{
								// Add a message they will see on next login
								AddRentMessage(finishedMsg,status.Tenant);
							}
						}
						else
						{
							// This one has expired, charge the rent
							if(status.IntervalPayment != 0)
							{
								try
								{
									if(Economy.hasEnough(status.Tenant, status.IntervalPayment))
									{
										Economy.subtract(status.Tenant, status.IntervalPayment);
										
										// The message about not having enough for next time
										RentMessage fundsWarning = new RentMessage();
										fundsWarning.Type = RentMessageTypes.InsufficientFunds;
										fundsWarning.EventCompletionTime = 0;
										fundsWarning.Region = status.Region;
										fundsWarning.Payment = status.IntervalPayment;
										
										// The message about a payment being made
										RentMessage paymentMade = new RentMessage();
										paymentMade.Type = RentMessageTypes.PaymentSent;
										paymentMade.EventCompletionTime = 0;
										paymentMade.Region = status.Region;
										paymentMade.Payment = status.IntervalPayment;
										
										// The message about the next payment time
										RentMessage paymentTime = new RentMessage();
										paymentTime.Type = RentMessageTypes.NextPaymentTime;
										paymentTime.EventCompletionTime = (status.PendingEviction ? status.NextIntervalEnd : status.NextIntervalEnd + status.RentInterval);
										paymentTime.Region = status.Region;
										paymentTime.Payment = status.IntervalPayment;
										
										// Display messages
										if(player != null)
										{
											player.sendMessage(getDisplayMessage(paymentMade));
											player.sendMessage(getDisplayMessage(paymentTime));
											
											if(!Economy.hasEnough(status.Tenant, status.IntervalPayment))
												player.sendMessage(getDisplayMessage(fundsWarning));
										}
										else
										{
											AddRentMessage(paymentMade, status.Tenant);
											if(!Economy.hasEnough(status.Tenant, status.IntervalPayment))
											{
												AddRentMessage(fundsWarning,status.Tenant);
											}
										}
										
										if(status.PendingEviction)
											status.PendingEviction = false;
										else
											status.NextIntervalEnd = status.NextIntervalEnd + status.RentInterval;
										
										// Put it back in the queue
										mRenters.remove(mRenters.firstKey());
										mRenters.put(status.NextIntervalEnd,status);
										
										softChanged = true;
									}
									else
									{
										// Problem. they dont have enough money to pay for the rent
										if(status.PendingEviction)
										{
											// They were unable to pay and have now been evicted
											// Remove the tenant(s)
											ProtectedRegion region = mWorldGuard.getRegionManager(mServer.getWorld(status.World)).getRegion(status.Region);
											region.setMembers(new DefaultDomain());
											try
											{
												mWorldGuard.getRegionManager(mServer.getWorld(status.World)).save();
											}
											catch (ProtectionDatabaseException e) {}
											
											RentMessage evicted = new RentMessage();
											evicted.Type = RentMessageTypes.Eviction;
											evicted.Region = status.Region;
											evicted.EventCompletionTime = 0;
											evicted.Payment = status.IntervalPayment;
											
											// Notify the tenant
											if(player != null)
												player.sendMessage(getDisplayMessage(evicted));
											else
											{
												// Add a message they will see on next login
												AddRentMessage(evicted,status.Tenant);
											}
											
											mRenters.remove(mRenters.firstKey());
											changed = true;
											
											mLogger.info(status.Tenant + " was evicted from '" + status.Region + "'");
										}
										else
										{
											// The region will go into pending eviction mode
											status.PendingEviction = true;
											
											RentMessage evictWarning = new RentMessage();
											evictWarning.Type = RentMessageTypes.EvictionPending;
											evictWarning.Region = status.Region;
											evictWarning.EventCompletionTime = status.NextIntervalEnd + status.RentInterval / 2;
											evictWarning.Payment = status.IntervalPayment;
											
											// Notify the tenant
											if(player != null)
												player.sendMessage(getDisplayMessage(evictWarning));
											else
											{
												// Add a message they will see on next login
												AddRentMessage(evictWarning,status.Tenant);
											}
											
											mLogger.info(status.Tenant + " is pending eviction from '" + status.Region + "' due to insufficient funds.");
											
											status.NextIntervalEnd = status.NextIntervalEnd + status.RentInterval;
											
											// Put it back in the queue
											mRenters.remove(mRenters.firstKey());
											mRenters.put(status.NextIntervalEnd - status.RentInterval / 2,status);
											
											softChanged = true;
										}
									}
								}
								catch(UserDoesNotExistException e)
								{
									mLogger.warning("Invalid data found: " + status.Tenant + " does not have an Ecomony account");
								}
								catch(NoLoanPermittedException e) {}
							}
							else
							{
								// Remove from the queue
								mRenters.remove(mRenters.firstKey());
							}
						}
					}
				}
				
				mLastAutosave += checkInterval;
				
				// hard changes should be changed immediately
				if(changed)
				{
					SaveRenters();
				}
				else if(softChanged)
				{
					// Soft changes should only be changed at most once every "rent.autosave-interval" frequency
					if(mLastAutosave >= plugin.getConfig().getInt("rent.autosave-interval"))
					{
						mLogger.info("Autosaving Renters");
						SaveRenters();
						mLastAutosave = 0;
					}
				}
			}
		}, checkInterval, checkInterval);
	}
	public void RegisterRenter(RentStatus status)
	{
		mRenters.put(status.NextIntervalEnd, status);
		SaveRenters();
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(args.length < 1)
			return false;
		
		if(args[0].compareToIgnoreCase("help") == 0)
		{
			handleHelpCommand(sender,args);
			return true;
		}
		else if(args[0].compareToIgnoreCase("info") == 0 && args.length == 2)
		{
			handleInfoCommand(sender,args);
			return true;
		}
		else if(args[0].compareToIgnoreCase("list") == 0 && (args.length == 2 || args.length == 3))
		{
			handleListCommand(sender,args);
			return true;
		}
		else if(args[0].compareToIgnoreCase("stop") == 0 && (args.length == 2 || args.length == 3))
		{
			handleStopCommand(sender,args,false);
			return true;
		}
		else if(args[0].compareToIgnoreCase("forcestop") == 0 && (args.length == 2 || args.length == 3))
		{
			handleStopCommand(sender,args,true);
			return true;
		}
		else if(args[0].compareToIgnoreCase("set") == 0 && args.length == 3)
		{
			handleSetCommand(sender,args);
			return true;
		}
		else if(args[0].compareToIgnoreCase("transfer") == 0 && args.length == 3)
		{
			handleTransferCommand(sender,args);
			return true;
		}
		else if(args[0].compareToIgnoreCase("reload") == 0 && args.length == 1)
		{
			RegionSigns.instance.reloadConfig();
			RegionSigns.instance.initializeConfig();
			sender.sendMessage("Configuration Reloaded");
			
			return true;
		}
		return false;
	}
	public void SaveRenters()
	{
		ConfigurationSection section = mStoredSigns.createSection("rentStatus");
		String[] keys = new String[mRenters.size()];
		
		for(int i = 0; i < mRenters.size(); i++)
		{
			RentStatus status = mRenters.get(mRenters.keySet().toArray()[i]);
			keys[i] = status.Region;
			section.set(keys[i] + ".tenant", status.Tenant);
			section.set(keys[i] + ".region", status.Region);
			section.set(keys[i] + ".world", status.World);
			section.set(keys[i] + ".evict", status.PendingEviction);
			section.set(keys[i] + ".remove", status.PendingRemoval);
			section.set(keys[i] + ".payment", status.IntervalPayment);
			section.set(keys[i] + ".interval", status.RentInterval);
			section.set(keys[i] + ".next", status.NextIntervalEnd);
			section.set(keys[i] + ".date", status.Date);
		}
		
		section.set("list", Arrays.asList(keys));
		
		section = mStoredSigns.createSection("rentMessages");
		keys = new String[mMessages.size()];
		
		for(int i = 0; i < mMessages.size(); i++)
		{
			ArrayList<RentMessage> messages = mMessages.get(mMessages.keySet().toArray()[i]);
			keys[i] = (String)mMessages.keySet().toArray()[i];
			
			String[] messagesList = new String[messages.size()];
			for(int j = 0; j < messages.size(); j++)
			{
				messagesList[j] = "" + j;
				section.set(keys[i] + "." + messagesList[j] + ".type", messages.get(j).Type.ordinal());
				section.set(keys[i] + "." + messagesList[j] + ".time", messages.get(j).EventCompletionTime);
				section.set(keys[i] + "." + messagesList[j] + ".payment", messages.get(j).Payment);
				section.set(keys[i] + "." + messagesList[j] + ".region", messages.get(j).Region);
			}
			section.set(keys[i] + ".list", Arrays.asList(messagesList));
		}
		
		section.set("list", Arrays.asList(keys));
		
		try
		{
			mStoredSigns.save(mStoredSignsFile);
		}
		catch(IOException e)
		{
			mLogger.severe("Failed to save renters");
		}
	}
	
	

	// Private Methods
	
	private void AddRentMessage(RentMessage msg, String player)
	{
		// Add the message to the queue
		if(mMessages.get(player) != null)
		{
			mMessages.get(player).add(msg);
		}
		else
		{
			mMessages.put(player,new ArrayList<RentMessage>());
			mMessages.get(player).add(msg);
		}
	}
	
	public String getDisplayMessage(RentMessage msg)
	{
		String message = mConfig.getString("rent.messages.prefix","");
		
		switch(msg.Type)
		{
		case Eviction:
			message += mConfig.getString("rent.messages.evicted");
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.RED + "You have been evicted from '" + msg.Region + "'.Please contact an Admin or Mod to retrieve any remaining possesions.";
		case EvictionPending:
			message += mConfig.getString("rent.messages.warning-evict");
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.RED + "Warning: You will be evicted from '" + msg.Region + "' if payment is not recieved in " + FormatTimeDifference(msg.EventCompletionTime - Calendar.getInstance().getTimeInMillis(),2);
		case InsufficientFunds:
			message += mConfig.getString("rent.messages.warning-funds");
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.GOLD + "Warning: currently you have insufficient funds to pay rent next time";
		case RentEnded:
			message += mConfig.getString("rent.messages.finished");
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.WHITE + "You have finished renting '" + msg.Region + "'. Please contact an Admin or Mod to retrieve any remaining possesions.";
		case RentEnding:
			message += mConfig.getString("rent.messages.terminate");
			break;
			//return ChatColor.GREEN + "[Rent] " + ChatColor.WHITE + "You have stopped renting '" + msg.Region + "'. You have " + FormatTimeDifference(msg.EventCompletionTime - Calendar.getInstance().getTimeInMillis(),2) + " to move out.";
		case NextPaymentTime:
			message += mConfig.getString("rent.messages.next-payment");
			break;
		case PaymentSent:
			message += mConfig.getString("rent.messages.payment");
			break;
		case FirstPaymentTime:
			message += mConfig.getString("rent.messages.first-payment");
			break;
		case RentBegin:
			message += mConfig.getString("rent.messages.begin");
			break;
		case RentBeginFree:
			message += mConfig.getString("rent.messages.begin-free");
			break;
		}
		
		// Replace the colour tags
		message = message.replace("&", "§");

		// Now replace all tags
		message = message.replace("<region>", msg.Region);
		message = message.replace("<payment>", Util.formatCurrency(msg.Payment));
		message = message.replace("<time>", Util.formatTimeDifference(msg.EventCompletionTime - Calendar.getInstance().getTimeInMillis(),2, false));

		return message;
	}
	@EventHandler(priority = EventPriority.LOWEST)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		// Check if there are messages to show them
		if(mMessages.containsKey(event.getPlayer().getName()))
		{
			ArrayList<RentMessage> messages = mMessages.get(event.getPlayer().getName());
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
				mMessages.remove(event.getPlayer().getName());
		}
	}
	
	private void handleHelpCommand(CommandSender sender, String[] args)
	{
		sender.sendMessage(ChatColor.GOLD + "Region Renting (part of RegionSigns " + RegionSigns.instance.getDescription().getVersion() + ") by Schmoller");
		sender.sendMessage(ChatColor.GOLD + "Commands: ");
		sender.sendMessage(ChatColor.GOLD + " /rent help: " + ChatColor.WHITE + "Displays help");
		sender.sendMessage(ChatColor.GOLD + " /rent info <region>: " + ChatColor.WHITE + "Displays information about a rented region");
		
		// Only display this command if they have the permission to use it
		if(sender.hasPermission("regionsigns.rent.list.others"))
			sender.sendMessage(ChatColor.GOLD + " /rent list .<player> [page]: " + ChatColor.WHITE + "Lists all the rented regions for that player");
		
		// Only display this command if they have the permission to use it
		if(sender.hasPermission("regionsigns.rent.list.me") && sender instanceof Player)
			sender.sendMessage(ChatColor.GOLD + " /rent list me [page]: " + ChatColor.WHITE + "Lists all your rented regions");
		
		// Only display this command if they have the permission to use it
		if(sender.hasPermission("regionsigns.rent.list.all"))
			sender.sendMessage(ChatColor.GOLD + " /rent list all [page]: " + ChatColor.WHITE + "Lists all the rented regions for all players");
		
		if(sender instanceof Player)
			sender.sendMessage(ChatColor.GOLD + " /rent stop <region>: " + ChatColor.WHITE + "Stops renting that region");
		
		if(sender.hasPermission("regionsigns.rent.stop.others"))
			sender.sendMessage(ChatColor.GOLD + " /rent stop <region> <player>: " + ChatColor.WHITE + "Stops renting that region for another player");
		
		if(sender.hasPermission("regionsigns.rent.forcestop"))
			sender.sendMessage(ChatColor.GOLD + " /rent forcestop <region>: " + ChatColor.WHITE + "Immediately terminates a lease. No final payment will be made and the region will be removed from their name");
		
		if(sender.hasPermission("regionsigns.rent.forcestop.others"))
			sender.sendMessage(ChatColor.GOLD + " /rent forcestop <region> <player>: " + ChatColor.WHITE + "Immediately terminates a lease for another player. No final payment will be made and the region will be removed from their name");
		
		if(sender.hasPermission("regionsigns.rent.set"))
			sender.sendMessage(ChatColor.GOLD + " /rent set <region> <amount>: " + ChatColor.WHITE + "Changes the amount of rent the tennant of <region> must pay");
		
		if(sender.hasPermission("regionsigns.rent.transfer"))
			sender.sendMessage(ChatColor.GOLD + " /rent transfer <region> <player>: " + ChatColor.WHITE + "Transfers tennantship of <region> to <player>. Both the original tenant and the new tenant must accept");
		
		if(sender.hasPermission("regionsigns.config.reload"))
			sender.sendMessage(ChatColor.GOLD + " /rent reload: " + ChatColor.WHITE + "Reloads the configuration");
	}
	private void handleInfoCommand(CommandSender sender, String[] args)
	{
		String region = args[1];
		RentStatus regionStatus = null;
		// Find a matching region
		for(int i = 0; i < mRenters.size(); i++)
		{
			RentStatus status = (RentStatus)mRenters.values().toArray()[i];
			if(status.Region.compareToIgnoreCase(region) == 0)
			{
				// Check if they are allowed to look at others' regions
				if(!sender.hasPermission("regionsigns.rent.info.others") && (sender instanceof Player))
				{
					if(status.Tenant.compareToIgnoreCase(((Player)sender).getName()) != 0)
						continue;
				}
				
				regionStatus = status;
				break;
			}
		}
		
		if(regionStatus == null)
			sender.sendMessage(ChatColor.RED + "Unable to find a rented region with that id");
		else
		{
			sender.sendMessage("Rent Status for region '" + regionStatus.Region + "'");
			sender.sendMessage("  Tenant:          " + regionStatus.Tenant);
			String status =    "  Status:          ";
			
			if(regionStatus.PendingEviction)
				status += ChatColor.RED + "Pending Eviction";
			else if(regionStatus.PendingRemoval)
				status += ChatColor.GOLD + "Ending Rent";
			else
				status += ChatColor.GREEN + "Normal";
			
			sender.sendMessage(status);
			if(regionStatus.PendingEviction)
			{
				sender.sendMessage("  Evicted In: " + Util.formatTimeDifference(regionStatus.NextIntervalEnd - regionStatus.RentInterval - Calendar.getInstance().getTimeInMillis(),2, false));
				sender.sendMessage("  Next Payment:    " + Util.formatCurrency(regionStatus.IntervalPayment));
			}
			else if(regionStatus.PendingRemoval)
			{
				sender.sendMessage("  Terminated In: " + Util.formatTimeDifference(regionStatus.NextIntervalEnd - Calendar.getInstance().getTimeInMillis(),2, false));
			}
			else
			{
				sender.sendMessage("  Next Payment In: " + Util.formatTimeDifference(regionStatus.NextIntervalEnd - Calendar.getInstance().getTimeInMillis(),2, false));
				sender.sendMessage("  Next Payment:    " + Util.formatCurrency(regionStatus.IntervalPayment));
			}
				
			
			if(regionStatus.Date != 0)
				sender.sendMessage("  Renting Since:   " + DateFormat.getDateTimeInstance().format(new Date(regionStatus.Date)));
			
		}
	}
	private void handleListCommand(CommandSender sender, String[] args)
	{
		// The number of items to show on a page
		int cItemsPerPage = 6;
		// The console can show more at a time
		if(!(sender instanceof Player))
			cItemsPerPage = 40;
		
		int page = 0;
		String mode = args[1];
		
		// Get the page number
		if(args.length == 3)
		{
			// Check its format
			if(!args[2].matches("[0-9]+"))
				return;
			
			// Parse it
			try
			{
				page = Integer.parseInt(args[2]) - 1;
			}
			catch(NumberFormatException e) {}
			
			if(page == 0)
				return;
		}
		
		// Build the results list
		ArrayList<String> results = new ArrayList<String>();

		// All results are requested
		if(mode.compareToIgnoreCase("all") == 0)
		{
			// Check permissions
			if(!sender.hasPermission("regionsigns.rent.list.all"))
			{
				sender.sendMessage(ChatColor.RED + "You dont have permission to use that");
				return;
			}
			
			// get all the results
			for(int i = 0; i < mRenters.size(); i++)
			{
				RentStatus status = (RentStatus)mRenters.values().toArray()[i];
				results.add(String.format("%1$-30s %2$s", status.Region, status.Tenant));
			}
		}
		// Only regions that the calling player rents
		else if(mode.compareToIgnoreCase("me") == 0)
		{
			if(!(sender instanceof Player))
			{
				sender.sendMessage("A player is required to use this command");
				return;
			}
			if(!sender.hasPermission("regionsigns.rent.list.me"))
			{
				sender.sendMessage(ChatColor.RED + "You dont have permission to use that");
				return;
			}
			// get all the results
			for(int i = 0; i < mRenters.size(); i++)
			{
				RentStatus status = (RentStatus)mRenters.values().toArray()[i];
				if(status.Tenant.compareToIgnoreCase(((Player)sender).getName()) == 0)
					results.add(String.format("%1$-30s %2$s", status.Region, status.Tenant));
			}
		}
		else if(mode.startsWith("."))
		{
			if(!sender.hasPermission("regionsigns.rent.list.others"))
			{
				sender.sendMessage(ChatColor.RED + "You dont have permission to use that");
				return;
			}
			
			Player player = mServer.getPlayer(mode.substring(1));
			String playerName;
			if(player != null)
				playerName = player.getName();
			else
				playerName = mode.substring(1);
			
			// get all the results
			for(int i = 0; i < mRenters.size(); i++)
			{
				RentStatus status = (RentStatus)mRenters.values().toArray()[i];
				if(status.Tenant.compareToIgnoreCase(playerName) == 0)
					results.add(String.format("%1$-30s %2$s", status.Region, status.Tenant));
			}
		}
		
		int totalPages = (int)Math.ceil(results.size()/cItemsPerPage); 
		// Make sure they dont specify a page that doesnt exist
		if(page > totalPages)
			page = totalPages;
		
		// Display the results
		sender.sendMessage("----Rented Regions----Page " + (page + 1) + " of " + (totalPages + 1));
		for(int i = (page * cItemsPerPage); i < (page * cItemsPerPage + cItemsPerPage) && i < results.size(); i++)
		{
			sender.sendMessage(results.get(i));
		}
		if(page != totalPages)
			sender.sendMessage("Use /rent list " + mode + " " + (page + 2));
	}
	private void handleStopCommand(CommandSender sender, String[] args, boolean force)
	{
		if(force && !sender.hasPermission("regionsigns.rent.forcestop"))
		{
			sender.sendMessage(ChatColor.RED + "You dont have permission to do that");
			return;
		}
		String regionName = args[1];
		RentStatus regionStatus = null;
		long key = 0;
		// Find a matching region
		for(int i = 0; i < mRenters.size(); i++)
		{
			RentStatus status = (RentStatus)mRenters.values().toArray()[i];
			if(status.Region.compareToIgnoreCase(regionName) == 0)
			{
				if(sender instanceof Player)
				{
					if(status.Tenant.compareToIgnoreCase(((Player)sender).getName()) != 0)
					{
						if((!force && sender.hasPermission("regionsigns.rent.stop.others")) || (force && sender.hasPermission("regionsigns.rent.forcestop.others")))
						{
							// Make sure that the extra argument is in place
							if(args.length != 3)
							{
								if(force)
									sender.sendMessage(ChatColor.RED + "To stop other players from renting a region, you must specify the name of the player that is renting the region. ie. /rent forcestop <region> <player>");
								else
									sender.sendMessage(ChatColor.RED + "To stop other players from renting a region, you must specify the name of the player that is renting the region. ie. /rent stop <region> <player>");
								return;
							}
							// Check that the argument is correct
							if(status.Tenant.compareToIgnoreCase(args[2]) != 0)
							{
								sender.sendMessage(ChatColor.RED + "The player specified is not the one listed as the tenant of the region. You must enter their full name.");
								return;
							}
						}
						else
						{
							sender.sendMessage(ChatColor.RED + "You dont have permission to do that");
							return;
						}
					}
				}
				else
				{
					// Make sure that the extra argument is in place
					if(args.length != 3)
					{
						if(force)
							sender.sendMessage(ChatColor.RED + "To stop other players from renting a region, you must specify the name of the player that is renting the region. ie. /rent forcestop <region> <player>");
						else
							sender.sendMessage(ChatColor.RED + "To stop other players from renting a region, you must specify the name of the player that is renting the region. ie. /rent stop <region> <player>");
						return;
					}
					// Check that the argument is correct
					if(status.Tenant.compareToIgnoreCase(args[2]) != 0)
					{
						sender.sendMessage(ChatColor.RED + "The player specified is not the one listed as the tenant of the region. You must enter their full name.");
						return;
					}
					
				}
				
				regionStatus = status;
				if(RentSystem.MinimumRentPeriod != 0 && (Calendar.getInstance().getTimeInMillis() - regionStatus.Date < RentSystem.MinimumRentPeriod) && !force && !sender.hasPermission("regionsigns.rent.nominperiod"))
				{
					if(sender instanceof Player && ((Player)sender).getName().equalsIgnoreCase(status.Tenant))
						sender.sendMessage(ChatColor.RED + "You cannot stop renting this region yet. You are required to rent for at least " + Util.formatTimeDifference(RentSystem.MinimumRentPeriod - (Calendar.getInstance().getTimeInMillis() - regionStatus.Date), 2, false) + " more");
					else
						sender.sendMessage(ChatColor.RED + status.Tenant + " cannot stop renting this region yet. They are required to rent for at least " + Util.formatTimeDifference(RentSystem.MinimumRentPeriod - (Calendar.getInstance().getTimeInMillis() - regionStatus.Date), 2, false) + " more");
					
					return;
				}
				// All good
				key = (Long)mRenters.keySet().toArray()[i];
				break;
			}
		}
		
		if(regionStatus == null)
		{
			sender.sendMessage(ChatColor.RED + "Unable to find a rented region with that id");
		}
		else
		{
			if(force)
			{
				mLogger.info(regionStatus.Tenant + " finished renting '" + regionStatus.Region + "' and has been removed from it");
				// Remove the tenant
				ProtectedRegion region = mWorldGuard.getRegionManager(mServer.getWorld(regionStatus.World)).getRegion(regionStatus.Region);
				
				region.setMembers(new DefaultDomain());
				
				try 
				{
					mWorldGuard.getRegionManager(mServer.getWorld(regionStatus.World)).save();
				} 
				catch (ProtectionDatabaseException e) {}
				
				// remove the rent status
				mRenters.remove(key);
				
				// The lease is terminated now
				RentMessage msg = new RentMessage();
				msg.Type = RentMessageTypes.RentEnded;
				msg.EventCompletionTime = 0;
				msg.Region = regionStatus.Region;
				msg.Payment = 0;

				Player player = mServer.getPlayerExact(regionStatus.Tenant);
				// Notify the tenant
				if(player != null)
					player.sendMessage(getDisplayMessage(msg));
				else
				{
					// Add a message they will see on next login
					AddRentMessage(msg,regionStatus.Tenant);
				}
				sender.sendMessage(ChatColor.GREEN + regionStatus.Tenant + " finished renting '" + regionStatus.Region + "' and has been removed from it");
				SaveRenters();
			}
			else
			{
				
				// They are required to pay their next rent and will be removed from the lot at the next rent interval
				RentMessage msg = new RentMessage();
				msg.Type = RentMessageTypes.RentEnding;
				msg.EventCompletionTime = regionStatus.NextIntervalEnd;
				msg.Region = regionStatus.Region;
				msg.Payment = regionStatus.IntervalPayment;
				
				regionStatus.PendingRemoval = true;
				if(mServer.getPlayerExact(regionStatus.Tenant) != null)
				{
					Player owner = mServer.getPlayerExact(regionStatus.Tenant);
					owner.sendMessage(getDisplayMessage(msg));
				}
				else
				{
					// Add a message they will see on next login
					AddRentMessage(msg,regionStatus.Tenant);
				}
				sender.sendMessage(ChatColor.GREEN + regionStatus.Tenant + " ended renting '" + regionStatus.Region + "'. They will be removed once their rent period is up");
			}
		}
	}
	private void handleSetCommand(CommandSender sender, String[] args)
	{
		if(!sender.hasPermission("regionsigns.rent.set"))
		{
			sender.sendMessage(ChatColor.RED + "You dont have permisison to set rent.");
			return;
		}
		
		String region = args[1];
		RentStatus regionStatus = null;
		// Find a matching region
		for(int i = 0; i < mRenters.size(); i++)
		{
			RentStatus status = (RentStatus)mRenters.values().toArray()[i];
			if(status.Region.compareToIgnoreCase(region) == 0)
			{
				regionStatus = status;
				break;
			}
		}
		
		if(regionStatus == null)
			sender.sendMessage(ChatColor.RED + "Unable to find a rented region with that id");
		else
		{
			// Try to parse the amount
			double amount = 0;
			try
			{
				amount = Double.parseDouble(args[2]);
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(ChatColor.RED + "Value expected, String found instead.");
				return;
			}
			
			regionStatus.IntervalPayment = amount;
			SaveRenters();
			
			sender.sendMessage("Rent for region '" + regionStatus.Region + "' has been set to " + Util.formatCurrency(amount) + " per " + Util.formatTimeDifference(regionStatus.RentInterval, 1, true));
		}
	}
	private void handleTransferCommand(CommandSender sender, String[] args)
	{
		if(!sender.hasPermission("regionsigns.rent.transfer"))
		{
			sender.sendMessage(ChatColor.RED + "You dont have permisison to transfer tenantship.");
			return;
		}
		
		String region = args[1];
		RentStatus regionStatus = null;
		// Find a matching region
		for(int i = 0; i < mRenters.size(); i++)
		{
			RentStatus status = (RentStatus)mRenters.values().toArray()[i];
			if(status.Region.compareToIgnoreCase(region) == 0)
			{
				regionStatus = status;
				break;
			}
		}
		
		if(regionStatus == null)
		{
			sender.sendMessage(ChatColor.RED + "The region specified does not exist");
			return;
		}
		
		Player currentTenant, newTenant;
		
		currentTenant = mServer.getPlayerExact(regionStatus.Tenant);
		newTenant = mServer.getPlayer(args[2]);
		
		// Check that they are online
		if(currentTenant == null || !currentTenant.isOnline())
		{
			sender.sendMessage(ChatColor.RED + regionStatus.Tenant + " is not online. They must be online to transfer their tenantship.");
			return;
		}
		if(newTenant == null || !newTenant.isOnline())
		{
			sender.sendMessage(ChatColor.RED + args[2] + " is not online or does not exist. They must be online to transfer the tenantship.");
			return;
		}
		
		
		sender.sendMessage("Both parties have been informed and are now required to accept before transfer will be complete.");
		new RentTransferConfirmation(regionStatus, currentTenant, newTenant, sender);
	}
	
	void LoadRenters()
	{
		if(!mStoredSigns.isConfigurationSection("rentStatus"))
			return;
		
		ConfigurationSection section = mStoredSigns.getConfigurationSection("rentStatus");
		mRenters.clear();

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
			mRenters.put(status.NextIntervalEnd, status);
		}
		
		if(!mStoredSigns.isConfigurationSection("rentMessages"))
			return;
		section = mStoredSigns.getConfigurationSection("rentMessages");
		keys = section.getStringList("list");
		
		for(int i = 0; i < keys.size(); i++)
		{
			List<String> messages = section.getStringList(keys.get(i) + ".list");
			mMessages.put(keys.get(i),new ArrayList<RentMessage>());
			
			for(int j = 0; j < messages.size(); j++)
			{
				RentMessage msg = new RentMessage();
				msg.Type = RentMessageTypes.values()[section.getInt(keys.get(i) + "." + messages.get(j) + ".type")];
				msg.EventCompletionTime = section.getLong(keys.get(i) + "." + messages.get(j) + ".time");
				msg.Region = section.getString(keys.get(i) + "." + messages.get(j) + ".region");
				msg.Payment = section.getDouble(keys.get(i) + "." + messages.get(j) + ".payment");
				
				mMessages.get(keys.get(i)).add(msg);
			}
		}
	}
	
	// Private Members
	
	private TreeMap<Long,RentStatus> mRenters = new TreeMap<Long,RentStatus>();
	private HashMap<String,ArrayList<RentMessage>> mMessages = new HashMap<String,ArrayList<RentMessage>>();
	
	private FileConfiguration mStoredSigns;
	private File mStoredSignsFile;
	
	private WorldGuardPlugin mWorldGuard;
	private Logger mLogger;
	private Server mServer;
	private FileConfiguration mConfig;
	
	private int mLastAutosave;

	public int getCountIn(ProtectedRegion parent, Player player)
	{
		int count = 0;

		for(RentStatus status : mRenters.values())
		{
			if(status.Tenant == player.getName())
			{
				RegionManager man = mWorldGuard.getRegionManager(mServer.getWorld(status.World));
				ProtectedRegion region = man.getRegionExact(status.Region);
				
				if(region.getParent() == parent)
					count++;
			}
		}
		return count;
	}
	public int getCount(Player player) 
	{
		int count = 0;
		for(RentStatus status : mRenters.values())
		{
			if(status.Tenant == player.getName())
				count++;
		}
		return count;
	}
}
