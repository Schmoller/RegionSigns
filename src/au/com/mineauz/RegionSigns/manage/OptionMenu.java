package au.com.mineauz.RegionSigns.manage;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.flags.DefaultFlag;

public class OptionMenu extends StringPrompt implements ISubMenu
{
	private Prompt mParent;
	private ArrayList<Option> mOptions;
	
	public OptionMenu(Player player)
	{
		mOptions = new ArrayList<Option>();
		
		if(player.hasPermission("regionsigns.flags.pvp"))
			addOption("Allow PVP", new FlagSetter(DefaultFlag.PVP, "Enable PVP?"));
		
		if(player.hasPermission("regionsigns.flags.use"))
			addOption("Allow Interactions", new FlagSetter(DefaultFlag.USE, "Enable other player interactions with doors, buttons, plates, etc.?"));
		
		if(player.hasPermission("regionsigns.flags.greeting"))
			addOption("Change the greeting message", new FlagSetter(DefaultFlag.GREET_MESSAGE, "Enter the new message. Enter 'back' to cancel."));
		
		if(player.hasPermission("regionsigns.flags.farewell"))
			addOption("Change the farewell message", new FlagSetter(DefaultFlag.FAREWELL_MESSAGE, "Enter the new message. Enter 'back' to cancel."));
	}
	
	@Override
	public void setParent( Prompt parent )
	{
		mParent = parent;
	}

	@Override
	public Prompt acceptInput( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return mParent;
		
		try
		{
			int number = Integer.parseInt(input);
			
			if(number == 0)
				return mParent;
			
			if(number >=1 && number <= mOptions.size())
			{
				Option option = mOptions.get(number-1);
				if(option.dest != null)
				{
					return (Prompt)option.dest;
				}
			}
			
			return this;
		}
		catch(NumberFormatException e)
		{
			return this;
		}
	}

	@Override
	public String getPromptText( ConversationContext context )
	{
		String output = "What would you like to do? (enter the number)\n";
		
		int index = 1;
		for(Option option : mOptions)
		{
			output += ChatColor.YELLOW + "" + index + ": " + ChatColor.WHITE + option.name + "\n";
			++index;
		}
		output += ChatColor.YELLOW + "0: " + ChatColor.WHITE + "Back\n";
		return output;
	}

	private void addOption(String optionName, ISubMenu dest)
	{
		Option option = new Option();
		option.name = optionName;
		option.dest = dest;
		
		dest.setParent(this);
		
		mOptions.add(option);
	}
	
	private static class Option
	{
		public String name;
		public ISubMenu dest;
	}
}
