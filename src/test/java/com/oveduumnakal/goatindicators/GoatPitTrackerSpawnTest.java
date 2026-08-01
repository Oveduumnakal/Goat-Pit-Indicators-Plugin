/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import net.runelite.api.GameObject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the tracker's scene bookkeeping: which spawned objects it keeps as pits versus spike supplies,
 * and that despawn and clear drop them. Object identity runs through the id allowlists in {@link GoatIds}
 * (both non-empty), so no game client is needed — only mocked {@link GameObject} ids and hashes.
 */
public class GoatPitTrackerSpawnTest
{
	private static final int PIT_ID = 62343;
	private static final int SUPPLY_ID = 62349;

	@Test
	public void aSupplyObjectIsTrackedAsASupplyNotAPit()
	{
		GoatPitTracker tracker = new GoatPitTracker();
		GameObject supply = object(SUPPLY_ID, 1L);

		tracker.onSpawn(supply);

		assertTrue(tracker.getSupplies().contains(supply));
		assertFalse(tracker.getPits().contains(supply));
	}

	@Test
	public void aPitObjectIsTrackedAsAPitNotASupply()
	{
		GoatPitTracker tracker = new GoatPitTracker();
		GameObject pit = object(PIT_ID, 2L);

		tracker.onSpawn(pit);

		assertTrue(tracker.getPits().contains(pit));
		assertFalse(tracker.getSupplies().contains(pit));
	}

	@Test
	public void anUnrelatedObjectIsTrackedAsNeither()
	{
		GoatPitTracker tracker = new GoatPitTracker();

		tracker.onSpawn(object(999, 3L));

		assertTrue(tracker.getPits().isEmpty());
		assertTrue(tracker.getSupplies().isEmpty());
	}

	@Test
	public void despawnDropsASupply()
	{
		GoatPitTracker tracker = new GoatPitTracker();
		GameObject supply = object(SUPPLY_ID, 4L);

		tracker.onSpawn(supply);
		tracker.onDespawn(supply);

		assertTrue(tracker.getSupplies().isEmpty());
	}

	@Test
	public void clearDropsBothPitsAndSupplies()
	{
		GoatPitTracker tracker = new GoatPitTracker();
		tracker.onSpawn(object(PIT_ID, 5L));
		tracker.onSpawn(object(SUPPLY_ID, 6L));

		tracker.clear();

		assertTrue(tracker.getPits().isEmpty());
		assertTrue(tracker.getSupplies().isEmpty());
	}

	/** A mock scene object with the given id and scene hash. */
	private static GameObject object(int id, long hash)
	{
		GameObject object = mock(GameObject.class);
		when(object.getId()).thenReturn(id);
		when(object.getHash()).thenReturn(hash);
		return object;
	}
}
