package au.com.mineauz.RegionSigns.manage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.rent.RentStatus;
import au.com.mineauz.RegionSigns.rent.RentTransferConfirmation;

public class TransferRentMenu extends ValidatingPrompt implements ISubMenu
{
	private Prompt mParent;
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		return "Enter the full name of the player to transfer the tenantship to or 'back'.";
	}

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
		
		final RentStatus status = (RentStatus)context.getSessionData("status");
		final Player player = ((Player)context.getForWhom());
		
		final Player currentTenant, newTenant;
		
		currentTenant = Bukkit.getPlayerExact(status.Tenant);
		newTenant = Bukkit.getPlayerExact(input);
		
		// Check that they are online
		if(currentTenant == null || !currentTenant.isOnline())
		{
			player.sendMessage(ChatColor.RED + status.Tenant + " is not online. They must be online to transfer their tenantship.");
			return mParent;
		}
		if(newTenant == null || !newTenant.isOnline())
		{
			player.sendMessage(ChatColor.RED + input + " is not online or does not exist. They must be online to transfer the tenantship.");
			return mParent;
		}
		
		player.sendMessage("Both parties have been informed and are now required to accept before transfer will be complete.");
		Bukkit.getScheduler().scheduleSyncDelayedTask(RegionSigns.instance, new Runnable()
		{
			@Override
			public void run()
			{
				new RentTransferConfirmation(status, currentTenant, newTenant, player);
			}
		}, 10L);
		
		
		return Prompt.END_OF_CONVERSATION;
	}

	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		return true;
	}

}
