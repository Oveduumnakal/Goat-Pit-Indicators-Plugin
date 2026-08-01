/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.runelite.api.NPC;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the stateful per-tick composition of {@link GoatTransitTracker#onTick} that the pure
 * {@code nextPhase}/{@code ownedThisTick} tests cannot reach: multi-tick phase persistence, the lured and
 * prodded expiry timers, despawn cleanup, ownership latching, and the lure-plus-prod union that
 * {@link GoatTransitTracker#inTransitCount()} returns. Goats are mocked with only the four methods
 * {@code onTick} reads.
 */
public class GoatTransitTrackerOnTickTest
{
	/** A non-transit idle animation, distinct from the jump-in animation the tracker watches for. */
	private static final int IDLE_ANIM = -1;

	@Test
	public void aGoatStaysInTransitFromFlightThroughWalkAndJumpThenClears()
	{
		GoatTransitTracker tracker = new GoatTransitTracker();

		tracker.onTick(one(goat(7, true, false)), 7, false);
		assertTrue(tracker.isInTransit(7));

		tracker.onTick(one(goat(7, false, false)), -1, false);
		assertTrue(tracker.isInTransit(7));

		tracker.onTick(one(goat(7, false, true)), -1, false);
		assertTrue(tracker.isInTransit(7));

		tracker.onTick(one(goat(7, false, false)), -1, false);
		assertFalse(tracker.isInTransit(7));
	}

	@Test
	public void despawnClearsAnInTransitGoat()
	{
		GoatTransitTracker tracker = new GoatTransitTracker();

		tracker.onTick(one(goat(7, true, false)), 7, false);
		assertTrue(tracker.isInTransit(7));

		tracker.onTick(Collections.emptyList(), -1, false);
		assertFalse(tracker.isInTransit(7));
		assertEquals(0, tracker.inTransitCount());
	}

	@Test
	public void aLuredGoatIsDroppedOnceItWalksPastTheLuredCap()
	{
		GoatTransitTracker tracker = new GoatTransitTracker();

		tracker.onTick(one(goat(7, true, false)), 7, false);
		for (int walk = 0; walk < 10; walk++)
			tracker.onTick(one(goat(7, false, false)), -1, false);

		assertTrue(tracker.isInTransit(7));

		tracker.onTick(one(goat(7, false, false)), -1, false);
		assertFalse(tracker.isInTransit(7));
	}

	@Test
	public void aProddedGoatCountsThenDropsOnceItsTimerRunsOut()
	{
		GoatTransitTracker tracker = new GoatTransitTracker();

		tracker.onTick(one(goat(7, false, false)), 7, true);
		assertEquals(1, tracker.inTransitCount());

		for (int idle = 0; idle < 4; idle++)
			tracker.onTick(one(goat(7, false, false)), -1, false);

		assertEquals(1, tracker.inTransitCount());

		tracker.onTick(one(goat(7, false, false)), -1, false);
		assertEquals(0, tracker.inTransitCount());
	}

	@Test
	public void aFreshProdRearmsAGoatsTimer()
	{
		GoatTransitTracker tracker = new GoatTransitTracker();

		tracker.onTick(one(goat(7, false, false)), 7, true);
		tracker.onTick(one(goat(7, false, false)), -1, false);
		tracker.onTick(one(goat(7, false, false)), 7, true);
		for (int idle = 0; idle < 4; idle++)
			tracker.onTick(one(goat(7, false, false)), -1, false);

		assertEquals(1, tracker.inTransitCount());
	}

	@Test
	public void onlyTheLocalPlayersOwnFlightCountsTowardTheTally()
	{
		GoatTransitTracker tracker = new GoatTransitTracker();

		NPC own = goat(1, true, false);
		NPC other = goat(2, true, false);
		tracker.onTick(Arrays.asList(own, other), 1, false);

		assertTrue(tracker.isInTransit(1));
		assertTrue(tracker.isInTransit(2));
		assertEquals(1, tracker.inTransitCount());
	}

	@Test
	public void aGoatBothLuredAndProddedIsCountedOnce()
	{
		GoatTransitTracker tracker = new GoatTransitTracker();

		tracker.onTick(one(goat(3, true, false)), 3, true);
		assertEquals(1, tracker.inTransitCount());
	}

	/** A single-goat scene, for the common one-goat tick. */
	private static List<NPC> one(NPC npc)
	{
		return Collections.singletonList(npc);
	}

	/** A mock goat carrying only the flight spot-anim, jump animation, name and index {@code onTick} reads. */
	private static NPC goat(int index, boolean flying, boolean jumping)
	{
		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Goat");
		when(npc.getIndex()).thenReturn(index);
		when(npc.hasSpotAnim(GoatIds.IN_TRANSIT_SPOTANIM)).thenReturn(flying);
		when(npc.getAnimation()).thenReturn(jumping ? GoatIds.IN_TRANSIT_ANIM : IDLE_ANIM);
		return npc;
	}
}
