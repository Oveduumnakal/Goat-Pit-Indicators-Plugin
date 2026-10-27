/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.time.Duration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Covers the session infobox's duration formatting: {@code MM:SS} under an hour and {@code H:MM:SS} past it,
 * with zero-padded minutes and seconds.
 */
public class SessionStatsOverlayTest
{
	@Test
	public void formatsUnderAnHourAsMinutesAndSeconds()
	{
		assertEquals("00:00", SessionStatsOverlay.formatDuration(Duration.ZERO));
		assertEquals("05:09", SessionStatsOverlay.formatDuration(Duration.ofSeconds(309)));
	}

	@Test
	public void formatsPastAnHourWithTheHoursField()
	{
		assertEquals("1:02:03", SessionStatsOverlay.formatDuration(Duration.ofSeconds(3723)));
	}

	@Test
	public void clampsNegativeDurationsToZero()
	{
		assertEquals("00:00", SessionStatsOverlay.formatDuration(Duration.ofSeconds(-5)));
	}

	@Test
	public void lootValueSumsBothItemsWithoutOverflow()
	{
		assertEquals(3 * 110 + 9 * 200, SessionStatsOverlay.lootValue(3, 110, 9, 200));
		assertEquals(3_000_000_000L, SessionStatsOverlay.lootValue(30_000, 100_000, 0, 5));
	}
}
