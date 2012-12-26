package au.com.mineauz.RegionSigns;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

public class Confirmation implements ConversationAbandonedListener
{
	private String mPromptText;
	private String mCancelText;
	private String mTimeoutText;
	
	private Runnable mCallback;
	
	
	public Confirmation()
	{
	}
	
	public Confirmation setPromptText(String text)
	{
		mPromptText = text;
		return this;
	}
	
	public Confirmation setCancelText(String text)
	{
		mCancelText = text;
		return this;
	}
	public Confirmation setTimeoutText(String text)
	{
		mTimeoutText = text;
		return this;
	}
	
	public Confirmation withSuccessCallback(Runnable callback)
	{
		mCallback = callback;
		return this;
	}
	
	public void askPlayer(Player player)
	{
		Map<Object, Object> sessionData = new HashMap<Object, Object>();
		sessionData.put("text", mPromptText);
		sessionData.put("timeout", mTimeoutText);
		sessionData.put("cancel", mCancelText);
		sessionData.put("call", mCallback);
		sessionData.put("failed", false);
		sessionData.put("player", player);
		
		ConversationFactory factory = new ConversationFactory(RegionSigns.instance)
			.withTimeout(20)
			.withModality(false)
			.withInitialSessionData(sessionData)
			.withLocalEcho(false)
			.addConversationAbandonedListener(this)
			.withFirstPrompt(new ConfirmationPrompt());
		
		factory.buildConversation(player).begin();
	}
	
	class ConfirmationPrompt extends StringPrompt
	{
		@Override
		public String getPromptText(ConversationContext context) 
		{
			String promptText = (String)context.getSessionData("text");
			boolean failed = (Boolean)context.getSessionData("failed");
			
			if(failed)
			{
				return "Please enter 'yes' or 'no'";
			}
			else
			{
				return promptText; 
			}
		}

		@Override
		public Prompt acceptInput(ConversationContext context, String input) 
		{
			if(input.compareToIgnoreCase("yes") == 0)
			{
				Runnable callback = (Runnable)context.getSessionData("call");
				
				if(callback != null)
					callback.run();
				
				context.setSessionData("graceful", true);
				context.setSessionData("success", true);
				
				return Prompt.END_OF_CONVERSATION;
			}
			else if (input.compareToIgnoreCase("no") == 0)
			{
				context.setSessionData("graceful", true);
				context.setSessionData("success", false);
				
				return Prompt.END_OF_CONVERSATION;
			}
			else
			{
				context.setSessionData("failed", true);
				// Invalid input
				return this;
			}
		}
	}

	@Override
	public void conversationAbandoned( ConversationAbandonedEvent event )
	{
		Player player = (Player)event.getContext().getSessionData("player");
		
		if(event.getContext().getSessionData("graceful") == null)
			player.sendMessage(ChatColor.RED + "You did not answer in 20 seconds.\n" + ChatColor.WHITE + (String)event.getContext().getSessionData("timeout"));
		else
		{
			if(!(Boolean)event.getContext().getSessionData("success"))
				player.sendMessage((String)event.getContext().getSessionData("cancel"));
		}
	}
	
}
