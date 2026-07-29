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
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.VarbitChanged;

/**
 * Writes goat-pit discovery data to the client log so the real object ids and count/spikes varbits
 * can be read straight from {@code ~/.runelite/logs/client.log} instead of hunting through Dev Tools.
 *
 * <p>Everything here is gated on {@link GoatIndicatorsConfig#debugLogging()} and does nothing when it
 * is off, so it is inert for normal users. When a pit first loads it logs the pit's id, name,
 * footprint, declared varbit and var-player, impostor ids, current actions and the spikes heuristic
 * result. While any pit is loaded it also logs every varbit change, so adding or removing a goat
 * reveals which varbit holds the count. All output is prefixed {@code [goat-discovery]} for grepping.
 */
@Slf4j
@Singleton
class GoatPitDiscovery
{
	private static final String PREFIX = "[goat-discovery]";

	@Inject
	private Client client;

	@Inject
	private GoatIndicatorsConfig config;

	@Inject
	private GoatPitTracker tracker;

	private final Set<Long> logged = new HashSet<>();

	/** Clears the "already logged" memory so a returning pit is described again after a scene reload. */
	void reset()
	{
		logged.clear();
	}

	/**
	 * Describes a pit the first time it is seen in the current scene.
	 *
	 * @param pit a goat pit the tracker has just accepted
	 */
	void onPitSpawn(GameObject pit)
	{
		if (!config.debugLogging() || pit == null)
		{
			return;
		}
		if (!logged.add(pit.getHash()))
		{
			return;
		}
		ObjectComposition base = client.getObjectDefinition(pit.getId());
		String name = base == null ? "?" : base.getName();
		int declaredVarbit = base == null ? -1 : base.getVarbitId();
		int declaredVarp = base == null ? -1 : base.getVarPlayerId();
		int varbitValue = declaredVarbit < 0 ? -1 : client.getVarbitValue(declaredVarbit);
		int[] impostorIds = base == null ? null : base.getImpostorIds();
		ObjectComposition active = base == null ? null : base.getImpostor();
		String[] actions = active == null ? (base == null ? null : base.getActions()) : active.getActions();
		boolean spiked = GoatPitTracker.spikedFromActions(actions);
		WorldPoint sw = pit.getWorldLocation();
		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		int width = min == null || max == null ? -1 : max.getX() - min.getX() + 1;
		int height = min == null || max == null ? -1 : max.getY() - min.getY() + 1;
		log.info(
			"{} pit spawned: id={} name='{}' worldPoint={} footprint={}x{} declaredVarbit={} (value={}) "
				+ "varPlayer={} impostorIds={} actions={} spikedHeuristic={}",
			PREFIX, pit.getId(), name, sw, width, height, declaredVarbit, varbitValue, declaredVarp,
			Arrays.toString(impostorIds), Arrays.toString(actions), spiked);
	}

	/**
	 * Logs a varbit change while a pit is loaded, so a goat added or removed points straight at the
	 * varbit that moved. Silent when no pit is in the scene, to keep the firehose down.
	 *
	 * @param event the varbit change reported by the client
	 */
	void onVarbitChanged(VarbitChanged event)
	{
		if (!config.debugLogging() || tracker.getPits().isEmpty())
		{
			return;
		}
		log.info(
			"{} varbit change while pit loaded: varbitId={} varpId={} value={}",
			PREFIX, event.getVarbitId(), event.getVarpId(), event.getValue());
	}
}
