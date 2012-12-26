package au.com.mineauz.RegionSigns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

// A sign that has a specific layout that performs an action when clicked
public abstract class InteractableSign
{
	protected enum ArgumentTypes
	{
		Integer,
		String,
		Currency,
		None
	}
	protected InteractableSign(JavaPlugin plugin, String typeName, String createPermission, String usePermission)
	{
		mPlugin = plugin;
		mSignTypeName = typeName;
		mCreatePermission = createPermission;
		mUsePermission = usePermission;
		
		mEvents = new BukkitEventHandler();
	}
	
	public void SetArgument1(ArgumentTypes type, boolean required, Object defaultValue)
	{
		Argument1Type = type;
		Argument1Required = required;
		Argument1Default = defaultValue;
	}
	public void SetArgument2(ArgumentTypes type, boolean required, Object defaultValue)
	{
		Argument2Type = type;
		Argument2Required = required;
		Argument2Default = defaultValue;
	}
	public void SetArgument3(ArgumentTypes type, boolean required, Object defaultValue)
	{
		Argument3Type = type;
		Argument3Required = required;
		Argument3Default = defaultValue;
	}
	
	// Called when a sign is clicked 
	protected abstract void onClick(Player player, InteractableSignState instance, Block block);
	
	protected abstract boolean validateArgument(int argumentId, Object argumentValue, Location location, Player player);

	// Allows you to alter the text on the sign afterwards
	protected abstract void replaceSign(InteractableSignState state, boolean valid, String[] lines);
	
	protected abstract void onSignCreated(InteractableSignState state);

	// This is what goes in the "[Type]"
	protected String mSignTypeName;
	
	// The second line of the sign
	protected ArgumentTypes Argument1Type;
	// Whether argument 1 is required
	protected boolean Argument1Required;
	// The default value of argument 1
	protected Object Argument1Default;
	
	// The third line of the sign
	protected ArgumentTypes Argument2Type;
	// Whether argument 2 is required
	protected boolean Argument2Required;
	// The default value of argument 2
	protected Object Argument2Default;
	
	// The forth line of the sign
	protected ArgumentTypes Argument3Type;
	// Whether argument 3 is required
	protected boolean Argument3Required;
	// The default value of argument 3
	protected Object Argument3Default;
	
	private String mCreatePermission;
	private String mUsePermission;
	@SuppressWarnings("unused")
	private BukkitEventHandler mEvents;
	protected JavaPlugin mPlugin;
	
 	private Object ParseArgument(String line, ArgumentTypes type, boolean required, Object defaultValue)
	{
		if(line.isEmpty())
		{
			if(required)
				// Not valid
				return null;
			else
			{
				// Return a default value
				return defaultValue;
			}
		}

		switch(type)
		{
		case None:
			return 0;
		case Integer:
			try 
			{ 
				return Integer.parseInt(line);
			} 
			catch(NumberFormatException e){	}
			break;
		case Currency:
			if(line.equalsIgnoreCase("free"))
				return 0.0;
			// There is one char at the beginning that is the currency symbol
			if(line.startsWith(Util.sCurrencyChar))
			{
				try 
				{ 
					double val = Double.parseDouble(line.substring(1));
					if(val >= 0)
						return val;
				} 
				catch(NumberFormatException e){}
			}
			else
			{
				// Maybe its just the number
				try 
				{ 
					double val = Double.parseDouble(line);
					if(val >= 0)
						return val;
				} 
				catch(NumberFormatException e){}
			}
			break;
		case String:
			return line;
		}
		
		// Not valid
		return null;
	}
	
	private void processSignChanged(SignChangeEvent event)
	{
		// If they use colour, clear the whole sign if they dont have permission
		if(event.getLine(0).equalsIgnoreCase(ChatColor.DARK_BLUE + "[" + mSignTypeName + "]") && (!event.getPlayer().hasPermission(mCreatePermission) && !mCreatePermission.isEmpty()))
		{
			event.setCancelled(true);
			return;
		}
		
		// This player must be able to create the signs
		if(ChatColor.stripColor(event.getLine(0)).compareToIgnoreCase("[" + mSignTypeName + "]") == 0)
		{
			// Prevent them from changing the sign then
			if(!event.getPlayer().hasPermission(mCreatePermission) && !mCreatePermission.isEmpty())
				return;
			
			InteractableSignState state = new InteractableSignState();
			
			state.SignLocation = event.getBlock().getLocation();
			state.Argument1 = ParseArgument(event.getLine(1),Argument1Type,Argument1Required,Argument1Default);
			state.Argument2 = ParseArgument(event.getLine(2),Argument2Type,Argument2Required,Argument2Default);
			state.Argument3 = ParseArgument(event.getLine(3),Argument3Type,Argument3Required,Argument3Default);
			
			boolean valid = true;
			// Check for validity
			if(Argument1Type != ArgumentTypes.None)
			{
				if(state.Argument1 == null)
					valid = false;
				else if(!validateArgument(1,state.Argument1,event.getBlock().getLocation(),event.getPlayer()))
					valid = false;
			}
			if(Argument2Type != ArgumentTypes.None)
			{
				if(state.Argument2 == null)
					valid = false;
				else if(!validateArgument(2,state.Argument2,event.getBlock().getLocation(),event.getPlayer()))
					valid = false;
			}
			if(Argument3Type != ArgumentTypes.None)
			{
				if(state.Argument3 == null)
					valid = false;
				else if(!validateArgument(3,state.Argument3,event.getBlock().getLocation(),event.getPlayer()))
					valid = false;
			}
		
			if(!valid)
			{
				// Invalid
				event.setLine(0, ChatColor.DARK_RED + "[" + mSignTypeName + "]");
				
				String[] lines = event.getLines();
				replaceSign(state, false, lines);
				
				for(int i = 0; i < 4; i++)
					event.setLine(i,lines[i]);
			}
			else
			{
				// Reformat sign
				event.setLine(0, ChatColor.DARK_BLUE + "[" + mSignTypeName + "]");
				
				if(Argument1Type == ArgumentTypes.Currency)
				{
					if((Double)state.Argument1 == 0)
						event.setLine(1,"Free");
					else
						event.setLine(1,Util.formatCurrency((Double)state.Argument1));
				}
					
				if(Argument2Type == ArgumentTypes.Currency)
				{
					if((Double)state.Argument2 == 0)
						event.setLine(2,"Free");
					else
						event.setLine(2,Util.formatCurrency((Double)state.Argument2));
				}
				if(Argument3Type == ArgumentTypes.Currency)
				{
					if((Double)state.Argument3 == 0)
						event.setLine(3,"Free");
					else
						event.setLine(3,Util.formatCurrency((Double)state.Argument3));
				}
				
				String[] lines = event.getLines();
				replaceSign(state, true, lines);
				
				for(int i = 0; i < 4; i++)
					event.setLine(i,lines[i]);
				
				onSignCreated(state);
			}
		}
	}
	private boolean processBlockBreak(BlockBreakEvent event)
	{
		// They can create the signs, then they can destroy them
		if(event.getPlayer().hasPermission(mCreatePermission) || mCreatePermission.isEmpty())
			return false;
		
		if(event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN)
		{
			// Check the first line
			if(((Sign)event.getBlock().getState()).getLines()[0].compareToIgnoreCase(ChatColor.DARK_BLUE + "[" + mSignTypeName + "]") == 0)
			{
				// It is one
				event.setCancelled(true);
				return true;
			}
		}
		else
		{
			// Protect signs that are depending on this block
			for(BlockFace face : BlockFace.values())
			{
				Block block = event.getBlock().getRelative(face);
				if((block.getType() == Material.WALL_SIGN && block.getData() == (Util.BlockFaceToNotch(face) ^ 1)) || (block.getType() == Material.SIGN_POST && face == BlockFace.UP))
				{
					// Check the first line
					if(((Sign)block.getState()).getLines()[0].compareToIgnoreCase(ChatColor.DARK_BLUE + "[" + mSignTypeName + "]") == 0)
					{
						// It is one
						event.setCancelled(true);
						return true;
					}
				}
			}
		}
		
		return false; // Not handled
	}
	
	private boolean processPlayerInteract(PlayerInteractEvent event)
	{
		if(!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return false;
		
		if(!event.getPlayer().hasPermission(mUsePermission) && !mUsePermission.isEmpty())
			// Does not have permission to use the sign
			return false;
		
		if(event.getClickedBlock().getType() != Material.SIGN_POST && event.getClickedBlock().getType() != Material.WALL_SIGN)
			// Not a sign
			return false;
		
		Sign clickedBlock = (Sign)event.getClickedBlock().getState();

		if(!clickedBlock.getLines()[0].equalsIgnoreCase(ChatColor.DARK_BLUE + "[" + mSignTypeName + "]"))
			// Not the right type
			return false;
		

		// Build a state
		InteractableSignState state = new InteractableSignState();
		
		state.SignLocation = clickedBlock.getLocation();
		state.Argument1 = ParseArgument(clickedBlock.getLines()[1],Argument1Type,Argument1Required,Argument1Default);
		state.Argument2 = ParseArgument(clickedBlock.getLines()[2],Argument2Type,Argument2Required,Argument2Default);
		state.Argument3 = ParseArgument(clickedBlock.getLines()[3],Argument3Type,Argument3Required,Argument3Default);
		
		onClick(event.getPlayer(), state, event.getClickedBlock());
		event.setCancelled(true);
		
		return true;
	}
	
	private class BukkitEventHandler implements Listener
	{
		public BukkitEventHandler()
		{
			Bukkit.getPluginManager().registerEvents(this, RegionSigns.instance);
		}
		
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		private void onSignChange(SignChangeEvent event)
		{
			processSignChanged(event);
		}
		
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		private void onBlockBreak(BlockBreakEvent event)
		{
			processBlockBreak(event);
		}
		
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		private void onPlayerInteract(PlayerInteractEvent event)
		{
			processPlayerInteract(event);
		}
		
	}
}
