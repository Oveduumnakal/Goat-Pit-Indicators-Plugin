/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Covers the prod pathfinding flood: which goat-adjacent tile a click walks the player to across open and
 * obstructed ground, and when the goat is unreachable. Uses a synthetic blocked-tile grid so no client is
 * needed. The goat sits at world tile (5, 0); the player starts at (0, 0).
 */
public class ProdPathingTest
{
	private static ProdPathing.StepFn blocking(Set<Long> blocked)
	{
		return (x, y, dx, dy) -> !blocked.contains(ProdPathing.key(x + dx, y + dy));
	}

	@Test
	public void openGroundLandsOnTheNearestNeighbour()
	{
		Map<Long, Integer> reach = ProdPathing.reachDistances(0, 0, blocking(new HashSet<>()), 20);

		assertArrayEquals(new int[]{4, 0}, ProdPathing.landingTile(5, 0, reach));
	}

	@Test
	public void aBlockedNearTileShiftsTheLandingToAReachableNeighbour()
	{
		Set<Long> blocked = new HashSet<>();
		blocked.add(ProdPathing.key(4, 0));
		Map<Long, Integer> reach = ProdPathing.reachDistances(0, 0, blocking(blocked), 20);

		assertArrayEquals(new int[]{4, -1}, ProdPathing.landingTile(5, 0, reach));
	}

	@Test
	public void aWallAcrossEveryApproachLeavesTheGoatUnreachable()
	{
		Set<Long> blocked = new HashSet<>();
		for (int y = -20; y <= 20; y++)
			blocked.add(ProdPathing.key(4, y));

		Map<Long, Integer> reach = ProdPathing.reachDistances(0, 0, blocking(blocked), 20);

		assertNull(ProdPathing.landingTile(5, 0, reach));
	}

	@Test
	public void theFloodMeasuresStepDistanceWithDiagonalsCostingOne()
	{
		Map<Long, Integer> reach = ProdPathing.reachDistances(0, 0, blocking(new HashSet<>()), 20);

		assertEquals(Integer.valueOf(0), reach.get(ProdPathing.key(0, 0)));
		assertEquals(Integer.valueOf(3), reach.get(ProdPathing.key(3, 3)));
		assertEquals(Integer.valueOf(5), reach.get(ProdPathing.key(5, 2)));
	}
}
