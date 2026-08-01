/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the tracker's identification and spikes-detection rules. These are the parts that decide
 * what the overlay draws, and they are pure functions of a name or an action list, so they need no
 * game client.
 */
public class GoatPitTrackerTest
{
	@Test
	public void pitNameMatchIsCaseInsensitive()
	{
		assertTrue(GoatPitTracker.matchesPitName("Goat pit"));
		assertTrue(GoatPitTracker.matchesPitName("GOAT PIT"));
		assertTrue(GoatPitTracker.matchesPitName("Empty goat pit"));
	}

	@Test
	public void unrelatedObjectsAreNotPits()
	{
		assertFalse(GoatPitTracker.matchesPitName("Goat"));
		assertFalse(GoatPitTracker.matchesPitName("Pit"));
		assertFalse(GoatPitTracker.matchesPitName(null));
	}

	@Test
	public void goatNameMatchIsCaseInsensitive()
	{
		assertTrue(GoatPitTracker.matchesGoatName("Goat"));
		assertTrue(GoatPitTracker.matchesGoatName("Billy goat"));
		assertFalse(GoatPitTracker.matchesGoatName("Sheep"));
		assertFalse(GoatPitTracker.matchesGoatName(null));
	}

	@Test
	public void anAddSpikesActionMeansThePitIsUnspiked()
	{
		assertFalse(GoatPitTracker.spikedFromActions(new String[]{"Add spikes", null, "Examine"}));
		assertFalse(GoatPitTracker.spikedFromActions(new String[]{"ADD SPIKES"}));
	}

	@Test
	public void withoutThatActionThePitCountsAsSpiked()
	{
		assertTrue(GoatPitTracker.spikedFromActions(new String[]{"Empty", "Examine"}));
		assertTrue(GoatPitTracker.spikedFromActions(new String[]{null, null}));
	}

	@Test
	public void missingActionsNeverProduceAFalsePrompt()
	{
		assertTrue(GoatPitTracker.spikedFromActions(null));
	}
}
