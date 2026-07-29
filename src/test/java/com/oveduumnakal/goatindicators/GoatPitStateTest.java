/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Covers the fullness and "needs spikes" rules at their boundaries. */
public class GoatPitStateTest
{
	@Test
	public void emptyUnspikedPitNeedsSpikes()
	{
		GoatPitState state = new GoatPitState(0, false);
		assertTrue(state.isEmpty());
		assertTrue(state.needsSpikes());
		assertFalse(state.isFull());
	}

	@Test
	public void emptySpikedPitIsWaitingNotBroken()
	{
		GoatPitState state = new GoatPitState(0, true);
		assertTrue(state.isEmpty());
		assertFalse(state.needsSpikes());
	}

	@Test
	public void oneGoatIsNeitherEmptyNorFull()
	{
		GoatPitState state = new GoatPitState(1, true);
		assertFalse(state.isEmpty());
		assertFalse(state.isFull());
		assertFalse(state.needsSpikes());
	}

	@Test
	public void nineteenGoatsIsNotYetFull()
	{
		assertFalse(new GoatPitState(19, true).isFull());
	}

	@Test
	public void twentyGoatsIsFull()
	{
		assertTrue(new GoatPitState(20, true).isFull());
	}

	@Test
	public void needsSpikesTracksSpikesRegardlessOfCount()
	{
		assertTrue(new GoatPitState(0, false).needsSpikes());
		assertTrue(new GoatPitState(5, false).needsSpikes());
		assertTrue(new GoatPitState(20, false).needsSpikes());
		assertFalse(new GoatPitState(0, true).needsSpikes());
		assertFalse(new GoatPitState(20, true).needsSpikes());
	}

	@Test
	public void countIsClampedToCapacity()
	{
		assertEquals(20, new GoatPitState(37, true).getCount());
		assertEquals(0, new GoatPitState(-4, true).getCount());
	}

	@Test
	public void labelReadsAsCountOverCapacity()
	{
		assertEquals("12 / 20", new GoatPitState(12, true).label());
		assertEquals("0 / 20", new GoatPitState(0, false).label());
	}
}
