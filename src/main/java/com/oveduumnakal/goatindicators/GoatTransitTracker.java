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
 *
 * <p>The flight spot-anim is shared by every player's grab, so tracking it alone cannot tell whose goat is in
 * transit. The tracker therefore also records ownership: a goat becomes the local player's when it enters
 * flight while the local player is (or was last tick) interacting with it — telegrab sets that interaction,
 * a plain walk-here click does not. The whole-scene phase set drives the highlight suppression (any goat mid-
 * lure cannot be grabbed, whoever cast it), while the owned subset drives the in-transit count, so other
 * players' grabs no longer push the local player's tally up.
 *
 * <p>Cattle-prodded goats are tracked too, but only for the count. A goat plays {@link GoatIds#PROD_REACT_ANIM}
 * the moment it is prodded; when that lands within a couple of ticks of the local player's own
 * {@link GoatIds#LOCAL_PROD_ANIM} it is taken as the local player's, and held in transit until it jumps in,
 * despawns, or {@link #MAX_PRODDED_TICKS} pass without a fresh prod. Prodding carries no distinguishing
 * graphic during the walk (like a lure) and no cap, so the count folds lured and prodded goats into one total.
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
	 * Ticks a prod attribution window stays open after the local player's prod animation, so the prodded
	 * goat's own reaction one tick later is still credited to the local player.
	 */
	private static final int PROD_WINDOW_TICKS = 2;

	/**
	 * Ticks a prodded goat is held in transit after its last prod reaction before being dropped — about three
	 * seconds. A goat prodded into the pit is caught within a tick or two and clears the instant it despawns,
	 * so this only governs a goat that was prodded but not caught, which loses interest after a couple of
	 * seconds; a fresh prod refreshes it.
	 */
	private static final int MAX_PRODDED_TICKS = 5;

	private final Map<Integer, Phase> phases = new HashMap<>();
	private final Map<Integer, Integer> luredTicks = new HashMap<>();
	private final Map<Integer, Integer> proddedTicks = new HashMap<>();
	private final Set<Integer> present = new HashSet<>();
	private final Set<Integer> owned = new HashSet<>();
	private int prevLocalTarget = -1;
	private int prodWindow;

	/**
	 * Advances every goat's phase by one tick from the live scene. Goats that have despawned since the last
	 * tick are dropped, which also clears a goat the instant it is caught.
	 *
	 * @param npcs             the world's current NPCs
	 * @param localTargetIndex the index of the NPC the local player is interacting with this tick, or
	 *                         {@code -1} if none — used to attribute a flight to the local player's own grab
	 * @param localProdding    whether the local player is playing the Cattleprod animation this tick, opening
	 *                         the window in which a goat's prod reaction is credited to the local player
	 */
	void onTick(Iterable<? extends NPC> npcs, int localTargetIndex, boolean localProdding)
	{
		prodWindow = localProdding ? PROD_WINDOW_TICKS : Math.max(0, prodWindow - 1);
		present.clear();
		for (NPC npc : npcs)
		{
			if (npc == null || !GoatPitTracker.matchesGoatName(npc.getName()))
			{
				continue;
			}
			int index = npc.getIndex();
			present.add(index);
			int animation = npc.getAnimation();
			boolean flying = npc.hasSpotAnim(GoatIds.IN_TRANSIT_SPOTANIM);
			boolean jumping = animation == GoatIds.IN_TRANSIT_ANIM;
			boolean localTargeted = index == localTargetIndex || index == prevLocalTarget;
			advance(index, flying, jumping, localTargeted);
			advanceProd(index, animation, jumping);
		}
		phases.keySet().retainAll(present);
		luredTicks.keySet().retainAll(present);
		proddedTicks.keySet().retainAll(present);
		owned.retainAll(phases.keySet());
		prevLocalTarget = localTargetIndex;
	}

	/** Applies one tick's phase transition for a single goat, tracking how long it has been walking. */
	private void advance(int index, boolean flying, boolean jumping, boolean localTargeted)
	{
		Phase prev = phases.get(index);
		Phase next = nextPhase(prev, flying, jumping);
		if (ownedThisTick(next, localTargeted))
		{
			owned.add(index);
		}
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
	 * Tracks a prodded goat's trip to the pit for the in-transit count. A goat playing
	 * {@link GoatIds#PROD_REACT_ANIM} while the prod window is open is (re)armed as the local player's for
	 * {@link #MAX_PRODDED_TICKS}; otherwise a goat already tracked ages by one tick, its timer refreshed
	 * while it plays the jump-in, and drops once the timer runs out. Prodded goats are held apart from the
	 * lure {@link #phases} so they never affect highlight suppression.
	 *
	 * @param index     the goat's NPC index
	 * @param animation the goat's animation this tick
	 * @param jumping   whether the goat is playing the jump-in animation this tick
	 */
	private void advanceProd(int index, int animation, boolean jumping)
	{
		if (startsProdTransit(animation, prodWindow > 0))
		{
			proddedTicks.put(index, MAX_PRODDED_TICKS);
			return;
		}
		if (!proddedTicks.containsKey(index))
		{
			return;
		}
		if (jumping)
		{
			proddedTicks.put(index, MAX_PRODDED_TICKS);
			return;
		}
		int left = proddedTicks.get(index) - 1;
		if (left <= 0)
		{
			proddedTicks.remove(index);
			return;
		}
		proddedTicks.put(index, left);
	}

	/**
	 * Whether this tick starts (or refreshes) a prodded goat's transit: the goat is playing its prod-reaction
	 * animation and the local player prodded within the attribution window.
	 *
	 * @param animation      the goat's animation this tick
	 * @param prodWindowOpen whether the local player's prod window is still open
	 * @return true when the goat is a fresh local prod
	 */
	static boolean startsProdTransit(int animation, boolean prodWindowOpen)
	{
		return animation == GoatIds.PROD_REACT_ANIM && prodWindowOpen;
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

	/**
	 * Whether this goat is mid-lure and so should not be highlighted or offered as a fresh target. Any goat
	 * in transit qualifies, whoever cast the grab — an already-flying goat cannot be grabbed again.
	 */
	boolean isInTransit(int npcIndex)
	{
		return phases.containsKey(npcIndex);
	}

	/**
	 * How many of the local player's own goats are committed to a pit — those it lured plus those it
	 * prodded, counted once each. Other players' lured goats share the flight graphic but are excluded, so a
	 * busy pit does not inflate the local player's tally.
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
		present.clear();
		owned.clear();
		prevLocalTarget = -1;
		prodWindow = 0;
	}
}
