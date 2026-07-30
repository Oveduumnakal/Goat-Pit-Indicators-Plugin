/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Covers the pure cast-requirement rule: level, spellbook, and the law-plus-air-source cost. */
public class TelekineticGrabTest
{
	@Test
	public void needsLawRuneAndAnAirSource()
	{
		assertTrue(TelekineticGrab.meetsRequirements(33, 0, true, true, false));
		assertTrue(TelekineticGrab.meetsRequirements(33, 0, true, false, true));
		assertFalse(TelekineticGrab.meetsRequirements(33, 0, false, true, false));
		assertFalse(TelekineticGrab.meetsRequirements(33, 0, true, false, false));
	}

	@Test
	public void airStaffStandsInForTheAirRune()
	{
		assertTrue(TelekineticGrab.meetsRequirements(40, 0, true, false, true));
	}

	@Test
	public void belowLevelCannotCast()
	{
		assertFalse(TelekineticGrab.meetsRequirements(32, 0, true, true, true));
	}

	@Test
	public void onlyTheStandardSpellbookHasTheSpell()
	{
		assertFalse(TelekineticGrab.meetsRequirements(50, 1, true, true, true));
		assertTrue(TelekineticGrab.meetsRequirements(50, 0, true, true, true));
	}
}
