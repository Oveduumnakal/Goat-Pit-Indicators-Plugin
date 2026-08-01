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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Finds goat pits in the loaded scene and reads their state.
 *
 * <p>Pits are collected from object spawn events and dropped on despawn or scene reload. State is
 * read on demand rather than cached: a varbit read and a composition lookup are both cheap, and the
 * only caller (the overlay) already runs on the client thread, so caching would add invalidation
 * bugs for no measurable gain.
 *
 * <p>Both the count and the spikes state degrade gracefully. If the pit declares a varbit, that is
 * the count; otherwise goats standing inside the pit footprint are counted instead. If the pit's
 * current composition offers an "Add spikes" action, the pit is unspiked; otherwise it is treated as
 * spiked. See {@code docs/discovery.md} for how to replace these heuristics with confirmed ids.
 */
@Singleton
class GoatPitTracker
{
	private final Map<Long, GameObject> pits = new LinkedHashMap<>();

	@Inject
	private Client client;

	/** Records a newly spawned object if it is a goat pit. */
	void onSpawn(GameObject object)
	{
		if (isPit(object))
			pits.put(object.getHash(), object);
	}

	/** Drops an object that has left the scene. */
	void onDespawn(GameObject object)
	{
		pits.remove(object.getHash());
	}

	/** Forgets every tracked pit, for a world hop or a scene reload. */
	void clear()
	{
		pits.clear();
	}

	/** Every goat pit currently loaded in the scene. */
	Collection<GameObject> getPits()
	{
		return Collections.unmodifiableCollection(pits.values());
	}

	/**
	 * Whether an object is a pit the tracker is currently holding.
	 *
	 * @param object any spawned object
	 * @return true once {@link #onSpawn(GameObject)} has accepted it as a pit
	 */
	boolean isPitTracked(GameObject object)
	{
		return object != null && pits.containsKey(object.getHash());
	}

	/**
	 * Reads the current state of a tracked pit.
	 *
	 * @param pit a pit previously handed to {@link #onSpawn(GameObject)}
	 * @return its goat count and spikes state
	 */
	GoatPitState stateOf(GameObject pit)
	{
		return new GoatPitState(readCount(pit), readSpiked(pit), readCapacity());
	}

	/**
	 * The pit's capacity for the player's current Hunter level. Uses the real (unboosted) level, since
	 * the pit's size is fixed by trained level, not by temporary boosts.
	 */
	private int readCapacity()
	{
		return GoatIds.capacityForHunterLevel(client.getRealSkillLevel(Skill.HUNTER));
	}

	private boolean isPit(GameObject object)
	{
		if (object == null)
			return false;

		if (!GoatIds.PIT_OBJECT_IDS.isEmpty())
			return GoatIds.PIT_OBJECT_IDS.contains(object.getId());

		ObjectComposition base = client.getObjectDefinition(object.getId());
		if (base == null)
			return false;

		return matchesPitName(base.getName());
	}

	/** Whether an object name identifies a goat pit. Package-private so it can be tested directly. */
	static boolean matchesPitName(String name)
	{
		return name != null && name.toLowerCase(Locale.ROOT).contains(GoatIds.PIT_NAME_FRAGMENT);
	}

	/** Whether an NPC name identifies a goat. Package-private so it can be tested directly. */
	static boolean matchesGoatName(String name)
	{
		return name != null && name.toLowerCase(Locale.ROOT).contains(GoatIds.GOAT_NAME_FRAGMENT);
	}

	/**
	 * Reads the spikes state from a pit's current menu actions: a pit that still offers "Add spikes"
	 * has none set. A pit with no actions at all is assumed spiked, so a missing composition never
	 * produces a false "Add Spikes" prompt.
	 */
	static boolean spikedFromActions(String[] actions)
	{
		if (actions == null)
			return true;

		for (String action : actions)
		{
			if (action != null && action.toLowerCase(Locale.ROOT).contains(GoatIds.ADD_SPIKES_ACTION))
				return false;
		}
		return true;
	}

	private int readCount(GameObject pit)
	{
		int varbit = GoatIds.COUNT_VARBIT_OVERRIDE;
		if (varbit < 0)
		{
			ObjectComposition base = client.getObjectDefinition(pit.getId());
			varbit = base == null ? -1 : base.getVarbitId();
		}
		if (varbit >= 0)
			return client.getVarbitValue(varbit);

		return countGoatsInside(pit);
	}

	private boolean readSpiked(GameObject pit)
	{
		if (GoatIds.SPIKES_VARBIT_OVERRIDE >= 0)
			return client.getVarbitValue(GoatIds.SPIKES_VARBIT_OVERRIDE) != 0;

		ObjectComposition active = activeComposition(pit.getId());
		return active == null || spikedFromActions(active.getActions());
	}

	/**
	 * Resolves the composition that reflects the object's <em>current</em> state, following the
	 * multiloc impostor if the object has one.
	 */
	private ObjectComposition activeComposition(int id)
	{
		ObjectComposition base = client.getObjectDefinition(id);
		if (base == null || base.getImpostorIds() == null)
			return base;

		ObjectComposition impostor = base.getImpostor();
		return impostor == null ? base : impostor;
	}

	/**
	 * Fallback count for a pit that declares no varbit: goat NPCs standing on the pit's footprint.
	 * Only accurate while the pit is loaded in the scene.
	 */
	private int countGoatsInside(GameObject pit)
	{
		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		if (min == null || max == null)
			return 0;

		int count = 0;
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (!isGoat(npc))
				continue;

			WorldPoint world = npc.getWorldLocation();
			if (world == null || world.getPlane() != pit.getPlane())
				continue;

			LocalPoint location = npc.getLocalLocation();
			if (location == null)
				continue;

			int x = location.getSceneX();
			int y = location.getSceneY();
			if (x >= min.getX() && x <= max.getX() && y >= min.getY() && y <= max.getY())
				count++;
		}
		return count;
	}

	private boolean isGoat(NPC npc)
	{
		return matchesGoatName(npc.getName());
	}
}
