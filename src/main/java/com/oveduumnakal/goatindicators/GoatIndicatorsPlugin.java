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

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/** Entry point: keeps the pit tracker fed with scene events and the overlay registered. */
@PluginDescriptor(
	name = "Goat Pit Indicators",
	description = "Shows how full a goat pit is, and warns when it needs spikes",
	tags = {"goat", "goats", "pit", "trap", "spikes", "hunter", "overlay", "indicator"}
)
public class GoatIndicatorsPlugin extends Plugin
{
	/** Config key holding the lifetime goats-caught total, so it survives logouts and plugin toggles. */
	private static final String TOTAL_CAUGHT_KEY = "totalCaught";

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

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(highlightOverlay);
		catchCounter.restore(loadPersistedTotal());
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			catchCounter.seed(client.getVarbitValue(GoatIds.COUNT_VARBIT_OVERRIDE));
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(highlightOverlay);
		tracker.clear();
		transitTracker.clear();
		catchCounter.suspend();
	}

	/** Advances the in-transit tracker once per tick so the highlight can bridge a lured goat's walk phase. */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		transitTracker.onTick(client.getTopLevelWorldView().npcs());
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
		{
			return;
		}
		int before = catchCounter.getTotal();
		catchCounter.onCountChanged(event.getValue());
		if (catchCounter.getTotal() != before)
		{
			configManager.setConfiguration(GoatIndicatorsConfig.GROUP, TOTAL_CAUGHT_KEY, catchCounter.getTotal());
		}
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
	 * clear the overlay would keep drawing pits that are no longer there. The counter is seeded from the
	 * live count varbit on login so a pit that is already part-full is not counted as fresh catches, and
	 * suspended on teardown so the reload does not either.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			catchCounter.seed(client.getVarbitValue(GoatIds.COUNT_VARBIT_OVERRIDE));
			return;
		}
		if (state == GameState.LOADING || state == GameState.HOPPING || state == GameState.LOGIN_SCREEN)
		{
			tracker.clear();
			transitTracker.clear();
			catchCounter.suspend();
		}
	}

	@Provides
	GoatIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GoatIndicatorsConfig.class);
	}
}
