package au.com.mineauz.RegionSigns;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Util 
{
	
	public static String repeat(String str, int times)
	{
		String output = "";
		for(int i = 0; i < times; ++i)
			output += str;
		
		return output;
	}
	public static String translateColours(String text)
	{
		String translated = text;
		
		for(ChatColor color : ChatColor.values())
			translated = translated.replaceAll("%" + color.name().toLowerCase() + "%", color.toString());

		return translated;
	}
	public static byte BlockFaceToNotch(BlockFace face)
	{
		switch(face)
		{
		case DOWN:
			return 0;
		case UP:
			return 1;
		case NORTH:
			return 2;
		case SOUTH:
			return 3;
		case WEST:
			return 4;
		case EAST:
			return 5;
			
		default:
			return -1;
		}
	}
	
	public static String formatCurrency(double value)
	{
		if(value == 0.0)
			return "Free";
		
		String result = sCurrencyChar;
		result += String.format("%.2f", value);
		
		if(result.endsWith(".00"))
			result = result.substring(0,result.length()-3);
		
		return result;
	}
	public static double parseCurrency(String value)
	{
		if(value.isEmpty())
			return 0.0;
		
		if(value.equalsIgnoreCase("free"))
			return 0.0;
		// There is one char at the beginning that is the currency symbol
		if(value.startsWith(sCurrencyChar))
		{
			try 
			{ 
				double val = Double.parseDouble(value.substring(1));
				return val;
			} 
			catch(NumberFormatException e)
			{
				return Double.NaN;
			}
		}
		else
		{
			// Maybe its just the number
			try 
			{ 
				double val = Double.parseDouble(value);
				return val;
			} 
			catch(NumberFormatException e)
			{
				return Double.NaN;
			}
		}
	}
	
	public static String formatTimeDifference(long time, int precision, boolean noSingular)
	{
		String result = "";
		long remaining = time;
		
		int count = 0;
		
		// Years as 365.25 days
		if(remaining / 31557600000L >= 1)
		{
			int years = (int)Math.floor(remaining / 31557600000L);
			remaining -= years * 31557600000L;
			count++;
			
			if(noSingular && years == 1 && count == 1)
				result += "Year ";
			else
				result += years + " Year" + (years > 1 ? "s " : " ");
		}
		
		// Months as 30 days
		if(remaining / 2592000000L >= 1 && count < precision)
		{
			int months = (int)Math.floor(remaining / 2592000000L);
			remaining -= months * 2592000000L;
			count++;
			
			if(noSingular && months == 1 && count == 1)
				result += "Month ";
			else
				result += months + " Month" + (months > 1 ? "s " : " ");
		}
		
		// Weeks
		if(remaining / 604800000L >= 1 && count < precision)
		{
			int weeks = (int)Math.floor(remaining / 604800000L);
			remaining -= weeks * 604800000L;
			count++;
			
			if(noSingular && weeks == 1 && count == 1)
				result += "Week ";
			else
				result += weeks + " Week" + (weeks > 1 ? "s " : " ");
		}
		
		// days
		if(remaining / 86400000L >= 1 && count < precision)
		{
			int days = (int)Math.floor(remaining / 86400000L);
			remaining -= days * 86400000L;
			count++;
			
			if(noSingular && days == 1 && count == 1)
				result += "Day ";
			else
				result += days + " Day" + (days > 1 ? "s " : " ");
		}
		
		// Hours
		if(remaining / 3600000L >= 1 && count < precision)
		{
			int hours = (int)Math.floor(remaining / 3600000L);
			remaining -= hours * 3600000L;
			count++;
			
			if(noSingular && hours == 1 && count == 1)
				result += "Hour ";
			else
				result += hours + " Hour" + (hours > 1 ? "s " : " ");
		}
		
		// Minutes
		if(remaining / 60000L >= 1 && count < precision)
		{
			int minutes = (int)Math.floor(remaining / 60000L);
			remaining -= minutes * 60000L;
			count++;
			
			if(noSingular && minutes == 1 && count == 1)
				result += "Minute ";
			else
				result += minutes + " Minute" + (minutes > 1 ? "s " : " ");
		}
		
		// Seconds
		if(remaining / 1000L >= 1 && count < precision)
		{
			int seconds = (int)Math.floor(remaining / 1000L);
			remaining -= seconds * 1000L;
			count++;
			
			if(noSingular && seconds == 1 && count == 1)
				result += "Second ";
			else
				result += seconds + " Second" + (seconds > 1 ? "s " : " ");
		}
		
		if(count == 0)
		{
			result += "Now";
		}
		return result.trim();
	}
	public static String formatDateDiffShort(long time)
	{
		String result = "";
		long remaining = time;
		
		int count = 0;
		
		// Years as 365.25 days
		if(remaining / 31557600000L >= 1)
		{
			int years = (int)Math.floor(remaining / 31557600000L);
			remaining -= years * 31557600000L;
			count++;
			
			result += years + "y";
		}
		
		// Months as 30 days
		if(remaining / 2592000000L >= 1)
		{
			int months = (int)Math.floor(remaining / 2592000000L);
			remaining -= months * 2592000000L;
			count++;
			
			result += months + "mo";
		}
		
		// Weeks
		if(remaining / 604800000L >= 1)
		{
			int weeks = (int)Math.floor(remaining / 604800000L);
			remaining -= weeks * 604800000L;
			count++;
			
			result += weeks + "w";
		}
		
		// days
		if(remaining / 86400000L >= 1)
		{
			int days = (int)Math.floor(remaining / 86400000L);
			remaining -= days * 86400000L;
			count++;
			
			result += days + "d";
		}
		
		// Hours
		if(remaining / 3600000L >= 1)
		{
			int hours = (int)Math.floor(remaining / 3600000L);
			remaining -= hours * 3600000L;
			count++;
			
			result += hours + "h";
		}
		
		// Minutes
		if(remaining / 60000L >= 1)
		{
			int minutes = (int)Math.floor(remaining / 60000L);
			remaining -= minutes * 60000L;
			count++;
			
			result += minutes + "m";
		}
		
		// Seconds
		if(remaining / 1000L >= 1)
		{
			int seconds = (int)Math.floor(remaining / 1000L);
			remaining -= seconds * 1000L;
			count++;
			
			result += seconds + "s";
		}
		
		if(count == 0)
		{
			result += "Now";
		}
		return result.trim();
	}
	public static long parseDateDiff(String dateDiff)
	{
		if(dateDiff == null)
			return 0;
		
		Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$");
		dateDiff = dateDiff.toLowerCase();
		
		Matcher m = dateDiffPattern.matcher(dateDiff);
		
		if(m.matches())
		{
			int years,months,weeks,days,hours,minutes,seconds;
			boolean negative;
			
			if(m.group(1) != null)
				negative = (m.group(1).compareTo("-") == 0);
			else
				negative = false;

			if(m.group(2) != null)
				years = Integer.parseInt(m.group(2));
			else
				years = 0;
			
			if(m.group(3) != null)
				months = Integer.parseInt(m.group(3));
			else
				months = 0;
			
			if(m.group(4) != null)
				weeks = Integer.parseInt(m.group(4));
			else
				weeks = 0;
			
			if(m.group(5) != null)
				days = Integer.parseInt(m.group(5));
			else
				days = 0;
			
			if(m.group(6) != null)
				hours = Integer.parseInt(m.group(6));
			else
				hours = 0;
			
			if(m.group(7) != null)
				minutes = Integer.parseInt(m.group(7));
			else
				minutes = 0;
			
			if(m.group(8) != null)
				seconds = Integer.parseInt(m.group(8));
			else
				seconds = 0;
			
			// Now calculate the time
			long time = 0;
			time += seconds * 1000L;
			time += minutes * 60000L;
			time += hours * 3600000L;
			time += days * 86400000L;
			time += weeks * 604800000L;
			time += months * 2592000000L;
			time += years * 31557600000L;
			
			if(negative)
				time *= -1;
			
			return time;
		}
		
		return 0;
	}
	
	public static boolean playerHasEnough(OfflinePlayer player, double amount)
	{
		try
		{
			return Economy.hasEnough(player.getName(), amount);
		}
		catch(UserDoesNotExistException e)
		{
			return false;
		}
	}
	public static boolean playerHasEnough(String player, double amount)
	{
		try
		{
			return Economy.hasEnough(player, amount);
		}
		catch(UserDoesNotExistException e)
		{
			return false;
		}
	}
	
	public static boolean playerSubtractMoney(OfflinePlayer player, double amount)
	{
		try
		{
			Economy.subtract(player.getName(), amount);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	public static boolean playerSubtractMoney(String player, double amount)
	{
		try
		{
			Economy.subtract(player, amount);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	
	public static boolean playerAddMoney(OfflinePlayer player, double amount)
	{
		try
		{
			Economy.add(player.getName(), amount);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	
	public static double getPlayerMoney(String player)
	{
		try
		{
			return Economy.getMoney(player);
		}
		catch(Exception e)
		{
			return 0;
		}
	}
	
	
	
	public static ProtectedRegion getRegion(World world, String regionName)
	{
		if(RegionSigns.worldGuard.getRegionManager(world) == null)
			return null;
		
		return RegionSigns.worldGuard.getRegionManager(world).getRegion(regionName);
	}
	
	public static String makeNameList(Set<String> names)
	{
		String nameList = "";
		
		for(String name : names)
		{
			nameList += (!nameList.isEmpty() ? ", " : "") + name;
		}
		
		return nameList;
	}
	
	public static String makeNameList(Set<String> names, String replaceName, String with)
	{
		String nameList = "";
		
		for(String name : names)
		{
			if(name.equalsIgnoreCase(replaceName))
				name = with;
			
			nameList += (!nameList.isEmpty() ? ", " : "") + name;
		}
		
		return nameList;
	}
	
	public static <T> boolean arrayHas(T[] array, T object)
	{
		for(T entry : array)
		{
			if(entry == object)
				return true;
		}
		
		return false;
	}
	public static boolean saveRegionManager(World world)
	{
		try
		{
			RegionManager manager = RegionSigns.worldGuard.getRegionManager(world);
			if(manager == null)
				return false;
			
			manager.save();
			return true;
		}
		catch( ProtectionDatabaseException e ) {}
		
		return false;
	}
	
	public static String sCurrencyChar;
}
