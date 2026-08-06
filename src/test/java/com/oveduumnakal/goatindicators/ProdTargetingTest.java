/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the fixed prod range test and the nearest-pit-tile clamp. */
public class ProdTargetingTest
{
	@Test
	public void prodRangeCoversZeroToTheMaxAndRejectsBeyond()
	{
		assertTrue(ProdTargeting.withinProdRange(0));
		assertTrue(ProdTargeting.withinProdRange(ProdTargeting.MAX_RANGE));
		assertFalse(ProdTargeting.withinProdRange(ProdTargeting.MAX_RANGE + 1));
		assertFalse(ProdTargeting.withinProdRange(-1));
	}

	@Test
	public void clampGivesTheNearestPitTileOnAnAxis()
	{
		assertEquals(105, ProdTargeting.clampToPit(120, 100, 105));
		assertEquals(100, ProdTargeting.clampToPit(90, 100, 105));
		assertEquals(103, ProdTargeting.clampToPit(103, 100, 105));
	}

	@Test
	public void pushDirectionHitsAPitBeyondTheGoat()
	{
		assertTrue(ProdTargeting.pitInPushDirection(105, 100, 103, 100, 100, 100, 101, 101));
	}

	@Test
	public void pushDirectionMissesAPitBehindThePlayer()
	{
		assertFalse(ProdTargeting.pitInPushDirection(90, 100, 103, 100, 100, 100, 101, 101));
	}

	@Test
	public void pushDirectionToleratesAPitOffTheStraightLine()
	{
		assertTrue(ProdTargeting.pitInPushDirection(105, 100, 103, 100, 100, 101, 101, 102));
	}

	@Test
	public void pushDirectionMissesAPitBehindThePush()
	{
		assertFalse(ProdTargeting.pitInPushDirection(103, 105, 103, 103, 100, 105, 101, 106));
	}

	@Test
	public void pushDirectionIsFalseWhenThePlayerStandsOnTheGoat()
	{
		assertFalse(ProdTargeting.pitInPushDirection(103, 100, 103, 100, 100, 100, 101, 101));
	}
}
