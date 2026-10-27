/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import net.runelite.api.GameObject;
import net.runelite.api.Tile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the tracker's scene bookkeeping: which spawned objects it keeps as pits versus spike supplies,
 * that despawn and clear drop them, and that the startup scene scan picks up objects already loaded.
 * Object identity runs through the id allowlists in {@link GoatIds} (both non-empty), so no game client is
 * needed — only mocked {@link GameObject} ids and hashes.
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

	@Test
	public void scanPicksUpObjectsAlreadyInTheSceneOnce()
	{
		GoatPitTracker tracker = new GoatPitTracker();
		GameObject pit = object(PIT_ID, 5L);
		GameObject supply = object(SUPPLY_ID, 6L);
		GameObject other = object(999, 7L);
		Tile pitWest = tile(pit);
		Tile pitEast = tile(pit, other);
		Tile supplyTile = tile(null, supply);
		Tile[][][] tiles = {{{pitWest, null}, {pitEast, supplyTile}}, null, {null}};

		tracker.scan(tiles);

		assertEquals(1, tracker.getPits().size());
		assertTrue(tracker.getPits().contains(pit));
		assertEquals(1, tracker.getSupplies().size());
		assertTrue(tracker.getSupplies().contains(supply));
	}

	@Test
	public void scanToleratesAMissingScene()
	{
		GoatPitTracker tracker = new GoatPitTracker();

		tracker.scan(null);

		assertTrue(tracker.getPits().isEmpty());
	}

	/** A mock scene tile holding the given game objects (entries may be null). */
	private static Tile tile(GameObject... objects)
	{
		Tile tile = mock(Tile.class);
		when(tile.getGameObjects()).thenReturn(objects);
		return tile;
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
