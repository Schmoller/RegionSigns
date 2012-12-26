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
	protected InteractableSign(JavaPlugin plugin, String typeName, String createPermission, String usePermission)
	{
		mPlugin = plugin;
		mSignTypeName = typeName;
		mCreatePermission = createPermission;
		mUsePermission = usePermission;
		mEvents = new BukkitEventHandler();
	}
	
	// Called when a sign is clicked 
	protected abstract void onClick(Player player, InteractableSignState instance, Block block);
	
	protected abstract void validateState(InteractableSignState state, Player player) throws Exception;

	// Allows you to alter the text on the sign afterwards
	protected abstract void replaceInvalidSign(InteractableSignState instance, String[] lines);
	
	protected abstract void onSignCreated(InteractableSignState state);
	
	protected abstract void onSignDestroyed(InteractableSignState state);
	
	protected abstract InteractableSignState getNewState();

	// This is what goes in the "[Type]"
	protected String mSignTypeName;
	
	private String mCreatePermission;
	private String mUsePermission;
	@SuppressWarnings("unused")
	private BukkitEventHandler mEvents;
	protected JavaPlugin mPlugin;
	
	
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
			
			InteractableSignState state = getNewState();
			
			try
			{
				state.load(event.getBlock().getLocation(), event.getLines());
				
				validateState(state, event.getPlayer());
			}
			catch(Exception e)
			{
				event.getPlayer().sendMessage(ChatColor.RED + e.getMessage());
				
				String[] lines = event.getLines();
				
				// Invalid
				lines[0] = ChatColor.DARK_RED + "[" + mSignTypeName + "]";
				
				replaceInvalidSign(state, lines);
				
				for(int i = 0; i < 4; i++)
					event.setLine(i, lines[i]);
				
				return;
			}

			// Reformat sign
			event.setLine(0, ChatColor.DARK_BLUE + "[" + mSignTypeName + "]");
			
			String[] newLines = state.getValidSignText();
			for(int i = 0; i < 3; ++i)
				event.setLine(i+1,newLines[i]);
						
			onSignCreated(state);
		}
	}
	private boolean processBlockBreak(BlockBreakEvent event)
	{
		Sign sign = null;
		
		if(event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN)
		{
			// Check the first line
			if(((Sign)event.getBlock().getState()).getLines()[0].compareToIgnoreCase(ChatColor.DARK_BLUE + "[" + mSignTypeName + "]") == 0)
			{
				// It is one
				// They can create the signs, then they can destroy them
				if(event.getPlayer().hasPermission(mCreatePermission) || mCreatePermission.isEmpty())
					sign = (Sign)event.getBlock().getState();
				else
				{
					event.setCancelled(true);
					return true;
				}
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
					sign = (Sign)block.getState();
					
					// Check the first line
					if(sign.getLines()[0].compareToIgnoreCase(ChatColor.DARK_BLUE + "[" + mSignTypeName + "]") == 0)
					{
						// It is one
						
						// They can create the signs, then they can destroy them
						if(!event.getPlayer().hasPermission(mCreatePermission) && !mCreatePermission.isEmpty())
						{
							event.setCancelled(true);
							return true;
						}
					}
				}
			}
		}
		
		// Build a state
		InteractableSignState state = getNewState();
		
		try
		{
			state.load(sign.getLocation(), sign.getLines());
		}
		catch(Exception e)
		{
			// Not a valid format, ok to destroy
			return false;
		}

		onSignDestroyed(state);
		return false;
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
		

		InteractableSignState state = getNewState();
		
		try
		{
			state.load(clickedBlock.getLocation(), clickedBlock.getLines());
		}
		catch(Exception e)
		{
			// Not a valid format, ok to destroy
			return false;
		}
		
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
