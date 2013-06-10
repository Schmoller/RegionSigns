package au.com.mineauz.RegionSigns.manage;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class FlagSetter extends ValidatingPrompt implements ISubMenu
{
	private Flag<?> mFlag;
	private String mMessage;
	private String mBadMessage;
	private Prompt mParent;
	
	public FlagSetter(Flag<?> flag, String message)
	{
		mFlag = flag;
		mMessage = message;
	}
	
	@Override
	public String getPromptText( ConversationContext context )
	{
		return mMessage;
	}

	@Override
	public void setParent( Prompt parent )
	{
		mParent = parent;
	}

	@Override
	protected Prompt acceptValidatedInput( ConversationContext context, String input )
	{
		ProtectedRegion region = (ProtectedRegion)context.getSessionData("region");
		
		if(input.equalsIgnoreCase("back"))
			return mParent;
		else if(input.equalsIgnoreCase("clear"))
		{
			region.setFlag(mFlag, null);
		}
		else if(mFlag instanceof StateFlag)
		{
			StateFlag flag = (StateFlag)mFlag;
			try
			{
				State state = flag.parseInput(null, (Player)context.getForWhom(), input);
				
				region.setFlag(flag, state);
			}
			catch(InvalidFlagFormat e) {}
		}
		else if(mFlag instanceof StringFlag)
		{
			StringFlag flag = (StringFlag)mFlag;
			
			try
			{
				String text = flag.parseInput(null, (Player)context.getForWhom(), input);
				
				region.setFlag(flag, text);
			}
			catch(InvalidFlagFormat e) {}
		}
		
		return mParent;
	}

	@Override
	protected String getFailedValidationText( ConversationContext context, String invalidInput )
	{
		return mBadMessage;
	}
	
	@Override
	protected boolean isInputValid( ConversationContext context, String input )
	{
		if(input.equalsIgnoreCase("back"))
			return true;
		
		if(mFlag instanceof StateFlag)
		{
			StateFlag flag = (StateFlag)mFlag;
			try
			{
				flag.parseInput(null, (Player)context.getForWhom(), input);
				return true;
			}
			catch(InvalidFlagFormat e)
			{
				mBadMessage = e.getMessage();
			}
		}
		else if(mFlag instanceof StringFlag)
		{
			StringFlag flag = (StringFlag)mFlag;
			
			try
			{
				flag.parseInput(null, (Player)context.getForWhom(), input);
				return true;
			}
			catch(InvalidFlagFormat e)
			{
				mBadMessage = e.getMessage();
			}
		}
		else
			mBadMessage = "Flag type not supported";
		
		return false;
	}

}
