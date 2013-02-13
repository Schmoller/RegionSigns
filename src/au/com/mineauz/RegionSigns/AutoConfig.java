package au.com.mineauz.RegionSigns;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;


public abstract class AutoConfig
{
	private File mFile;
	protected AutoConfig(File file)
	{
		mFile = file;
	}
	protected void onPostLoad() throws InvalidConfigurationException {};
	protected void onPreSave(){};
	public boolean load()
	{
		FileConfiguration yml = new YamlConfiguration();
		try
		{
			// Make sure the file exists
			if(!mFile.exists())
			{
				mFile.getParentFile().mkdirs();
				mFile.createNewFile();
			}
			
			// Parse the config
			yml.load(mFile);
			for(Field field : getClass().getDeclaredFields())
			{
				ConfigField configField = field.getAnnotation(ConfigField.class);
				if(configField == null)
					continue;
				
				String optionName = configField.name();
				if(optionName.isEmpty())
					optionName = field.getName();
				
				field.setAccessible(true);
				
				String path = (configField.category().isEmpty() ? "" : configField.category() + ".") + optionName;
				if(!yml.contains(path))
				{
					if(field.get(this) == null)
						throw new InvalidConfigurationException(path + " is required to be set! Info:\n" + configField.comment());
				}
				else
				{
					// Parse the value
					
					if(field.getType().isArray())
					{
						// Integer
						if(field.getType().getComponentType().equals(Integer.TYPE))
							field.set(this, yml.getIntegerList(path).toArray(new Integer[0]));
						
						// Float
						else if(field.getType().getComponentType().equals(Float.TYPE))
							field.set(this, yml.getFloatList(path).toArray(new Float[0]));
						
						// Double
						else if(field.getType().getComponentType().equals(Double.TYPE))
							field.set(this, yml.getDoubleList(path).toArray(new Double[0]));
						
						// Long
						else if(field.getType().getComponentType().equals(Long.TYPE))
							field.set(this, yml.getLongList(path).toArray(new Long[0]));
						
						// Short
						else if(field.getType().getComponentType().equals(Short.TYPE))
							field.set(this, yml.getShortList(path).toArray(new Short[0]));
						
						// Boolean
						else if(field.getType().getComponentType().equals(Boolean.TYPE))
							field.set(this, yml.getBooleanList(path).toArray(new Boolean[0]));
						
						// String
						else if(field.getType().getComponentType().equals(String.class))
						{
							field.set(this, yml.getStringList(path).toArray(new String[0]));
						}
						else
							throw new IllegalArgumentException("Cannot use type " + field.getType().getSimpleName() + " for AutoConfiguration");
					}
					else
					{
						// Integer
						if(field.getType().equals(Integer.TYPE))
							field.setInt(this, yml.getInt(path));
						
						// Float
						else if(field.getType().equals(Float.TYPE))
							field.setFloat(this, (float)yml.getDouble(path));
						
						// Double
						else if(field.getType().equals(Double.TYPE))
							field.setDouble(this, yml.getDouble(path));
						
						// Long
						else if(field.getType().equals(Long.TYPE))
							field.setLong(this, yml.getLong(path));
						
						// Short
						else if(field.getType().equals(Short.TYPE))
							field.setShort(this, (short)yml.getInt(path));
						
						// Boolean
						else if(field.getType().equals(Boolean.TYPE))
							field.setBoolean(this, yml.getBoolean(path));
						
						// ItemStack
						else if(field.getType().equals(ItemStack.class))
							field.set(this, yml.getItemStack(path));
						
						// String
						else if(field.getType().equals(String.class))
							field.set(this, yml.getString(path));
						else
							throw new IllegalArgumentException("Cannot use type " + field.getType().getSimpleName() + " for AutoConfiguration");
					}
				}
			}
			
			onPostLoad();
			
			return true;
		}
		catch( IOException e )
		{
			e.printStackTrace();
			return false;
		}
		catch ( InvalidConfigurationException e )
		{
			e.printStackTrace();
			return false;
		}
		catch ( IllegalArgumentException e )
		{
			e.printStackTrace();
			return false;
		}
		catch ( IllegalAccessException e )
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean save()
	{
		try
		{
			onPreSave();
			
			YamlConfiguration config = new YamlConfiguration();
			Map<String, String> comments = new HashMap<String, String>();
			
			// Add all the values
			for(Field field : getClass().getDeclaredFields())
			{
				ConfigField configField = field.getAnnotation(ConfigField.class);
				if(configField == null)
					continue;
				
				String optionName = configField.name();
				if(optionName.isEmpty())
					optionName = field.getName();
				
				field.setAccessible(true);
				
				String path = (configField.category().isEmpty() ? "" : configField.category() + ".") + optionName;

				// Ensure the secion exists
				if(!configField.category().isEmpty() && !config.contains(configField.category()))
					config.createSection(configField.category());
				
				if(field.getType().isArray())
				{
					// Integer
					if(field.getType().getComponentType().equals(Integer.TYPE))
						config.set(path, Arrays.asList((Integer[])field.get(this)));
					
					// Float
					else if(field.getType().getComponentType().equals(Float.TYPE))
						config.set(path, Arrays.asList((Float[])field.get(this)));
					
					// Double
					else if(field.getType().getComponentType().equals(Double.TYPE))
						config.set(path, Arrays.asList((Double[])field.get(this)));
					
					// Long
					else if(field.getType().getComponentType().equals(Long.TYPE))
						config.set(path, Arrays.asList((Long[])field.get(this)));
					
					// Short
					else if(field.getType().getComponentType().equals(Short.TYPE))
						config.set(path, Arrays.asList((Short[])field.get(this)));
					
					// Boolean
					else if(field.getType().getComponentType().equals(Boolean.TYPE))
						config.set(path, Arrays.asList((Boolean[])field.get(this)));
					
					// String
					else if(field.getType().getComponentType().equals(String.class))
						config.set(path, Arrays.asList((String[])field.get(this)));
					else
						throw new IllegalArgumentException("Cannot use type " + field.getType().getSimpleName() + " for AutoConfiguration");
				}
				else
				{
					// Integer
					if(field.getType().equals(Integer.TYPE))
						config.set(path, field.get(this));
					
					// Float
					else if(field.getType().equals(Float.TYPE))
						config.set(path, field.get(this));
					
					// Double
					else if(field.getType().equals(Double.TYPE))
						config.set(path, field.get(this));
					
					// Long
					else if(field.getType().equals(Long.TYPE))
						config.set(path, field.get(this));
					
					// Short
					else if(field.getType().equals(Short.TYPE))
						config.set(path, field.get(this));
					
					// Boolean
					else if(field.getType().equals(Boolean.TYPE))
						config.set(path, field.get(this));
					
					// ItemStack
					else if(field.getType().equals(ItemStack.class))
						config.set(path, field.get(this));
					
					// String
					else if(field.getType().equals(String.class))
						config.set(path, field.get(this));
					else
						throw new IllegalArgumentException("Cannot use type " + field.getType().getSimpleName() + " for AutoConfiguration");
				}
				
				// Record the comment
				if(!configField.comment().isEmpty())
					comments.put(path,configField.comment());
			}
			
			String output = config.saveToString();
			
			// Apply comments
			String category = "";
			List<String> lines = new ArrayList<String>(Arrays.asList(output.split("\n")));
			for(int l = 0; l < lines.size(); l++)
			{
				String line = lines.get(l);
				
				if(line.startsWith("#"))
					continue;
				
				if(line.trim().startsWith("-"))
					continue;
				
				if(!line.contains(":"))
					continue;
				
				String path = "";
				line = line.substring(0, line.indexOf(":"));
				
				if(line.startsWith("  "))
					path = category + "." + line.substring(2).trim();
				else
				{
					category = line.trim();
					path = line.trim();
				}
				
				if(comments.containsKey(path))
				{
					String indent = "";
					for(int i = 0; i < line.length(); i++)
					{
						if(line.charAt(i) == ' ')
							indent += " ";
						else
							break;
					}
					
					// Add in the comment lines
					String[] commentLines = comments.get(path).split("\n");
					lines.add(l++, "");
					for(int i = 0; i < commentLines.length; i++)
					{
						commentLines[i] = indent + "# " + commentLines[i];
						lines.add(l++,commentLines[i]);
					}
				}
			}
			output = "";
			for(String line : lines)
				output += line + "\n";
			
			FileWriter writer = new FileWriter(mFile);
			writer.write(output);
			writer.close();
			return true;
		}
		catch ( IllegalArgumentException e )
		{
			e.printStackTrace();
		}
		catch ( IllegalAccessException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		return false;
	}
}
