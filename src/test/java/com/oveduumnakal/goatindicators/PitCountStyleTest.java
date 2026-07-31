/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Covers which parts each pit-count display style draws. */
public class PitCountStyleTest
{
	@Test
	public void textStyleDrawsTextOnly()
	{
		assertTrue(PitCountStyle.TEXT.showsText());
		assertFalse(PitCountStyle.TEXT.showsBar());
	}

	@Test
	public void barStyleDrawsBarOnly()
	{
		assertTrue(PitCountStyle.BAR.showsBar());
		assertFalse(PitCountStyle.BAR.showsText());
	}

	@Test
	public void barAndTextStyleDrawsBoth()
	{
		assertTrue(PitCountStyle.BAR_AND_TEXT.showsBar());
		assertTrue(PitCountStyle.BAR_AND_TEXT.showsText());
	}
}
