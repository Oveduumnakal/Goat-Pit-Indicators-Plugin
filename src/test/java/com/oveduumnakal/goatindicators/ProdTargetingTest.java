/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the prod range test, the nearest-pit-tile clamp, and the away-from-pit stand-tile offset. */
public class ProdTargetingTest
{
	@Test
	public void prodRangeCoversZeroToMax()
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
	public void standTileSitsOneStepBeyondTheGoatAwayFromThePit()
	{
		assertEquals(11, ProdTargeting.standTile(10, 5));
		assertEquals(4, ProdTargeting.standTile(5, 10));
	}

	@Test
	public void anAxisAlignedGoatDoesNotShiftOnThatAxis()
	{
		assertEquals(10, ProdTargeting.standTile(10, 10));
	}
}
