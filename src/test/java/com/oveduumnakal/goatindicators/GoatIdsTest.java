/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** Covers the Hunter-level -> pit capacity mapping against the OSRS Wiki's stepped table. */
public class GoatIdsTest
{
	@Test
	public void capacityStepsUpWithHunterLevel()
	{
		assertEquals(16, GoatIds.capacityForHunterLevel(60));
		assertEquals(16, GoatIds.capacityForHunterLevel(69));
		assertEquals(18, GoatIds.capacityForHunterLevel(70));
		assertEquals(18, GoatIds.capacityForHunterLevel(76));
		assertEquals(20, GoatIds.capacityForHunterLevel(77));
		assertEquals(20, GoatIds.capacityForHunterLevel(84));
		assertEquals(22, GoatIds.capacityForHunterLevel(85));
		assertEquals(22, GoatIds.capacityForHunterLevel(92));
		assertEquals(24, GoatIds.capacityForHunterLevel(93));
		assertEquals(24, GoatIds.capacityForHunterLevel(99));
	}

	/** The pit needs Hunter 60 to use; anything lower (or an unread level) reports the minimum. */
	@Test
	public void levelsBelowSixtyFallBackToTheMinimum()
	{
		assertEquals(GoatIds.MIN_CAPACITY, GoatIds.capacityForHunterLevel(1));
		assertEquals(GoatIds.MIN_CAPACITY, GoatIds.capacityForHunterLevel(59));
		assertEquals(GoatIds.MIN_CAPACITY, GoatIds.capacityForHunterLevel(-1));
	}

	@Test
	public void capacityNeverLeavesTheSupportedRange()
	{
		for (int level = -5; level <= 130; level++)
		{
			int capacity = GoatIds.capacityForHunterLevel(level);
			assertEquals("even capacity", 0, capacity % 2);
			assertEquals("within range", true,
				capacity >= GoatIds.MIN_CAPACITY && capacity <= GoatIds.MAX_CAPACITY);
		}
	}
}
