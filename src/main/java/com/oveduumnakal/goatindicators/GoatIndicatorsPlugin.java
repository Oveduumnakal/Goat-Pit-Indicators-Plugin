/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oveduumnakal.goatindicators;

import javax.inject.Inject;

import com.google.inject.Provides;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/** Entry point: keeps the pit tracker fed with scene events and the overlay registered. */
@PluginDescriptor(
	name = "Goat Pit Indicators",
	description = "Show how full the goat pit is, highlights goats you can lure, prevent accidental "
		+ "emptying, track your total catches, and more",
	tags = {"goat", "goats", "pit", "trap", "spikes", "hunter", "overlay", "indicator"}
)
public class GoatIndicatorsPlugin extends Plugin
{
	/** Config key holding the lifetime goats-caught total, so it survives logouts and plugin toggles. */
	private static final String TOTAL_CAUGHT_KEY = "totalCaught";

	/** Config key for the total-caught prefix, so the one-time animated migration can read and rewrite it. */
	private static final String TOTAL_PREFIX_KEY = "totalPrefix";

	/**
	 * Config key flagging that the one-time "Icon becomes Animated" migration has run, so a user who later
	 * chooses Icon on purpose is left alone.
	 */
	private static final String ANIMATED_MIGRATION_KEY = "animatedMigrationDone";

	/**
	 * Game ticks to wait after login (or plugin start) before seeding the catch counter's baseline from
	 * the live count varbit. Two ticks give the pit's count varbit time to settle after a world hop, so
	 * its value is read once rather than caught mid-reload.
	 */
	private static final int SEED_SETTLE_TICKS = 2;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GoatPitOverlay overlay;

	@Inject
	private GoatHighlightOverlay highlightOverlay;

	@Inject
	private GoatPitTracker tracker;

	@Inject
	private GoatTransitTracker transitTracker;

	@Inject
	private GoatCatchCounter catchCounter;

	@Inject
	private GoatMenuSwapper menuSwapper;

	/**
	 * Ticks left before the catch counter is seeded from the live count varbit, or {@code 0} when no seed
	 * is pending. Set on plugin start and on login; counted down in {@link #onGameTick(GameTick)}.
	 */
	private int seedCountdown;

	@Override
	protected void startUp()
	{
		migrateIconPrefixToAnimated();
		overlayManager.add(overlay);
		overlayManager.add(highlightOverlay);
		catchCounter.restore(loadPersistedTotal());
		scheduleSeed();
	}

	/**
	 * Moves users off the old "Icon" total prefix onto the new "Animated" one, exactly once. Before the
	 * animation existed, Icon was the default, so a saved value of Icon is almost always the old default
	 * rather than a deliberate choice. This runs a single time (guarded by {@link #ANIMATED_MIGRATION_KEY})
	 * and only rewrites an explicit Icon value, so anyone who picks Icon on purpose afterwards keeps it.
	 */
	private void migrateIconPrefixToAnimated()
	{
		Boolean migrated = configManager.getConfiguration(
			GoatIndicatorsConfig.GROUP, ANIMATED_MIGRATION_KEY, boolean.class);
		if (Boolean.TRUE.equals(migrated))
			return;

		String stored = configManager.getConfiguration(GoatIndicatorsConfig.GROUP, TOTAL_PREFIX_KEY);
		if (TotalPrefix.ICON.name().equals(stored))
			configManager.setConfiguration(GoatIndicatorsConfig.GROUP, TOTAL_PREFIX_KEY, TotalPrefix.ANIMATED);

		configManager.setConfiguration(GoatIndicatorsConfig.GROUP, ANIMATED_MIGRATION_KEY, true);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(highlightOverlay);
		tracker.clear();
		transitTracker.clear();
		catchCounter.suspend();
		seedCountdown = 0;
	}

	/**
	 * Arms a deferred seed of the catch counter's baseline. The varbit is read later from
	 * {@link #onGameTick(GameTick)} rather than here, for two reasons: plugin start-up can run off the
	 * client thread, where reading the client throws and leaves the plugin unusable until a restart; and
	 * the instant a world hop finishes the count varbit still holds a stale value, so seeding then makes
	 * the pit's real count arrive as a burst of phantom catches. Waiting a couple of settled ticks and
	 * reading on the game thread avoids both.
	 */
	private void scheduleSeed()
	{
		seedCountdown = SEED_SETTLE_TICKS;
	}

	/**
	 * Advances the in-transit tracker once per tick so the highlight can bridge a lured goat's walk phase.
	 * The NPC the local player is interacting with is passed through so the tracker can attribute a fresh
	 * flight to the player's own grab, keeping other players' lured goats off the two-in-transit cap. Also
	 * runs any pending catch-counter seed once the count varbit has had a couple of ticks to settle; the
	 * counter ignores varbit changes while unseeded, so a world hop's reload does not count.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		transitTracker.onTick(client.getTopLevelWorldView().npcs(), localTargetIndex(), localProdding());
		if (seedCountdown > 0 && --seedCountdown == 0)
			catchCounter.seed(client.getVarbitValue(GoatIds.COUNT_VARBIT_OVERRIDE));
	}

	/** Whether the local player is playing the Cattleprod animation this tick, marking a prod of a goat. */
	private boolean localProdding()
	{
		Player localPlayer = client.getLocalPlayer();
		return localPlayer != null && localPlayer.getAnimation() == GoatIds.LOCAL_PROD_ANIM;
	}

	/** The index of the NPC the local player is interacting with, or {@code -1} when it is not an NPC. */
	private int localTargetIndex()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
			return -1;

		Actor interacting = localPlayer.getInteracting();
		if (interacting instanceof NPC)
			return ((NPC) interacting).getIndex();

		return -1;
	}

	/** Reorders the menu after the client sorts it, so a stray click near a full pit or a prod cannot misfire. */
	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		menuSwapper.onPostMenuSort();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		tracker.onSpawn(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		tracker.onDespawn(event.getGameObject());
	}

	/**
	 * Feeds count-varbit changes to the catch counter. Only the pit's count varbit matters; every other
	 * varbit change is ignored.
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != GoatIds.COUNT_VARBIT_OVERRIDE)
			return;

		int before = catchCounter.getTotal();
		catchCounter.onCountChanged(event.getValue());
		if (catchCounter.getTotal() != before)
			configManager.setConfiguration(GoatIndicatorsConfig.GROUP, TOTAL_CAUGHT_KEY, catchCounter.getTotal());
	}

	/** Reads the persisted lifetime total, defaulting to zero when none is stored yet. */
	private int loadPersistedTotal()
	{
		Integer stored = configManager.getConfiguration(GoatIndicatorsConfig.GROUP, TOTAL_CAUGHT_KEY, int.class);
		return stored == null ? 0 : stored;
	}

	/**
	 * Drops every tracked pit when the scene is torn down, and keeps the catch counter's baseline in
	 * step with login state. Object despawn events do not always fire on a world hop, so without the
	 * clear the overlay would keep drawing pits that are no longer there. On login the counter's seed is
	 * scheduled a couple of ticks out (see {@link #scheduleSeed()}) so a pit that is already part-full is
	 * not counted as fresh catches; on teardown it is suspended and any pending seed cancelled, so the
	 * reload does not count either.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			scheduleSeed();
			return;
		}

		if (state == GameState.LOADING || state == GameState.HOPPING || state == GameState.LOGIN_SCREEN)
		{
			tracker.clear();
			transitTracker.clear();
			catchCounter.suspend();
			seedCountdown = 0;
		}
	}

	@Provides
	GoatIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GoatIndicatorsConfig.class);
	}
}
