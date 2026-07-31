/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Covers which in-transit placements draw, and when one stacks under the total-caught label. */
public class InTransitPositionTest
{
	@Test
	public void offDoesNotDraw()
	{
		assertFalse(InTransitPosition.OFF.isDrawn());
		assertFalse(InTransitPosition.OFF.isCenter());
	}

	@Test
	public void centerDrawsUnderTheCount()
	{
		assertTrue(InTransitPosition.CENTER.isDrawn());
		assertTrue(InTransitPosition.CENTER.isCenter());
	}

	@Test
	public void matchingCompassPointSharesTheTotalTile()
	{
		assertTrue(InTransitPosition.SOUTH_EAST.sharesTileWith(TotalCaughtPosition.SOUTH_EAST));
	}

	@Test
	public void differentCompassPointDoesNotShareTheTotalTile()
	{
		assertFalse(InTransitPosition.SOUTH_EAST.sharesTileWith(TotalCaughtPosition.NORTH_WEST));
	}

	@Test
	public void centerNeverSharesTheTotalTile()
	{
		assertFalse(InTransitPosition.CENTER.sharesTileWith(TotalCaughtPosition.NORTH));
	}

	@Test
	public void nothingSharesAnOffTotal()
	{
		assertFalse(InTransitPosition.NORTH.sharesTileWith(TotalCaughtPosition.OFF));
	}
}
