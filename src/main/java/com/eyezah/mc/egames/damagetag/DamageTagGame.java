package com.eyezah.mc.egames.damagetag;

import com.eyezah.mc.egames.Game;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import static org.bukkit.Bukkit.getServer;

public class DamageTagGame extends Game {
	int firstSeconds = 30;
	int roundSeconds = 120;
	int gracePeriod = 3;

	Player it = null;
	Player sender;
	long lastTagged = -1;
	long timerStart;
	int gameTime = firstSeconds;
	long gameOffset = 0;
	Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
	Team teamIt = null;
	Team teamNotIt = null;


	public DamageTagGame(JavaPlugin plugin, World world, String[] args, Player sender) {
		super(world, plugin, "Damage Tag", 2, sender);

		class EventListener implements Listener {
			@EventHandler
			public void onEntityDamage(EntityDamageEvent entityDamageEvent) {
				if (!running) {
					entityDamageEvent.getHandlers().unregister(this);
					return;
				}
				if (!(entityDamageEvent.getEntity() instanceof Player) || entityDamageEvent.getEntity().getWorld() != world) return;
				Player player = (Player) entityDamageEvent.getEntity();
				if (!players.containsKey(player)) return;
				entityDamageEvent.setDamage(0);
				if (player == it) return;
				long timestamp = System.currentTimeMillis();
				if (!isGracePeriod() && gameTime != firstSeconds) {
					it = player;
					if (timestamp - lastTagged > 3000) {
						gameTime += gracePeriod;
					} else {
						gameTime += (int) Math.floor((timestamp - lastTagged) / 1000);
					}
					lastTagged = timestamp;
					if (gameTime > 3600) gameTime = 3600;
					messagePlayer(player, "§c§lTAG", MessageType.TITLE);
					messagePlayers("§a" + player.getName() + " was tagged!", true);
				}
			}
		}
		getServer().getPluginManager().registerEvents(new EventListener(), plugin);
	}

	@Override
	public void playerRemoved(Player player) {
		if (teamIt != null) teamIt.removePlayer(player);
		if (teamNotIt != null) teamNotIt.removePlayer(player);
		Bukkit.getScheduler().runTask(plugin, () -> player.setGlowing(false));
		messagePlayers("§c" + player.getName() + " left " + gameName + "!", true);
		if (player == it) reallocateIt();
	}

	@Override
	public void gameEnded() {
		vLog("Damage Tag game marked as ended.");
		try {
			for (OfflinePlayer player : teamIt.getPlayers()) {
				Bukkit.getScheduler().runTask(plugin, () -> player.getPlayer().setGlowing(false));
			}
			for (OfflinePlayer player : teamNotIt.getPlayers()) {
				Bukkit.getScheduler().runTask(plugin, () -> player.getPlayer().setGlowing(false));
			}
			if (teamIt != null) teamIt.unregister();
			if (teamNotIt != null) teamNotIt.unregister();
		} catch (Exception ignored) {}
	}

	public boolean reallocateIt() {
		it = randomPlayer();
		if (it == null) {
			forceStop();
			return false;
		}
		if (players.size() == 1) {
			for (Player player : players.keySet()) {
				Bukkit.getScheduler().runTask(plugin, () -> player.setGlowing(false));
				teamIt.removePlayer(player);
				teamNotIt.removePlayer(player);
			}
			messagePlayers("§a" + it.getName() + " wins!", true);
			messagePlayers("§a" + it.getName() + " wins!", true, MessageType.ACTIONBAR);
			return false;
		}
		for (Player player : players.keySet()) {
			if (player == it) {
				teamIt.addPlayer(player);
				teamNotIt.removePlayer(player);
			} else {
				teamIt.removePlayer(player);
				teamNotIt.addPlayer(player);
			}
		}
		messagePlayer(it, "§c§lTAG", MessageType.TITLE);
		messagePlayers("§c" + it.getName() + " was tagged!", true);
		lastTagged = System.currentTimeMillis();
		//gameOffset += lastTagged - timerStart;
		gameTime += roundSeconds;
		return true;
	}

	public double getTimeLeft() {
		double time = timerStart - System.currentTimeMillis() + gameOffset + gameTime * 1000L;
		return round(time / 1000, 1);
	}

	@Override
	public void start() {
		if (board.getTeam("egames-" + world.getName() + "-it") != null) board.getTeam(world.getName() + "-it").unregister();
		if (board.getTeam("egames-" + world.getName() + "-notit") != null) board.getTeam(world.getName() + "-notit").unregister();
		teamIt = board.registerNewTeam("egames-" + world.getName() + "-it");
		teamNotIt = board.registerNewTeam("egames-" + world.getName() + "-notit");
		teamIt.setColor(ChatColor.RED);
		teamNotIt.setColor(ChatColor.GREEN);
		messagePlayers("§aDamage Tag has started!", true);
		timerStart = System.currentTimeMillis();
		running	= true;
		//reallocateIt();
		//messagePlayers("§aThe game has started!", true);
		for (Player player : players.keySet()) {
			//Bukkit.getScheduler().runTask(plugin, () -> player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1000000, 1, false, false, false)));
			Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SURVIVAL));
		}
		tick(100);
	}

	public boolean isGracePeriod() {
		return System.currentTimeMillis() < lastTagged + gracePeriod * 1000L;
	}

	@Override
	public boolean tickFunction() {
		if (!running) return false;
		double time = getTimeLeft();
		if (gameTime == firstSeconds) {
			if (time <= 0) {
				if (!reallocateIt()) return false;
				messagePlayers((isGracePeriod() ? "§c" : "§a") + it.getName() + " - " + getTimeLeft(), true, MessageType.ACTIONBAR);
				return true;
			}
			messagePlayers("§aGrace Period - " + time, true, MessageType.ACTIONBAR);
		} else {
			if (time <= 0) {
				makeSpectator(it);
				teamIt.removePlayer(it);
				teamNotIt.removePlayer(it);
				Bukkit.getScheduler().runTask(plugin, () -> it.setGlowing(false));
				messagePlayers("§c" + it.getName() + " was eliminated!", true);
				Player eliminated = it;
				if (!reallocateIt()) return false;
				time = getTimeLeft();
			}
			messagePlayers((isGracePeriod() ? "§c" : "§a") + it.getName() + " - " + time, true, MessageType.ACTIONBAR);

			for (Player player : players.keySet()) {
				Bukkit.getScheduler().runTask(plugin, () -> player.setGlowing(true));
				if (player == it) {
					teamIt.addPlayer(player);
					teamNotIt.removePlayer(player);
				} else {
					teamIt.removePlayer(player);
					teamNotIt.addPlayer(player);
				}
			}
		}
		return true;
	}

	public static double round(double value, int precision) {
		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}
}
