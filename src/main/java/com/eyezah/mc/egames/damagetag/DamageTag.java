package com.eyezah.mc.egames.damagetag;

import com.eyezah.mc.egames.Game;
import com.eyezah.mc.egames.GameProfile;
import com.eyezah.mc.egames.eGames;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.eyezah.mc.egames.Game.getAllGames;

public final class DamageTag extends JavaPlugin {
	eGames eGamesManager;
	GameProfile gameProfile;


	@Override
	public void onEnable() {
		eGamesManager = eGames.getInstance();

		try {
			gameProfile = new GameProfile("Damage Tag", "DamageTag", "damagetag <min players>", DamageTagGame.class);
			if (!eGamesManager.registerGameProfile(gameProfile)) {
				getLogger().warning("Couldn't register Damage Tag!");
			} else {
				getLogger().info("Registered Damage Tag!");
			}
		} catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		eGamesManager.unregisterGameProfile(gameProfile);
		List<Game> games = getAllGames();
		for (Game game : games) {
			if (game.getClass() == DamageTagGame.class) {
				game.forceStop();
			}
		}
	}
}
