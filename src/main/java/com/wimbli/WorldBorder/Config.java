package com.wimbli.WorldBorder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;


public class Config
{
	// private stuff used within this class
	private static WorldBorder plugin;
	private static FileConfiguration cfg = null;
	private static final Logger mcLog = Logger.getLogger("Minecraft");
	public static DecimalFormat coord = new DecimalFormat("0.0");
	private static int borderTask = -1;
	public static WorldFillTask fillTask;
	public static WorldTrimTask trimTask;
	private static Runtime rt = Runtime.getRuntime();

	// actual configuration values which can be changed
	private static boolean shapeRound = true;
	private static Map<String, BorderData> borders = Collections.synchronizedMap(new LinkedHashMap<String, BorderData>());
	private static Set<String> bypassPlayers = Collections.synchronizedSet(new LinkedHashSet<String>());
	private static String message;		// raw message without color code formatting
	private static String messageFmt;	// message with color code formatting ("&" changed to funky sort-of-double-dollar-sign for legitimate color/formatting codes)
	private static String messageClean;	// message cleaned of formatting codes
	private static boolean DEBUG = false;
	private static double knockBack = 3.0;
	private static int timerTicks = 4;
	private static boolean whooshEffect = false;
	private static boolean portalRedirection = true;
	private static boolean dynmapEnable = true;
	private static String dynmapMessage;
	private static int remountDelayTicks = 0;
	private static boolean killPlayer = false;
	private static boolean denyEnderpearl = false;
	private static int fillAutosaveFrequency = 30;

	// for monitoring plugin efficiency
//	public static long timeUsed = 0;

	public static long Now()
	{
		return System.currentTimeMillis();
	}


	public static void setBorder(String world, BorderData border)
	{
		borders.put(world, border);
		Log("Border set. " + BorderDescription(world));
		save(true);
		DynMapFeatures.showBorder(world, border);
	}

	public static void setBorder(String world, int radiusX, int radiusZ, double x, double z, Boolean shapeRound)
	{
		BorderData old = Border(world);
		boolean oldWrap = (old == null) ? false : old.getWrapping();
		setBorder(world, new BorderData(x, z, radiusX, radiusZ, shapeRound, oldWrap));
	}
	public static void setBorder(String world, int radiusX, int radiusZ, double x, double z)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		boolean oldWrap = (old == null) ? false : old.getWrapping();
		setBorder(world, new BorderData(x, z, radiusX, radiusZ, oldShape, oldWrap));
	}


	// backwards-compatible methods from before elliptical/rectangular shapes were supported
	public static void setBorder(String world, int radius, double x, double z, Boolean shapeRound)
	{
		setBorder(world, new BorderData(x, z, radius, radius, shapeRound));
	}
	public static void setBorder(String world, int radius, double x, double z)
	{
		setBorder(world, radius, radius, x, z);
	}


	// set border based on corner coordinates
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound, boolean wrap)
	{
		double radiusX = Math.abs(x1 - x2) / 2;
		double radiusZ = Math.abs(z1 - z2) / 2;
		double x = ((x1 < x2) ? x1 : x2) + radiusX;
		double z = ((z1 < z2) ? z1 : z2) + radiusZ;
		setBorder(world, new BorderData(x, z, (int)Math.round(radiusX), (int)Math.round(radiusZ), shapeRound, wrap));
	}
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound)
	{
		setBorderCorners(world, x1, z1, x2, z2, shapeRound, false);
	}
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		boolean oldWrap = (old == null) ? false : old.getWrapping();
		setBorderCorners(world, x1, z1, x2, z2, oldShape, oldWrap);
	}


	public static void removeBorder(String world)
	{
		borders.remove(world);
		Log("Removed border for world \"" + world + "\".");
		save(true);
		DynMapFeatures.removeBorder(world);
	}

	public static void removeAllBorders()
	{
		borders.clear();
		Log("Removed all borders for all worlds.");
		save(true);
		DynMapFeatures.removeAllBorders();
	}

	public static String BorderDescription(String world)
	{
		BorderData border = borders.get(world);
		if (border == null)
			return "No border was found for the world \"" + world + "\".";
		else
			return "World \"" + world + "\" has border " + border.toString();
	}

	public static Set<String> BorderDescriptions()
	{
		Set<String> output = new HashSet<String>();

		Iterator world = borders.keySet().iterator();
		while(world.hasNext())
		{
			output.add( BorderDescription((String)world.next()) );
		}

		return output;
	}

	public static BorderData Border(String world)
	{
		return borders.get(world);
	}

	public static Map<String, BorderData> getBorders()
	{
		return new LinkedHashMap<String, BorderData>(borders);
	}

	public static void setMessage(String msg)
	{
		updateMessage(msg);
		Log("Border message is now set to: " + MessageRaw());
		save(true);
	}

	public static void updateMessage(String msg)
	{
		message = msg;
		messageFmt = replaceAmpColors(msg);
		messageClean = stripAmpColors(msg);
	}

	public static String Message()
	{
		return messageFmt;
	}
	public static String MessageRaw()
	{
		return message;
	}
	public static String MessageClean()
	{
		return messageClean;
	}

	public static void setShape(boolean round)
	{
		shapeRound = round;
		Log("Set default border shape to " + (ShapeName()) + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean ShapeRound()
	{
		return shapeRound;
	}

	public static String ShapeName()
	{
		return ShapeName(shapeRound);
	}
	public static String ShapeName(boolean round)
	{
		return round ? "elliptic/round" : "rectangular/square";
	}

	public static void setDebug(boolean debugMode)
	{
		DEBUG = debugMode;
		Log("Debug mode " + (DEBUG ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean Debug()
	{
		return DEBUG;
	}

	public static void setWhooshEffect(boolean enable)
	{
		whooshEffect = enable;
		Log("\"Whoosh\" knockback effect " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean whooshEffect()
	{
		return whooshEffect;
	}
	public static boolean getIfPlayerKill(){
		return killPlayer;
	}
	public static boolean getDenyEnderpearl()
	{
		return denyEnderpearl;
	}
	public static void showWhooshEffect(Location loc)
	{
		if (!whooshEffect())
			return;

		World world = loc.getWorld();
		world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
		world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
		world.playEffect(loc, Effect.SMOKE, 4);
		world.playEffect(loc, Effect.SMOKE, 4);
		world.playEffect(loc, Effect.SMOKE, 4);
		world.playEffect(loc, Effect.GHAST_SHOOT, 0);
	}

	public static void setPortalRedirection(boolean enable)
	{
		portalRedirection = enable;
		Log("Portal redirection " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean portalRedirection()
	{
		return portalRedirection;
	}

	public static void setKnockBack(double numBlocks)
	{
		knockBack = numBlocks;
		Log("Knockback set to " + knockBack + " blocks inside the border.");
		save(true);
	}

	public static double KnockBack()
	{
		return knockBack;
	}

	public static void setTimerTicks(int ticks)
	{
		timerTicks = ticks;
		Log("Timer delay set to " + timerTicks + " tick(s). That is roughly " + (timerTicks * 50) + "ms / " + (((double)timerTicks * 50.0) / 1000.0) + " seconds.");
		StartBorderTimer();
		save(true);
	}

	public static int TimerTicks()
	{
		return timerTicks;
	}

	public static void setRemountTicks(int ticks)
	{
		remountDelayTicks = ticks;
		if (remountDelayTicks == 0)
			Log("Remount delay set to 0. Players will be left dismounted when knocked back from the border while on a vehicle.");
		else
			Log("Remount delay set to " + remountDelayTicks + " tick(s). That is roughly " + (remountDelayTicks * 50) + "ms / " + (((double)remountDelayTicks * 50.0) / 1000.0) + " seconds.");
		if (ticks < 10)
			LogWarn("setting the remount delay to less than 10 (and greater than 0) is not recommended. This can lead to nasty client glitches.");
		save(true);
	}

	public static int RemountTicks()
	{
		return remountDelayTicks;
	}

	public static void setFillAutosaveFrequency(int seconds)
	{
		fillAutosaveFrequency = seconds;
		if (fillAutosaveFrequency == 0)
			Log("World autosave frequency during Fill process set to 0, disabling it. Note that much progress can be lost this way if there is a bug or crash in the world generation process from Bukkit or any world generation plugin you use.");
		else
			Log("World autosave frequency during Fill process set to " + fillAutosaveFrequency + " seconds (rounded to a multiple of 5). New chunks generated by the Fill process will be forcibly saved to disk this often to prevent loss of progress due to bugs or crashes in the world generation process.");
		save(true);
	}

	public static int FillAutosaveFrequency()
	{
		return fillAutosaveFrequency;
	}


	public static void setDynmapBorderEnabled(boolean enable)
	{
		dynmapEnable = enable;
		Log("DynMap border display is now " + (enable ? "enabled" : "disabled") + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean DynmapBorderEnabled()
	{
		return dynmapEnable;
	}

	public static void setDynmapMessage(String msg)
	{
		dynmapMessage = msg;
		Log("DynMap border label is now set to: " + msg);
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static String DynmapMessage()
	{
		return dynmapMessage;
	}

	public static void setPlayerBypass(String player, boolean bypass)
	{
		if (bypass)
			bypassPlayers.add(player.toLowerCase());
		else
			bypassPlayers.remove(player.toLowerCase());
	}

	public static boolean isPlayerBypassing(String player)
	{
		return bypassPlayers.contains(player.toLowerCase());
	}

	public static void togglePlayerBypass(String player)
	{
		setPlayerBypass(player, !isPlayerBypassing(player));
	}

	public static String getPlayerBypassList()
	{
		if (bypassPlayers.isEmpty())
			return "<none>";
		String newString = bypassPlayers.toString();
		return newString.substring(1, newString.length() - 1);
	}



	public static boolean isBorderTimerRunning()
	{
		if (borderTask == -1) return false;
		return (plugin.getServer().getScheduler().isQueued(borderTask) || plugin.getServer().getScheduler().isCurrentlyRunning(borderTask));
	}

	public static void StartBorderTimer()
	{
		StopBorderTimer();

		borderTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new BorderCheckTask(), timerTicks, timerTicks);

		if (borderTask == -1)
			LogWarn("Failed to start timed border-checking task! This will prevent the plugin from working. Try restarting Bukkit.");

		LogConfig("Border-checking timed task started.");
	}

	public static void StopBorderTimer()
	{
		if (borderTask == -1) return;

		plugin.getServer().getScheduler().cancelTask(borderTask);
		borderTask = -1;
		LogConfig("Border-checking timed task stopped.");
	}

	public static void StopFillTask()
	{
		if (fillTask != null && fillTask.valid())
			fillTask.cancel();
	}

	public static void StoreFillTask()
	{
		save(false, true);
	}
	public static void UnStoreFillTask()
	{
		save(false);
	}

	public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total, boolean forceLoad)
	{
		fillTask = new WorldFillTask(plugin.getServer(), null, world, fillDistance, chunksPerRun, tickFrequency, forceLoad);
		if (fillTask.valid())
		{
			fillTask.continueProgress(x, z, length, total);
			int task = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, fillTask, 20, tickFrequency);
			fillTask.setTaskID(task);
		}
	}
	// for backwards compatibility
	public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total)
	{
		RestoreFillTask(world, fillDistance, chunksPerRun, tickFrequency, x, z, length, total, false);
	}


	public static void StopTrimTask()
	{
		if (trimTask != null && trimTask.valid())
			trimTask.cancel();
	}


	public static int AvailableMemory()
	{
		return (int)((rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576);  // 1024*1024 = 1048576 (bytes in 1 MB)
	}


	public static boolean HasPermission(Player player, String request)
	{
		return HasPermission(player, request, true);
	}
	public static boolean HasPermission(Player player, String request, boolean notify)
	{
		if (player == null)				// console, always permitted
			return true;

		if (player.hasPermission("worldborder." + request))	// built-in Bukkit superperms
			return true;

		if (notify)
			player.sendMessage("You do not have sufficient permissions.");

		return false;
	}


	// adapted from code posted by Sleaker
	public static String replaceAmpColors (String message)
	{
		return message.replaceAll("(?i)&([a-fk-or0-9])", "\u00A7$1");
	}
	public static String stripAmpColors (String message)
	{
		return message.replaceAll("(?i)&([a-fk-or0-9])", "");
	}


	private static final String logName = "WorldBorder";
	public static void Log(Level lvl, String text)
	{
		mcLog.log(lvl, String.format("[%s] %s", logName, text));
	}
	public static void Log(String text)
	{
		Log(Level.INFO, text);
	}
	public static void LogWarn(String text)
	{
		Log(Level.WARNING, text);
	}
	public static void LogConfig(String text)
	{
		Log(Level.INFO, "[CONFIG] " + text);
	}


	private static final int currentCfgVersion = 9;

	public static void load(WorldBorder master, boolean logIt)
	{	// load config from file
		plugin = master;

		plugin.reloadConfig();
		cfg = plugin.getConfig();

		int cfgVersion = cfg.getInt("cfg-version", currentCfgVersion);

		String msg = cfg.getString("message");
		shapeRound = cfg.getBoolean("round-border", true);
		DEBUG = cfg.getBoolean("debug-mode", false);
		whooshEffect = cfg.getBoolean("whoosh-effect", false);
		portalRedirection = cfg.getBoolean("portal-redirection", true);
		knockBack = cfg.getDouble("knock-back-dist", 3.0);
		timerTicks = cfg.getInt("timer-delay-ticks", 5);
		remountDelayTicks = cfg.getInt("remount-delay-ticks", 0);
		dynmapEnable = cfg.getBoolean("dynmap-border-enabled", true);
		dynmapMessage = cfg.getString("dynmap-border-message", "The border of the world.");
		LogConfig("Using " + (ShapeName()) + " border, knockback of " + knockBack + " blocks, and timer delay of " + timerTicks + ".");
		killPlayer = cfg.getBoolean("player-killed-bad-spawn", false);
		denyEnderpearl = cfg.getBoolean("deny-enderpearl", false);
		fillAutosaveFrequency = cfg.getInt("fill-autosave-frequency", 30);
		bypassPlayers = Collections.synchronizedSet(new LinkedHashSet<String>(cfg.getStringList("bypass-list")));

		StartBorderTimer();

		borders.clear();

		// if empty border message, assume no config
		if (msg == null || msg.isEmpty())
		{	// store defaults
			LogConfig("Configuration not present, creating new file.");
			msg = "&cYou have reached the edge of this world.";
			updateMessage(msg);
			save(false);
			return;
		}
		// if loading older config which didn't support color codes in border message, make sure default red color code is added at start of it
		else if (cfgVersion < 8 && !(msg.substring(0, 1).equals("&")))
			updateMessage("&c" + msg);
		// otherwise just set border message
		else
			updateMessage(msg);

		ConfigurationSection worlds = cfg.getConfigurationSection("worlds");
		if (worlds != null)
		{
			Set<String> worldNames = worlds.getKeys(false);

			for(String worldName : worldNames)
			{
				ConfigurationSection bord = worlds.getConfigurationSection(worldName);

				// we're swapping "<" to "." at load since periods denote configuration nodes without a working way to change that, so world names with periods wreak havoc and are thus modified for storage
				if (cfgVersion > 3)
					worldName = worldName.replace("<", ".");

				// backwards compatibility for config from before elliptical/rectangular borders were supported
				if (bord.isSet("radius") && !bord.isSet("radiusX"))
				{
					int radius = bord.getInt("radius");
					bord.set("radiusX", radius);
					bord.set("radiusZ", radius);
				}

				Boolean overrideShape = (Boolean) bord.get("shape-round");
				boolean wrap = (boolean) bord.getBoolean("wrapping", false);
				BorderData border = new BorderData(bord.getDouble("x", 0), bord.getDouble("z", 0), bord.getInt("radiusX", 0), bord.getInt("radiusZ", 0), overrideShape, wrap);
				borders.put(worldName, border);
				LogConfig(BorderDescription(worldName));
			}
		}

		// if we have an unfinished fill task stored from a previous run, load it up
		ConfigurationSection storedFillTask = cfg.getConfigurationSection("fillTask");
		if (storedFillTask != null)
		{
			String worldName = storedFillTask.getString("world");
			int fillDistance = storedFillTask.getInt("fillDistance", 176);
			int chunksPerRun = storedFillTask.getInt("chunksPerRun", 5);
			int tickFrequency = storedFillTask.getInt("tickFrequency", 20);
			int fillX = storedFillTask.getInt("x", 0);
			int fillZ = storedFillTask.getInt("z", 0);
			int fillLength = storedFillTask.getInt("length", 0);
			int fillTotal = storedFillTask.getInt("total", 0);
			boolean forceLoad = storedFillTask.getBoolean("forceLoad", false);
			RestoreFillTask(worldName, fillDistance, chunksPerRun, tickFrequency, fillX, fillZ, fillLength, fillTotal, forceLoad);
			save(false);
		}

		if (logIt)
			LogConfig("Configuration loaded.");

		if (cfgVersion < currentCfgVersion)
			save(false);
	}

	public static void save(boolean logIt)
	{
		save(logIt, false);
	}
	public static void save(boolean logIt, boolean storeFillTask)
	{	// save config to file
		if (cfg == null) return;

		cfg.set("cfg-version", currentCfgVersion);
		cfg.set("message", message);
		cfg.set("round-border", shapeRound);
		cfg.set("debug-mode", DEBUG);
		cfg.set("whoosh-effect", whooshEffect);
		cfg.set("portal-redirection", portalRedirection);
		cfg.set("knock-back-dist", knockBack);
		cfg.set("timer-delay-ticks", timerTicks);
		cfg.set("remount-delay-ticks", remountDelayTicks);
		cfg.set("dynmap-border-enabled", dynmapEnable);
		cfg.set("dynmap-border-message", dynmapMessage);
		cfg.set("player-killed-bad-spawn", killPlayer);
		cfg.set("deny-enderpearl", denyEnderpearl);
		cfg.set("fill-autosave-frequency", fillAutosaveFrequency);
		cfg.set("bypass-list", new ArrayList(bypassPlayers));

		cfg.set("worlds", null);
		Iterator world = borders.entrySet().iterator();
		while(world.hasNext())
		{
			Entry wdata = (Entry)world.next();
			String name = ((String)wdata.getKey()).replace(".", "<");
			BorderData bord = (BorderData)wdata.getValue();

			cfg.set("worlds." + name + ".x", bord.getX());
			cfg.set("worlds." + name + ".z", bord.getZ());
			cfg.set("worlds." + name + ".radiusX", bord.getRadiusX());
			cfg.set("worlds." + name + ".radiusZ", bord.getRadiusZ());
			cfg.set("worlds." + name + ".wrapping", bord.getWrapping());

			if (bord.getShape() != null)
				cfg.set("worlds." + name + ".shape-round", bord.getShape());
		}

		if (storeFillTask && fillTask != null && fillTask.valid())
		{
			cfg.set("fillTask.world", fillTask.refWorld());
			cfg.set("fillTask.fillDistance", fillTask.refFillDistance());
			cfg.set("fillTask.chunksPerRun", fillTask.refChunksPerRun());
			cfg.set("fillTask.tickFrequency", fillTask.refTickFrequency());
			cfg.set("fillTask.x", fillTask.refX());
			cfg.set("fillTask.z", fillTask.refZ());
			cfg.set("fillTask.length", fillTask.refLength());
			cfg.set("fillTask.total", fillTask.refTotal());
			cfg.set("fillTask.forceLoad", fillTask.refForceLoad());
		}
		else
			cfg.set("fillTask", null);

		plugin.saveConfig();

		if (logIt)
			LogConfig("Configuration saved.");
	}
}
