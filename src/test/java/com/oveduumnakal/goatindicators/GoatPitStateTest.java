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
	/** Capacity at Hunter 77-84, the value the plugin shipped as a fixed constant before 1.0.1. */
	private static final int CAP_20 = 20;

	@Test
	public void emptyUnspikedPitNeedsSpikes()
	{
		GoatPitState state = new GoatPitState(0, false, CAP_20);
		assertTrue(state.isEmpty());
		assertTrue(state.needsSpikes());
		assertFalse(state.isFull());
	}

	@Test
	public void emptySpikedPitIsWaitingNotBroken()
	{
		GoatPitState state = new GoatPitState(0, true, CAP_20);
		assertTrue(state.isEmpty());
		assertFalse(state.needsSpikes());
	}

	@Test
	public void oneGoatIsNeitherEmptyNorFull()
	{
		GoatPitState state = new GoatPitState(1, true, CAP_20);
		assertFalse(state.isEmpty());
		assertFalse(state.isFull());
		assertFalse(state.needsSpikes());
	}

	@Test
	public void nineteenGoatsIsNotYetFull()
	{
		assertFalse(new GoatPitState(19, true, CAP_20).isFull());
	}

	@Test
	public void twentyGoatsIsFull()
	{
		assertTrue(new GoatPitState(20, true, CAP_20).isFull());
	}

	/** A low-level pit (capacity 16) is full at 16, while a high-level pit (capacity 24) is not. */
	@Test
	public void fullnessTracksTheProvidedCapacity()
	{
		assertTrue(new GoatPitState(16, true, 16).isFull());
		assertFalse(new GoatPitState(16, true, 24).isFull());
		assertTrue(new GoatPitState(24, true, 24).isFull());
	}

	@Test
	public void needsSpikesTracksSpikesRegardlessOfCount()
	{
		assertTrue(new GoatPitState(0, false, CAP_20).needsSpikes());
		assertTrue(new GoatPitState(5, false, CAP_20).needsSpikes());
		assertTrue(new GoatPitState(20, false, CAP_20).needsSpikes());
		assertFalse(new GoatPitState(0, true, CAP_20).needsSpikes());
		assertFalse(new GoatPitState(20, true, CAP_20).needsSpikes());
	}

	@Test
	public void countIsClampedToCapacity()
	{
		assertEquals(20, new GoatPitState(37, true, CAP_20).getCount());
		assertEquals(0, new GoatPitState(-4, true, CAP_20).getCount());
		assertEquals(16, new GoatPitState(37, true, 16).getCount());
	}

	@Test
	public void capacityIsClampedToTheSupportedRange()
	{
		assertEquals(GoatIds.MIN_CAPACITY, new GoatPitState(0, true, 0).getCapacity());
		assertEquals(GoatIds.MAX_CAPACITY, new GoatPitState(0, true, 999).getCapacity());
		assertEquals(22, new GoatPitState(0, true, 22).getCapacity());
	}

	@Test
	public void labelReadsAsCountOverCapacity()
	{
		assertEquals("12 / 20", new GoatPitState(12, true, CAP_20).label());
		assertEquals("0 / 20", new GoatPitState(0, false, CAP_20).label());
		assertEquals("16 / 16", new GoatPitState(16, true, 16).label());
		assertEquals("18 / 24", new GoatPitState(18, true, 24).label());
	}
}
