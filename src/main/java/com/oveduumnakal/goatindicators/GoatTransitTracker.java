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

import java.util.Collections;
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
 *
 * <p>The flight spot-anim is shared by every player's grab, so tracking it alone cannot tell whose goat is in
 * transit. The tracker therefore also records ownership: a goat becomes the local player's when it enters
 * flight while the local player is (or was last tick) interacting with it — telegrab sets that interaction,
 * a plain walk-here click does not. The whole-scene phase set drives the highlight suppression (any goat mid-
 * lure cannot be grabbed, whoever cast it), while the owned subset drives the in-transit count, so other
 * players' grabs no longer push the local player's tally up.
 *
 * <p>Cattle-prodded goats are tracked too. The goat the local player is interacting with while playing the
 * prod animation ({@link GoatIds#LOCAL_PROD_ANIM}) is taken as theirs and held in transit until it jumps in,
 * despawns, or {@link #MAX_PRODDED_TICKS} pass without a fresh prod. The goat's own reaction animation proved
 * too unreliable to arm from, whereas the interaction target is set the instant the prod lands. Prodding
 * carries no distinguishing graphic during the walk (like a lure) and no cap, so the count folds the local
 * player's lured and prodded goats into one total.
 *
 * <p>Goats prodded by <em>other</em> players are tracked separately, keyed off any scene player caught playing
 * the prod animation with a goat as its interaction target. These feed highlight suppression only — a goat
 * walking into a pit under anyone's prod is not a fresh grab target — but never the local player's in-transit
 * count, so a neighbour's prods do not inflate the local tally.
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

	/**
	 * Ticks a prodded goat is held in transit after the last prod before being dropped — about three seconds.
	 * A goat prodded into the pit is caught within a tick or two and clears the instant it despawns, so this
	 * only governs a goat that was prodded but not caught, which loses interest after a couple of seconds; a
	 * fresh prod refreshes it.
	 */
	private static final int MAX_PRODDED_TICKS = 5;

	private final Map<Integer, Phase> phases = new HashMap<>();
	private final Map<Integer, Integer> luredTicks = new HashMap<>();
	private final Map<Integer, Integer> proddedTicks = new HashMap<>();
	private final Map<Integer, Integer> remoteProddedTicks = new HashMap<>();
	private final Set<Integer> present = new HashSet<>();
	private final Set<Integer> owned = new HashSet<>();
	private int prevLocalTarget = -1;

	/**
	 * Advances every goat's phase by one tick from the live scene. Goats that have despawned since the last
	 * tick are dropped, which also clears a goat the instant it is caught.
	 *
	 * @param npcs             the world's current NPCs
	 * @param localTargetIndex the index of the NPC the local player is interacting with this tick, or
	 *                         {@code -1} if none — used to attribute a flight to the local player's own grab
	 * @param localProdding    whether the local player is playing the Cattleprod animation this tick, marking
	 *                         the goat they are interacting with as prodded toward the pit
	 */
	void onTick(Iterable<? extends NPC> npcs, int localTargetIndex, boolean localProdding)
	{
		onTick(npcs, localTargetIndex, localProdding, Collections.emptySet());
	}

	/**
	 * Advances every goat's phase by one tick from the live scene. Goats that have despawned since the last
	 * tick are dropped, which also clears a goat the instant it is caught.
	 *
	 * @param npcs                 the world's current NPCs
	 * @param localTargetIndex     the index of the NPC the local player is interacting with this tick, or
	 *                             {@code -1} if none — used to attribute a flight to the local player's own grab
	 * @param localProdding        whether the local player is playing the Cattleprod animation this tick,
	 *                             marking the goat they are interacting with as prodded toward the pit
	 * @param remoteProddedIndices indices of goats being prodded by other players this tick — suppressed from
	 *                             highlighting but never counted toward the local player's in-transit tally
	 */
	void onTick(Iterable<? extends NPC> npcs, int localTargetIndex, boolean localProdding,
		Set<Integer> remoteProddedIndices)
	{
		present.clear();
		for (NPC npc : npcs)
		{
			if (npc == null || !GoatPitTracker.matchesGoatName(npc.getName()))
				continue;

			int index = npc.getIndex();
			present.add(index);
			int animation = npc.getAnimation();
			boolean flying = npc.hasSpotAnim(GoatIds.IN_TRANSIT_SPOTANIM);
			boolean jumping = animation == GoatIds.IN_TRANSIT_ANIM;
			boolean localTargeted = index == localTargetIndex || index == prevLocalTarget;
			boolean freshlyProdded = localProdding && localTargeted;
			advance(index, flying, jumping, localTargeted);
			advanceProd(proddedTicks, index, freshlyProdded, jumping);
			advanceProd(remoteProddedTicks, index, remoteProddedIndices.contains(index), jumping);
		}

		phases.keySet().retainAll(present);
		luredTicks.keySet().retainAll(present);
		proddedTicks.keySet().retainAll(present);
		remoteProddedTicks.keySet().retainAll(present);
		owned.retainAll(phases.keySet());
		prevLocalTarget = localTargetIndex;
	}

	/** Applies one tick's phase transition for a single goat, tracking how long it has been walking. */
	private void advance(int index, boolean flying, boolean jumping, boolean localTargeted)
	{
		Phase prev = phases.get(index);
		Phase next = nextPhase(prev, flying, jumping);
		if (ownedThisTick(next, localTargeted))
			owned.add(index);

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
	 * Tracks a prodded goat's trip to the pit. A goat being prodded this tick (its prodder interacting with it
	 * while playing the prod animation) is (re)armed for {@link #MAX_PRODDED_TICKS}; otherwise a goat already
	 * tracked ages by one tick, its timer refreshed while it plays the jump-in, and drops once the timer runs
	 * out. The same logic drives both prod maps: {@link #proddedTicks} for the local player's own prods (which
	 * also feed the in-transit count) and {@link #remoteProddedTicks} for other players' (suppression only).
	 *
	 * @param ticks          the prod-timer map to advance
	 * @param index          the goat's NPC index
	 * @param freshlyProdded whether this goat is being prodded this tick
	 * @param jumping        whether the goat is playing the jump-in animation this tick
	 */
	private void advanceProd(Map<Integer, Integer> ticks, int index, boolean freshlyProdded, boolean jumping)
	{
		if (freshlyProdded)
		{
			ticks.put(index, MAX_PRODDED_TICKS);
			return;
		}

		if (!ticks.containsKey(index))
			return;

		if (jumping)
		{
			ticks.put(index, MAX_PRODDED_TICKS);
			return;
		}

		int left = ticks.get(index) - 1;
		if (left <= 0)
		{
			ticks.remove(index);
			return;
		}

		ticks.put(index, left);
	}

	/**
	 * Whether this tick establishes the goat as the local player's own grab. Ownership latches on the flight
	 * phase — the only phase carrying a caster-identifying interaction — and is kept for the rest of the
	 * transit by the {@link #owned} set. A goat already owned stays owned regardless of this result.
	 *
	 * @param next          the goat's phase this tick
	 * @param localTargeted whether the local player is (or was last tick) interacting with this goat
	 * @return true when the local player owns this in-transit goat
	 */
	static boolean ownedThisTick(Phase next, boolean localTargeted)
	{
		return next == Phase.FLIGHT && localTargeted;
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
			return Phase.FLIGHT;

		if (jumping)
			return Phase.JUMPING;

		if (prev == Phase.FLIGHT || prev == Phase.LURED)
			return Phase.LURED;

		return null;
	}

	/**
	 * Whether this goat is committed to a pit and so should not be highlighted or offered as a fresh target.
	 * Any goat in transit qualifies, whoever set it going — a goat already flying to a pit under a lure, or
	 * walking into one under any player's prod, cannot be grabbed again.
	 */
	boolean isInTransit(int npcIndex)
	{
		return phases.containsKey(npcIndex)
			|| proddedTicks.containsKey(npcIndex)
			|| remoteProddedTicks.containsKey(npcIndex);
	}

	/**
	 * How many of the local player's own goats are committed to a pit — those it lured plus those it
	 * prodded, counted once each. Other players' lured goats share the flight graphic but are excluded, so a
	 * busy pit does not inflate the local player's tally.
	 *
	 * <p>This is a single scene-wide total, not attributed to any particular pit object. Callers that iterate
	 * pits ({@code GoatPitOverlay}, {@code GoatHighlightOverlay}, {@code GoatMenuSwapper}) apply it to every
	 * pit alike, which holds only because a single personal pit is assumed in scene; a multi-pit area would
	 * need the in-transit goats keyed to their target pit.
	 */
	int inTransitCount()
	{
		Set<Integer> committed = new HashSet<>(owned);
		committed.addAll(proddedTicks.keySet());
		return committed.size();
	}

	/** Forgets every tracked goat, for when the scene is torn down. */
	void clear()
	{
		phases.clear();
		luredTicks.clear();
		proddedTicks.clear();
		remoteProddedTicks.clear();
		present.clear();
		owned.clear();
		prevLocalTarget = -1;
	}
}
