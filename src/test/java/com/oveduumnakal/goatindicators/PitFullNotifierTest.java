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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the pit-full alert: it fires once as the pit crosses into full, stays quiet while it remains full,
 * re-arms after the pit empties, honours the count-in-transit option, and never fires without a spiked pit.
 * The RuneLite notifier, config and pit types are mocked.
 */
public class PitFullNotifierTest
{
	private final Notifier notifier = mock(Notifier.class);
	private final GoatIndicatorsConfig config = mock(GoatIndicatorsConfig.class);
	private final GoatPitTracker tracker = mock(GoatPitTracker.class);
	private final GoatTransitTracker transitTracker = mock(GoatTransitTracker.class);

	private final PitFullNotifier notifierUnderTest =
		new PitFullNotifier(notifier, config, tracker, transitTracker);

	@Test
	public void firesOnceWhenThePitBecomesFullAndStaysQuietWhileFull()
	{
		when(config.pitFullNotification()).thenReturn(Notification.ON);
		when(config.notifyCountInTransit()).thenReturn(true);
		singleSpikedPit(16, 16);

		notifierUnderTest.onTick();
		notifierUnderTest.onTick();

		verify(notifier, times(1)).notify(any(Notification.class), anyString());
	}

	@Test
	public void reArmsAndFiresAgainAfterThePitEmptiesAndRefills()
	{
		when(config.pitFullNotification()).thenReturn(Notification.ON);
		when(config.notifyCountInTransit()).thenReturn(true);

		singleSpikedPit(16, 16);
		notifierUnderTest.onTick();

		singleSpikedPit(4, 16);
		notifierUnderTest.onTick();

		singleSpikedPit(16, 16);
		notifierUnderTest.onTick();

		verify(notifier, times(2)).notify(any(Notification.class), anyString());
	}

	@Test
	public void countsGoatsInTransitTowardTheTriggerWhenEnabled()
	{
		when(config.pitFullNotification()).thenReturn(Notification.ON);
		when(config.notifyCountInTransit()).thenReturn(true);
		singleSpikedPit(10, 16);
		when(transitTracker.inTransitCount()).thenReturn(6);

		notifierUnderTest.onTick();

		verify(notifier, times(1)).notify(any(Notification.class), anyString());
	}

	@Test
	public void ignoresGoatsInTransitWhenTheOptionIsOff()
	{
		when(config.pitFullNotification()).thenReturn(Notification.ON);
		when(config.notifyCountInTransit()).thenReturn(false);
		singleSpikedPit(10, 16);
		when(transitTracker.inTransitCount()).thenReturn(6);

		notifierUnderTest.onTick();

		verify(notifier, never()).notify(any(Notification.class), anyString());
	}

	@Test
	public void doesNotFireWhileNoSpikedPitCanCatch()
	{
		when(config.notifyCountInTransit()).thenReturn(true);
		GameObject pit = mock(GameObject.class);
		when(tracker.getPits()).thenReturn(Collections.singletonList(pit));
		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(16, false, 16));

		notifierUnderTest.onTick();

		verify(notifier, never()).notify(any(Notification.class), anyString());
	}

	/** Points the tracker at one spiked pit at {@code count}/{@code capacity}, with no goats in transit. */
	private void singleSpikedPit(int count, int capacity)
	{
		GameObject pit = mock(GameObject.class);
		when(tracker.getPits()).thenReturn(Collections.singletonList(pit));
		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(count, true, capacity));
		when(transitTracker.inTransitCount()).thenReturn(0);
	}
}
