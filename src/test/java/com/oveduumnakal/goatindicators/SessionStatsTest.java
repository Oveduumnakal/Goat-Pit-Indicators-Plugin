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
 * re-baselines, and a shrinking lifetime total cannot drive the counts negative. Also covers the fur/horn
 * tally: only rises at the pit count, drops book nothing, and a reset keeps the carried baseline.
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

	@Test
	public void rebaseKeepsSessionCatchesWhenTheLifetimeTotalIsReset()
	{
		stats.update(T0, 100, 5000);
		stats.update(T0.plus(Duration.ofMinutes(10)), 107, 5700);
		stats.rebaseCatches(0);
		stats.update(T0.plus(Duration.ofMinutes(20)), 2, 5900);

		assertEquals(9, stats.catches());
	}

	@Test
	public void rebaseBeforeTheSessionStartsIsIgnored()
	{
		stats.rebaseCatches(0);
		stats.update(T0, 50, 5000);

		assertEquals(0, stats.catches());
	}

	@Test
	public void firstInventoryReadingOnlyFixesTheBaseline()
	{
		stats.recordInventory(5, 12, true);

		assertEquals(0, stats.fur());
		assertEquals(0, stats.horn());
	}

	@Test
	public void risesAtThePitCountAsLoot()
	{
		stats.recordInventory(0, 0, true);
		stats.recordInventory(1, 3, true);
		stats.recordInventory(2, 6, true);

		assertEquals(2, stats.fur());
		assertEquals(6, stats.horn());
	}

	@Test
	public void risesAwayFromThePitAreIgnoredButMoveTheBaseline()
	{
		stats.recordInventory(0, 0, true);
		stats.recordInventory(20, 20, false);
		stats.recordInventory(21, 20, true);

		assertEquals(1, stats.fur());
		assertEquals(0, stats.horn());
	}

	@Test
	public void dropsBookNothing()
	{
		stats.recordInventory(10, 10, true);
		stats.recordInventory(0, 4, true);
		stats.recordInventory(1, 4, true);

		assertEquals(1, stats.fur());
		assertEquals(0, stats.horn());
	}

	@Test
	public void resetClearsLootButKeepsTheCarriedBaseline()
	{
		stats.recordInventory(0, 0, true);
		stats.recordInventory(3, 9, true);
		stats.reset();
		stats.recordInventory(4, 9, true);

		assertEquals(1, stats.fur());
		assertEquals(0, stats.horn());
	}
}
