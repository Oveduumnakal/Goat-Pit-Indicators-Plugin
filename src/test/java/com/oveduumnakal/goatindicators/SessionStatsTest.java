/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the session tally: the first sample fixes the baseline, later samples report the catch/XP delta and
 * elapsed time, the per-hour rates scale by elapsed time (and stay zero before any passes), a reset
 * re-baselines, and a shrinking lifetime total cannot drive the counts negative.
 */
public class SessionStatsTest
{
	private static final Instant T0 = Instant.ofEpochSecond(1_000_000);

	private final SessionStats stats = new SessionStats();

	@Test
	public void firstSampleFixesTheBaselineAndReportsZero()
	{
		stats.update(T0, 100, 5000);

		assertTrue(stats.started());
		assertEquals(0, stats.catches());
		assertEquals(0, stats.xpGained());
		assertEquals(Duration.ZERO, stats.elapsed());
	}

	@Test
	public void laterSamplesReportTheDeltaFromTheBaseline()
	{
		stats.update(T0, 100, 5000);
		stats.update(T0.plus(Duration.ofHours(1)), 105, 5500);

		assertEquals(5, stats.catches());
		assertEquals(500, stats.xpGained());
		assertEquals(Duration.ofHours(1), stats.elapsed());
	}

	@Test
	public void ratesScaleByElapsedTime()
	{
		stats.update(T0, 100, 5000);
		stats.update(T0.plus(Duration.ofMinutes(30)), 105, 5500);

		assertEquals(10, stats.catchesPerHour());
		assertEquals(1000, stats.xpPerHour());
	}

	@Test
	public void ratesAreZeroBeforeAnyTimeElapses()
	{
		stats.update(T0, 100, 5000);

		assertEquals(0, stats.catchesPerHour());
		assertEquals(0, stats.xpPerHour());
	}

	@Test
	public void resetStartsAFreshBaseline()
	{
		stats.update(T0, 100, 5000);
		stats.update(T0.plus(Duration.ofHours(1)), 110, 6000);

		stats.reset();
		assertFalse(stats.started());

		stats.update(T0.plus(Duration.ofHours(2)), 110, 6000);
		assertEquals(0, stats.catches());
		assertEquals(0, stats.xpGained());
	}

	@Test
	public void neverReportsNegativeCountsIfTheLifetimeTotalShrinks()
	{
		stats.update(T0, 100, 5000);
		stats.update(T0.plus(Duration.ofHours(1)), 40, 4000);

		assertEquals(0, stats.catches());
		assertEquals(0, stats.xpGained());
	}
}
