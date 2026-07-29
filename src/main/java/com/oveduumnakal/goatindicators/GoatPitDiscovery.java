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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.events.VarbitChanged;

/**
 * Writes goat-pit discovery data to the client log so the real count and spikes varbits can be read
 * straight from {@code ~/.runelite/logs/client.log} instead of hunting through Dev Tools.
 *
 * <p>Everything here is gated on {@link GoatIndicatorsConfig#debugLogging()} and does nothing when it
 * is off. Rather than wait for the pit to fire a scene spawn (its runtime id can be a multiloc
 * impostor that never matches), {@link #dumpDefinitions} reads each id in {@link GoatIds#PIT_OBJECT_IDS}
 * straight from the object cache and logs its declared varbit, var-player, actions and impostor ids —
 * that declared varbit is the count source the plugin uses. It then logs varbit changes, marking any
 * that hit the pit's declared varbit/var-player and otherwise keeping only values in the goat-count
 * range so the per-tick game clocks drop out. All output is prefixed {@code [goat-discovery]}.
 */
@Slf4j
@Singleton
class GoatPitDiscovery
{
	private static final String PREFIX = "[goat-discovery]";

	/** Values above this are never a goat count, so out-of-range varbit changes are filtered as noise. */
	private static final int MAX_PLAUSIBLE_COUNT = 25;

	/** Var-players that tick every game cycle regardless of the pit; logging them is pure noise. */
	private static final Set<Integer> NOISE_VARPS = noiseVarps();

	@Inject
	private Client client;

	@Inject
	private GoatIndicatorsConfig config;

	private final Set<Integer> pitVarbits = new HashSet<>();

	private final Set<Integer> pitVarps = new HashSet<>();

	private boolean dumped;

	/** Clears the per-scene memory so the pit definition is dumped again after a scene reload. */
	void reset()
	{
		dumped = false;
		pitVarbits.clear();
		pitVarps.clear();
	}

	/**
	 * Logs the cached object definition of every configured pit id once per scene: name, declared
	 * varbit and var-player (the count source), menu actions and impostor ids. Safe to call every
	 * event; it runs at most once until {@link #reset()}.
	 */
	void dumpDefinitions()
	{
		if (!config.debugLogging() || dumped)
		{
			return;
		}
		dumped = true;
		for (int id : GoatIds.PIT_OBJECT_IDS)
		{
			ObjectComposition base = client.getObjectDefinition(id);
			if (base == null)
			{
				log.info("{} object def id={} -> not in cache", PREFIX, id);
				continue;
			}
			int varbit = base.getVarbitId();
			int varp = base.getVarPlayerId();
			if (varbit >= 0)
			{
				pitVarbits.add(varbit);
			}
			if (varp >= 0)
			{
				pitVarps.add(varp);
			}
			int[] impostorIds = base.getImpostorIds();
			log.info(
				"{} object def: id={} name='{}' declaredVarbit={} declaredVarPlayer={} actions={} impostorIds={}",
				PREFIX, id, base.getName(), varbit, varp, Arrays.toString(base.getActions()),
				Arrays.toString(impostorIds));
			if (impostorIds != null)
			{
				ObjectComposition active = base.getImpostor();
				String name = active == null ? null : active.getName();
				String[] actions = active == null ? null : active.getActions();
				log.info(
					"{} object def id={} active impostor: name='{}' actions={}",
					PREFIX, id, name, Arrays.toString(actions));
			}
		}
	}

	/**
	 * Logs a varbit change worth seeing: any change to the pit's declared varbit/var-player, or any
	 * other change whose value is in the plausible goat-count range. Per-tick clocks are dropped.
	 *
	 * @param event the varbit change reported by the client
	 */
	void onVarbitChanged(VarbitChanged event)
	{
		if (!config.debugLogging())
		{
			return;
		}
		dumpDefinitions();
		int varbit = event.getVarbitId();
		int varp = event.getVarpId();
		int value = event.getValue();
		if (pitVarbits.contains(varbit) || pitVarps.contains(varp))
		{
			log.info(
				"{} PIT VARBIT change: varbitId={} varpId={} value={}",
				PREFIX, varbit, varp, value);
			return;
		}
		if (NOISE_VARPS.contains(varp) || value < 0 || value > MAX_PLAUSIBLE_COUNT)
		{
			return;
		}
		log.info(
			"{} candidate varbit change: varbitId={} varpId={} value={}",
			PREFIX, varbit, varp, value);
	}

	private static Set<Integer> noiseVarps()
	{
		Set<Integer> varps = new HashSet<>();
		varps.add(3077);
		varps.add(3079);
		return varps;
	}
}
