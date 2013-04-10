package au.com.mineauz.RegionSigns.manage;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ClearPlayerMenu extends ValidatingPrompt implements ISubMenu
{
	private Prompt mParent = null;
	
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
		
		if(input.equalsIgnoreCase("no"))
			return mParent;
		
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		
		region.setMembers(new DefaultDomain());
		
		((Player)context.getForWhom()).sendMessage("Cleared all members from " + ChatColor.YELLOW + region.getId());
		
		return mParent;
	}
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		if(input.equalsIgnoreCase("yes"))
			return true;
		
		if(input.equalsIgnoreCase("no"))
			return true;
		
		return false;
	}
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		return "Clear all players from " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + "? (Enter yes or no)"; 
	}

}
