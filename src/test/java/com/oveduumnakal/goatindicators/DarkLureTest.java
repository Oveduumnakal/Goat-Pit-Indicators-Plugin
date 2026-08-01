/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the pure Dark Lure cast-requirement rule: level, Arceuus spellbook, quest, and death + nature. */
public class DarkLureTest
{
	@Test
	public void needsBothDeathAndNatureRunes()
	{
		assertTrue(DarkLure.meetsRequirements(50, 3, true, true, true));
		assertFalse(DarkLure.meetsRequirements(50, 3, true, false, true));
		assertFalse(DarkLure.meetsRequirements(50, 3, false, true, true));
	}

	@Test
	public void belowLevelCannotCast()
	{
		assertFalse(DarkLure.meetsRequirements(49, 3, true, true, true));
	}

	@Test
	public void onlyTheArceuusSpellbookHasTheSpell()
	{
		assertFalse(DarkLure.meetsRequirements(50, 0, true, true, true));
		assertTrue(DarkLure.meetsRequirements(50, 3, true, true, true));
	}

	@Test
	public void theQuestMustBeComplete()
	{
		assertFalse(DarkLure.meetsRequirements(50, 3, true, true, false));
	}
}
