/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers Telegrab range and the opposite-side lure geometry. */
public class TelegrabTargetingTest
{
	@Test
	public void castRangeCoversZeroToTenTiles()
	{
		assertTrue(TelegrabTargeting.withinCastRange(0));
		assertTrue(TelegrabTargeting.withinCastRange(10));
		assertFalse(TelegrabTargeting.withinCastRange(11));
	}

	@Test
	public void effectivelyFullCountsPitAndTransitTogether()
	{
		assertFalse(TelegrabTargeting.effectivelyFull(18, 1, 20));
		assertTrue(TelegrabTargeting.effectivelyFull(18, 2, 20));
		assertTrue(TelegrabTargeting.effectivelyFull(19, 2, 20));
		assertTrue(TelegrabTargeting.effectivelyFull(20, 0, 20));
		assertFalse(TelegrabTargeting.effectivelyFull(0, 0, 16));
	}

	@Test
	public void crossPlaneDistanceIsOutOfRange()
	{
		assertFalse(TelegrabTargeting.withinCastRange(Integer.MAX_VALUE));
		assertFalse(TelegrabTargeting.withinCastRange(-1));
	}

	/**
	 * Mirrors the account holder's diagram: a 3x3 pit with the player one tile south of it. Goats level
	 * with the pit or north of it are targets; anything on the player's (south) side is not.
	 */
	@Test
	public void playerSouthTargetsGoatsAtOrNorthOfPit()
	{
		int minX = 40;
		int minY = 40;
		int maxX = 42;
		int maxY = 42;
		int px = 41;
		int py = 39;
		assertTrue(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 41, 45));
		assertTrue(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 41, 40));
		assertTrue(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 48, 41));
		assertFalse(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 41, 39));
		assertFalse(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 30, 38));
	}

	@Test
	public void playerNorthMirrorsTheRule()
	{
		int minX = 40;
		int minY = 40;
		int maxX = 42;
		int maxY = 42;
		int px = 41;
		int py = 43;
		assertTrue(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 41, 40));
		assertTrue(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 41, 42));
		assertFalse(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, px, py, 41, 43));
	}

	@Test
	public void playerEastAndWestUseTheXAxis()
	{
		int minX = 40;
		int minY = 40;
		int maxX = 42;
		int maxY = 42;
		assertTrue(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, 45, 41, 41, 41));
		assertFalse(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, 45, 41, 44, 41));
		assertTrue(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, 38, 41, 41, 41));
		assertFalse(TelegrabTargeting.oppositeSideOfPit(minX, minY, maxX, maxY, 38, 41, 39, 41));
	}

	@Test
	public void playerLevelWithPitOnBothAxesHasNoFarSide()
	{
		assertFalse(TelegrabTargeting.oppositeSideOfPit(40, 40, 42, 42, 41, 41, 50, 50));
	}
}
