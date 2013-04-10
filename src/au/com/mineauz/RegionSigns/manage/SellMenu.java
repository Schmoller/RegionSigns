package au.com.mineauz.RegionSigns.manage;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.Util;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SellMenu extends ValidatingPrompt implements ISubMenu
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
		
		double amount;
		if(input.equalsIgnoreCase("free") || input.equalsIgnoreCase("nothing") || input.equalsIgnoreCase("none"))
			amount = 0;
		else
			amount = Util.parseCurrency(input);

		Location signLocation = (Location)context.getSessionData("sign");
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		
		
		if(signLocation.getBlock().getType() == Material.WALL_SIGN || signLocation.getBlock().getType() == Material.SIGN_POST)
		{
			Sign sign = (Sign)signLocation.getBlock().getState();
			sign.setLine(0, ChatColor.DARK_BLUE + "[For Sale]");
			sign.setLine(1, region.getId());
			sign.setLine(2, "");
			sign.setLine(3, Util.formatCurrency(amount));
			
			sign.update(true);

			((Player)context.getForWhom()).sendMessage(ChatColor.YELLOW + region.getId() + ChatColor.WHITE + " is now for sale for " + ChatColor.GREEN + Util.formatCurrency(amount));
			
			return Prompt.END_OF_CONVERSATION;
		}
		
		((Player)context.getForWhom()).sendMessage(ChatColor.RED + "Unable to sell region, the sign was removed.");
		return mParent;
	}
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		if(input.equalsIgnoreCase("free") || input.equalsIgnoreCase("nothing") || input.equalsIgnoreCase("none"))
			return true;
		
		Double amt = Util.parseCurrency(input);
		
		if(amt.equals(Double.NaN))
			return false;
		
		if(amt < 0)
			return false;
		
		return true;
	}
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		return "How much would you like to sell " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + " for? (Type an amount, 'free', 'nothing', or 'back')"; 
	}

}
