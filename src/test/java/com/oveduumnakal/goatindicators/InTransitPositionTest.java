/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
	public void centerDoesNotShareACenteredTotal()
	{
		assertFalse(InTransitPosition.CENTER.sharesTileWith(TotalCaughtPosition.CENTER));
	}

	@Test
	public void compassPointDoesNotShareACenteredTotal()
	{
		assertFalse(InTransitPosition.NORTH.sharesTileWith(TotalCaughtPosition.CENTER));
	}

	@Test
	public void totalCenterIsTheOnlyCenterPlacement()
	{
		for (TotalCaughtPosition position : TotalCaughtPosition.values())
			assertEquals(position == TotalCaughtPosition.CENTER, position.isCenter());
	}

	@Test
	public void totalCenterSitsMidPit()
	{
		assertEquals(5, TotalCaughtPosition.CENTER.sceneX(4, 6));
		assertEquals(11, TotalCaughtPosition.CENTER.sceneY(10, 12));
	}

	@Test
	public void nothingSharesAnOffTotal()
	{
		assertFalse(InTransitPosition.NORTH.sharesTileWith(TotalCaughtPosition.OFF));
	}
}
