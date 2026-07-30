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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.NPC;

/**
 * Remembers which goats are committed to a pit, so the highlight can stay quiet across the whole lure and
 * not just its visible frames.
 *
 * <p>A telegrabbed goat passes through three phases: it flies (the {@link GoatIds#IN_TRANSIT_SPOTANIM}
 * spot-anim), then walks to the pit, then jumps in (the {@link GoatIds#IN_TRANSIT_ANIM} animation). During
 * the middle walk the goat carries no distinguishing graphic at all — it looks like any wandering goat — so
 * a per-frame check re-highlights it and invites a wasted cast. This tracker bridges that gap: once a goat
 * has flown it is held in transit until it has jumped in (or despawned, or stalled past
 * {@link #MAX_LURED_TICKS}). Advanced once per game tick from {@link GoatIndicatorsPlugin}.
 */
@Singleton
class GoatTransitTracker
{
	/**
	 * The phase a committed goat is in. All three count as in transit; the distinction only drives when the
	 * goat leaves the set.
	 */
	enum Phase
	{
		/** Airborne, carrying the flight spot-anim. */
		FLIGHT,
		/** Flight over, walking to the pit with no telltale graphic yet. */
		LURED,
		/** Playing the jump-in animation. */
		JUMPING
	}

	/**
	 * How many ticks a goat may sit in {@link Phase#LURED} before it is dropped. The walk to the pit is a
	 * tick or two; this cap only guards against a lure that never lands, so a goat is not suppressed forever.
	 */
	private static final int MAX_LURED_TICKS = 10;

	private final Map<Integer, Phase> phases = new HashMap<>();
	private final Map<Integer, Integer> luredTicks = new HashMap<>();
	private final Set<Integer> present = new HashSet<>();

	/**
	 * Advances every goat's phase by one tick from the live scene. Goats that have despawned since the last
	 * tick are dropped, which also clears a goat the instant it is caught.
	 *
	 * @param npcs the world's current NPCs
	 */
	void onTick(Iterable<? extends NPC> npcs)
	{
		present.clear();
		for (NPC npc : npcs)
		{
			if (npc == null || !GoatPitTracker.matchesGoatName(npc.getName()))
			{
				continue;
			}
			int index = npc.getIndex();
			present.add(index);
			boolean flying = npc.hasSpotAnim(GoatIds.IN_TRANSIT_SPOTANIM);
			boolean jumping = npc.getAnimation() == GoatIds.IN_TRANSIT_ANIM;
			advance(index, flying, jumping);
		}
		phases.keySet().retainAll(present);
		luredTicks.keySet().retainAll(present);
	}

	/** Applies one tick's phase transition for a single goat, tracking how long it has been walking. */
	private void advance(int index, boolean flying, boolean jumping)
	{
		Phase prev = phases.get(index);
		Phase next = nextPhase(prev, flying, jumping);
		if (next == Phase.LURED)
		{
			int age = (prev == Phase.LURED ? luredTicks.getOrDefault(index, 0) : 0) + 1;
			if (age > MAX_LURED_TICKS)
			{
				phases.remove(index);
				luredTicks.remove(index);
				return;
			}
			luredTicks.put(index, age);
			phases.put(index, next);
			return;
		}
		luredTicks.remove(index);
		if (next == null)
		{
			phases.remove(index);
			return;
		}
		phases.put(index, next);
	}

	/**
	 * The phase a goat moves to this tick, or {@code null} when it is not (or no longer) in transit. Flight
	 * and jump are read straight from the goat; the walk in between is inferred from having just flown.
	 *
	 * @param prev    the goat's phase last tick, or {@code null} if it was not tracked
	 * @param flying  whether the goat carries the flight spot-anim now
	 * @param jumping whether the goat plays the jump-in animation now
	 * @return the goat's phase this tick, or {@code null} to stop tracking it
	 */
	static Phase nextPhase(Phase prev, boolean flying, boolean jumping)
	{
		if (flying)
		{
			return Phase.FLIGHT;
		}
		if (jumping)
		{
			return Phase.JUMPING;
		}
		if (prev == Phase.FLIGHT || prev == Phase.LURED)
		{
			return Phase.LURED;
		}
		return null;
	}

	/** Whether this goat is mid-lure and so should not be highlighted or offered as a fresh target. */
	boolean isInTransit(int npcIndex)
	{
		return phases.containsKey(npcIndex);
	}

	/** How many goats are currently committed to a pit, for the two-in-transit cap. */
	int inTransitCount()
	{
		return phases.size();
	}

	/** Forgets every tracked goat, for when the scene is torn down. */
	void clear()
	{
		phases.clear();
		luredTicks.clear();
		present.clear();
	}
}
