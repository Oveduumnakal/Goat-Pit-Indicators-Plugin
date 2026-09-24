/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.Collections;

import org.junit.Test;

import net.runelite.api.GameObject;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the idle nudge (#105): it fires once when the idle window passes with no activity, re-arms on the
 * next activity, stays quiet away from the pit or when switched off, rounds the window up to whole ticks, and
 * names the likely cause. The RuneLite notifier, config and pit types are mocked.
 */
public class IdleNotifierTest
{
	private final Notifier notifier = mock(Notifier.class);
	private final GoatIndicatorsConfig config = mock(GoatIndicatorsConfig.class);
	private final GoatPitTracker tracker = mock(GoatPitTracker.class);
	private final GoatTransitTracker transitTracker = mock(GoatTransitTracker.class);

	private final IdleNotifier idle = new IdleNotifier(notifier, config, tracker, transitTracker);

	@Test
	public void firesOnceWhenTheWindowPassesWithoutActivity()
	{
		enabledWithWindow(6);
		pit(new GoatPitState(5, true, 16));

		for (int tick = 0; tick <= 30; tick++)
			idle.onTick(tick, 1000);

		verify(notifier, times(1)).notify(Notification.ON, IdleNotifier.STALLED_MESSAGE);
	}

	@Test
	public void activityRestartsTheWindowAndReArms()
	{
		assertFalse(idle.stalled(0, 1, 10));
		assertFalse(idle.stalled(9, 1, 10));
		assertTrue(idle.stalled(10, 1, 10));
		assertFalse(idle.stalled(11, 1, 10));

		assertFalse(idle.stalled(12, 2, 10));
		assertFalse(idle.stalled(21, 2, 10));
		assertTrue(idle.stalled(22, 2, 10));
	}

	@Test
	public void staysQuietAwayFromThePit()
	{
		enabledWithWindow(6);
		when(tracker.getPits()).thenReturn(Collections.emptyList());

		for (int tick = 0; tick <= 30; tick++)
			idle.onTick(tick, 1000);

		verify(notifier, never()).notify(any(Notification.class), anyString());
	}

	@Test
	public void staysQuietWhenSwitchedOff()
	{
		when(config.idleNotification()).thenReturn(Notification.OFF);
		when(config.idleThresholdSeconds()).thenReturn(6);
		pit(new GoatPitState(5, true, 16));

		for (int tick = 0; tick <= 30; tick++)
			idle.onTick(tick, 1000);

		verify(notifier, never()).notify(any(Notification.class), anyString());
	}

	@Test
	public void theWindowRoundsUpToWholeTicks()
	{
		assertEquals(17, IdleNotifier.thresholdTicks(10));
		assertEquals(100, IdleNotifier.thresholdTicks(60));
		assertEquals(1, IdleNotifier.thresholdTicks(0));
	}

	@Test
	public void theMessageNamesTheLikelyCause()
	{
		assertEquals(IdleNotifier.FULL_MESSAGE, IdleNotifier.messageFor(new GoatPitState(16, true, 16)));
		assertEquals(IdleNotifier.SPIKES_MESSAGE, IdleNotifier.messageFor(new GoatPitState(0, false, 16)));
		assertEquals(IdleNotifier.STALLED_MESSAGE, IdleNotifier.messageFor(new GoatPitState(3, true, 16)));
	}

	@Test
	public void eachKindOfActivityChangesTheSignature()
	{
		long base = IdleNotifier.signature(new GoatPitState(3, true, 16), 0, 1000);
		assertTrue(base != IdleNotifier.signature(new GoatPitState(4, true, 16), 0, 1000));
		assertTrue(base != IdleNotifier.signature(new GoatPitState(3, false, 16), 0, 1000));
		assertTrue(base != IdleNotifier.signature(new GoatPitState(3, true, 16), 1, 1000));
		assertTrue(base != IdleNotifier.signature(new GoatPitState(3, true, 16), 0, 1001));
	}

	/** Switches the nudge on with the given idle window in seconds. */
	private void enabledWithWindow(int seconds)
	{
		when(config.idleNotification()).thenReturn(Notification.ON);
		when(config.idleThresholdSeconds()).thenReturn(seconds);
	}

	/** Points the tracker at one pit in the given state, with no goats in transit. */
	private void pit(GoatPitState state)
	{
		GameObject pit = mock(GameObject.class);
		when(tracker.getPits()).thenReturn(Collections.singletonList(pit));
		when(tracker.stateOf(pit)).thenReturn(state);
		when(transitTracker.inTransitCount()).thenReturn(0);
	}
}
