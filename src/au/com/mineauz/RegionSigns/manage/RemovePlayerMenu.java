package au.com.mineauz.RegionSigns.manage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RemovePlayerMenu extends ValidatingPrompt implements ISubMenu
{
	private Prompt mParent = null;
	private boolean mNotFound;
	
	@Override
	public void setParent( Prompt parent )
	{
		mParent = parent;
	}
	
	@Override
	protected Prompt acceptValidatedInput( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return mParent;
		
		OfflinePlayer toAdd;
		toAdd = Bukkit.getOfflinePlayer(input);
		
		if(!toAdd.hasPlayedBefore())
			toAdd = Bukkit.getPlayer(input);
		
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		
		if(!region.getMembers().contains(toAdd.getName()))
		{
			((Player)context.getForWhom()).sendMessage(ChatColor.YELLOW + toAdd.getName() + ChatColor.RED + " was not a member of " + ChatColor.YELLOW + region.getId());
		}
		else
		{
			region.getMembers().removePlayer(toAdd.getName());
			
			((Player)context.getForWhom()).sendMessage("Removed " + ChatColor.YELLOW + toAdd.getName() + ChatColor.WHITE + " from " + ChatColor.YELLOW + region.getId());
		}
		
		
		return mParent;
	}
	
	@Override
	protected String getFailedValidationText( ConversationContext context, String invalidInput )
	{
		if(mNotFound)
			return "That player cannot be found";
		else
			return "That player isn't a member of this region"; 
	}
	
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		OfflinePlayer toAdd;
		toAdd = Bukkit.getOfflinePlayer(input);
		
		if(!toAdd.hasPlayedBefore())
			toAdd = Bukkit.getPlayer(input);
		
		if(toAdd == null)
			mNotFound = true;
		else
		{
			mNotFound = false;
			
			ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
			
			if(!region.isMember(toAdd.getName()))
				return false;
			
			return true;
		}
		return false;
	}
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		return "Enter the name of the player to remove or enter 'back' to go back"; 
	}

}
