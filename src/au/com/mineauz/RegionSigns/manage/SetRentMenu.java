package au.com.mineauz.RegionSigns.manage;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import au.com.mineauz.RegionSigns.Util;
import au.com.mineauz.RegionSigns.rent.RentStatus;

public class SetRentMenu extends ValidatingPrompt implements ISubMenu
{
	private Prompt mParent;
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		return "Enter the new rent or back.";
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
		
		double amount;
		if(input.equalsIgnoreCase("free") || input.equalsIgnoreCase("nothing") || input.equalsIgnoreCase("none"))
			amount = 0;
		else
			amount = Util.parseCurrency(input);
		
		RentStatus status = (RentStatus)context.getSessionData("status");
		
		Player player = ((Player)context.getForWhom());
		
		status.IntervalPayment = amount;
		
		player.sendMessage("Rent for region '" + status.Region + "' has been set to " + Util.formatCurrency(amount) + " per " + Util.formatTimeDifference(status.RentInterval, 1, true));
		
		return mParent;
	}

	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		if(input.equalsIgnoreCase("free") || input.equalsIgnoreCase("nothing") || input.equalsIgnoreCase("none"))
			return true;
		
		Double amount = Util.parseCurrency(input);
		
		if(amount.equals(Double.NaN))
			return false;
		
		if(amount < 0)
			return false;
		
		return true;
	}

}
