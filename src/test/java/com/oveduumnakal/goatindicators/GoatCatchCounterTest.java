/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** Covers the session catch-counting logic: upward steps count, emptying does not, seeding gates. */
public class GoatCatchCounterTest
{
	@Test
	public void countsUpwardStepsWhileSeeded()
	{
		GoatCatchCounter counter = new GoatCatchCounter();
		counter.seed(0);
		counter.onCountChanged(1);
		counter.onCountChanged(2);
		counter.onCountChanged(3);
		assertEquals(3, counter.getTotal());
	}

	@Test
	public void aStepLargerThanOneCountsEveryGoat()
	{
		GoatCatchCounter counter = new GoatCatchCounter();
		counter.seed(0);
		counter.onCountChanged(4);
		assertEquals(4, counter.getTotal());
	}

	@Test
	public void emptyingDoesNotCountAndDoesNotDoubleCountTheRefill()
	{
		GoatCatchCounter counter = new GoatCatchCounter();
		counter.seed(0);
		counter.onCountChanged(5);
		counter.onCountChanged(0);
		counter.onCountChanged(2);
		assertEquals(7, counter.getTotal());
	}

	@Test
	public void nothingCountsUntilSeeded()
	{
		GoatCatchCounter counter = new GoatCatchCounter();
		counter.onCountChanged(3);
		counter.onCountChanged(4);
		assertEquals(0, counter.getTotal());
	}

	@Test
	public void seedingFromAPartFullPitDoesNotCountThoseGoats()
	{
		GoatCatchCounter counter = new GoatCatchCounter();
		counter.seed(8);
		counter.onCountChanged(9);
		assertEquals(1, counter.getTotal());
	}

	@Test
	public void suspendKeepsTheTotalButStopsCounting()
	{
		GoatCatchCounter counter = new GoatCatchCounter();
		counter.seed(0);
		counter.onCountChanged(3);
		counter.suspend();
		counter.onCountChanged(4);
		assertEquals(3, counter.getTotal());
	}

	@Test
	public void resetClearsTheTotalAndBaseline()
	{
		GoatCatchCounter counter = new GoatCatchCounter();
		counter.seed(0);
		counter.onCountChanged(3);
		counter.reset();
		counter.onCountChanged(4);
		assertEquals(0, counter.getTotal());
	}
}
