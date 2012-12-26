package au.com.mineauz.RegionSigns;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.events.RegionClaimEvent;

import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ClaimConfirmation implements ConversationAbandonedListener
{
	class ConfirmationPrompt extends StringPrompt
	{
		@Override
		public String getPromptText(ConversationContext context) 
		{
			ProtectedRegion region = (ProtectedRegion)context.getSessionData("r");
			//Player newOwner = (Player)context.getSessionData("new");
			double amount = (Double)context.getSessionData("amt");
			//InteractableSignState sign = (InteractableSignState)context.getSessionData("sign");
			boolean failed = (Boolean)context.getSessionData("fail");
			
			if(failed)
			{
				return "Please enter 'yes' or 'no'";
			}
			else
			{
				if(amount == 0)
					return "Do you wish to claim " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + " as your own? (Type yes or no)";
				else
					return "Do you wish to purchase " + ChatColor.YELLOW + region.getId() + ChatColor.WHITE + " for " + ChatColor.GREEN + Util.formatCurrency(amount) + ChatColor.WHITE  + "? (Type yes or no)";
			}
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) 
		{
			if(input.compareToIgnoreCase("yes") == 0)
			{
				ProtectedRegion region = (ProtectedRegion)context.getSessionData("r");
				Player player = (Player)context.getSessionData("new");
				double amount = (Double)context.getSessionData("amt");
				InteractableSignState sign = (InteractableSignState)context.getSessionData("sign");
				
				ClaimSign.Reason reason = ClaimSign.canUserClaim(region, player, amount);
				
				if(reason != ClaimSign.Reason.Ok)
					player.sendMessage(ChatColor.RED + ClaimSign.getDisplayMessage(reason));
				else
				{
					// Check that the sign still exists
					if(sign.SignLocation.getBlock().getType() != Material.SIGN_POST && sign.SignLocation.getBlock().getType() != Material.WALL_SIGN)
						player.sendMessage(ChatColor.RED + "Unable to claim region. It is no longer available to claim");
					else
					{
						Sign signState = (Sign)sign.SignLocation.getBlock().getState();
						
						String[] lines = signState.getLines();
						
						for(int i = 0; i < 4; ++i)
						{
							lines[i] = RegionSigns.instance.getConfig().getString("claim.sign.line" + (i+1), "");
							lines[i] = lines[i].replaceAll("<user>", player.getName());
							lines[i] = lines[i].replaceAll("<region>", region.getId());
						}
						
						RegionClaimEvent event = new RegionClaimEvent(region, player, amount, lines);
						
						// Pre-check
						if(!Util.playerHasEnough(player, amount))
						{
							player.sendMessage(ChatColor.RED + "Unable to claim region. Insufficient funds");
							return Prompt.END_OF_CONVERSATION;
						}
						
						Bukkit.getPluginManager().callEvent(event);
						
						if(event.isCancelled())
						{
							player.sendMessage(ChatColor.RED + "Unable to claim region. Your claim was cancelled.");
							return Prompt.END_OF_CONVERSATION;
						}
						
						amount = event.getPayment();
						
						// Handle payment
						if(!Util.playerSubtractMoney(player, amount))
						{
							player.sendMessage(ChatColor.RED + "Unable to claim region. Insufficient funds");
							return Prompt.END_OF_CONVERSATION;
						}
						
						if(amount > 0)
							player.sendMessage(ChatColor.GREEN + "You have purchased " + region.getId() + " for " + Util.formatCurrency(amount));
						else
							player.sendMessage(ChatColor.GREEN + "You have claimed " + region.getId());
						
						// Add the player to the 
						region.getOwners().addPlayer(player.getName());
						try {
							RegionSigns.worldGuard.getRegionManager(sign.SignLocation.getWorld()).save();
						} catch (ProtectionDatabaseException e) {
						}
						
						// Change the sign text
						for(int i = 0; i < 4; i++)
							signState.setLine(i, event.getSignLines()[i]);
						
						signState.update(true);
					}					
				}
				context.setSessionData("end", true);
				
				return Prompt.END_OF_CONVERSATION;
			}
			else if (input.compareToIgnoreCase("no") == 0)
			{
				context.setSessionData("end", false);
				return Prompt.END_OF_CONVERSATION;
			}
			else
			{
				context.setSessionData("fail", true);
				// Invalid input
				return this;
			}
		}
		
	}
	
	ProtectedRegion mRegion;
	Player mNewOwner;
	double mAmount;
	
	public ClaimConfirmation(ProtectedRegion region, Player newOwner, double amount, InteractableSignState sign)
	{
		mRegion = region;
		mNewOwner = newOwner;
		mAmount = amount;
		
		Map<Object, Object> sessionData = new HashMap<Object, Object>();
		sessionData.put("r", region);
		sessionData.put("new", newOwner);
		sessionData.put("amt", amount);
		sessionData.put("sign", sign);
		sessionData.put("fail", false);
		
		ConversationFactory factory = new ConversationFactory(RegionSigns.instance)
			.withTimeout(20)
			.withModality(false)
			.withInitialSessionData(sessionData)
			.withLocalEcho(false)
			.addConversationAbandonedListener(this)
			.withFirstPrompt(new ConfirmationPrompt());
		
		factory.buildConversation(newOwner).begin();
	}

	@Override
	public void conversationAbandoned(ConversationAbandonedEvent event) 
	{
		if(event.getContext().getSessionData("end") == null)
		{
			mNewOwner.sendMessage(ChatColor.RED + "You did not answer in 20 seconds.\n" + ChatColor.WHITE + "Please click the sign again if you still wish to claim " + ChatColor.YELLOW + ((ProtectedRegion)event.getContext().getSessionData("r")).getId());
		}
		else
		{
			if(!(Boolean)event.getContext().getSessionData("end"))
			{
				mNewOwner.sendMessage(ChatColor.WHITE + "You have decided not to claim " + ChatColor.YELLOW + ((ProtectedRegion)event.getContext().getSessionData("r")).getId());
			}
		}
	}
}
