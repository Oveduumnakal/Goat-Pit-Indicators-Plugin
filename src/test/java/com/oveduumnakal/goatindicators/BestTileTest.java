/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Covers the best-tile scoring (#104) on a synthetic 3x3 pit spanning (10, 10)-(12, 12): lure counts goats in
 * range, across the pit and in sight; prod counts adjacent goats a push sends into the pit; the pick takes the
 * highest score, breaks ties by fewest steps, never suggests a pit tile, and yields nothing when no tile scores.
 */
public class BestTileTest
{
	private static final List<int[]> PITS = Collections.singletonList(new int[]{10, 10, 12, 12});

	private static final BestTile.SightFn ALWAYS = (x, y, id) -> true;

	@Test
	public void lureCountsGoatsInRangeAcrossThePitAndInSight()
	{
		List<int[]> goats = Arrays.asList(new int[]{11, 14, 0}, new int[]{11, 15, 1}, new int[]{11, 8, 2});

		assertEquals(2, BestTile.lureCount(11, 7, goats, PITS, ALWAYS));
		assertEquals(1, BestTile.lureCount(11, 7, goats, PITS, (x, y, id) -> id != 1));
	}

	@Test
	public void lureLeavesOutGoatsBeyondCastRange()
	{
		List<int[]> goats = Arrays.asList(new int[]{11, 14, 0}, new int[]{11, 15, 1});

		assertEquals(1, BestTile.lureCount(11, 4, goats, PITS, ALWAYS));
	}

	@Test
	public void prodCountsAdjacentGoatsPushedTowardThePit()
	{
		List<int[]> goats = Collections.singletonList(new int[]{11, 14, 0});

		assertEquals(1, BestTile.prodCount(11, 15, goats, PITS));
		assertEquals(0, BestTile.prodCount(11, 13, goats, PITS));
		assertEquals(0, BestTile.prodCount(11, 17, goats, PITS));
	}

	@Test
	public void pickTakesTheHighestScoreThenTheFewestSteps()
	{
		Map<Long, Integer> reach = new HashMap<>();
		reach.put(ProdPathing.key(0, 0), 0);
		reach.put(ProdPathing.key(1, 0), 1);
		reach.put(ProdPathing.key(2, 0), 2);
		reach.put(ProdPathing.key(3, 0), 3);

		assertArrayEquals(new int[]{2, 0, 5}, BestTile.pick(reach, PITS, (x, y) -> x == 2 ? 5 : 1));
		assertArrayEquals(new int[]{1, 0, 3}, BestTile.pick(reach, PITS, (x, y) -> x >= 1 ? 3 : 0));
	}

	@Test
	public void pickNeverSuggestsAPitTile()
	{
		Map<Long, Integer> reach = new HashMap<>();
		reach.put(ProdPathing.key(11, 11), 1);
		reach.put(ProdPathing.key(11, 9), 2);

		assertArrayEquals(new int[]{11, 9, 1}, BestTile.pick(reach, PITS, (x, y) -> x == 11 && y == 11 ? 9 : 1));
	}

	@Test
	public void pickYieldsNothingWhenNoTileScores()
	{
		Map<Long, Integer> reach = new HashMap<>();
		reach.put(ProdPathing.key(0, 0), 0);
		reach.put(ProdPathing.key(-5, -7), 3);

		assertNull(BestTile.pick(reach, PITS, (x, y) -> 0));
	}

	@Test
	public void pickUnpacksNegativeCoordinates()
	{
		Map<Long, Integer> reach = new HashMap<>();
		reach.put(ProdPathing.key(-5, -7), 3);

		assertArrayEquals(new int[]{-5, -7, 1}, BestTile.pick(reach, PITS, (x, y) -> 1));
	}
}
