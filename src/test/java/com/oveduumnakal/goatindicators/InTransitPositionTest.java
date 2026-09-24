/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers which in-transit placements draw, when one stacks under the total-caught label, and that its
 * compass constants stay aligned with {@link TotalCaughtPosition} (the stacking couples the two by name).
 */
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

	/**
	 * {@link InTransitPosition#sharesTileWith(TotalCaughtPosition)} matches the two enums by {@code name()},
	 * so their eight compass constants must carry identical names. Renaming one in either enum without the
	 * other would silently disable the stack-under-total behaviour; this fails the build instead.
	 */
	@Test
	public void compassConstantNameSetsMatch()
	{
		Set<String> transit = new HashSet<>();
		for (InTransitPosition p : InTransitPosition.values())
		{
			if (p != InTransitPosition.OFF && p != InTransitPosition.CENTER)
				transit.add(p.name());
		}

		Set<String> total = new HashSet<>();
		for (TotalCaughtPosition p : TotalCaughtPosition.values())
		{
			if (p != TotalCaughtPosition.OFF)
				total.add(p.name());
		}

		assertEquals(total, transit);
	}

	/**
	 * A shared name is only useful if it lands on the same tile: the stack-under-total draws the in-transit
	 * line beneath a total placed by {@link TotalCaughtPosition}, so equally-named constants must resolve to
	 * the same scene coordinates or the two labels would drift apart.
	 */
	@Test
	public void sharedCompassPointsResolveToTheSameTile()
	{
		int minX = 10;
		int minY = 20;
		int maxX = 13;
		int maxY = 25;
		for (TotalCaughtPosition total : TotalCaughtPosition.values())
		{
			if (total == TotalCaughtPosition.OFF)
				continue;

			InTransitPosition transit = InTransitPosition.valueOf(total.name());
			assertEquals(total.sceneX(minX, maxX), transit.sceneX(minX, maxX));
			assertEquals(total.sceneY(minY, maxY), transit.sceneY(minY, maxY));
		}
	}
}
