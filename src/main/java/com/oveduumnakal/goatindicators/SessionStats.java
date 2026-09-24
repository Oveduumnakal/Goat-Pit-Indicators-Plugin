/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oveduumnakal.goatindicators;

import java.time.Duration;
import java.time.Instant;
import javax.inject.Singleton;

/**
 * Tracks this catching session's running figures (#101): goats caught, Hunter XP gained, elapsed time, and
 * the per-hour rates derived from them.
 *
 * <p>The plugin feeds a fresh sample each game tick via {@link #update(Instant, int, long)}; the first
 * sample after a {@link #reset()} fixes the session baseline (the lifetime catch total and Hunter XP at that
 * moment), and every later sample reports the delta from it. Rates are the delta scaled by elapsed wall time.
 * Kept free of the client and the clock so it is unit-testable — the caller supplies "now" and the readings.
 */
@Singleton
class SessionStats
{
	private Instant start;
	private int startCatches;
	private long startXp;

	private Duration elapsed = Duration.ZERO;
	private int catches;
	private long xpGained;

	/**
	 * Records a fresh sample. The first sample after construction or {@link #reset()} fixes the baseline;
	 * later samples report the catch and XP gained since, and the time elapsed.
	 *
	 * @param now the current instant
	 * @param catchTotal the lifetime goats-caught total (its session delta is the catch count)
	 * @param hunterXp the current total Hunter experience
	 */
	void update(Instant now, int catchTotal, long hunterXp)
	{
		if (start == null)
		{
			start = now;
			startCatches = catchTotal;
			startXp = hunterXp;
		}

		elapsed = Duration.between(start, now);
		catches = Math.max(0, catchTotal - startCatches);
		xpGained = Math.max(0, hunterXp - startXp);
	}

	/**
	 * Re-anchors the catch baseline after the lifetime total is reset out from under the session, so the
	 * catches already counted this session survive rather than dropping to zero until the total climbs back.
	 *
	 * @param catchTotal the lifetime goats-caught total just after the reset
	 */
	void rebaseCatches(int catchTotal)
	{
		if (start != null)
			startCatches = catchTotal - catches;
	}

	/** Clears the session so the next {@link #update(Instant, int, long)} starts a fresh one. */
	void reset()
	{
		start = null;
		elapsed = Duration.ZERO;
		catches = 0;
		xpGained = 0;
	}

	/** @return whether a session is running (at least one sample has been recorded since the last reset). */
	boolean started()
	{
		return start != null;
	}

	/** @return the time elapsed since the session began. */
	Duration elapsed()
	{
		return elapsed;
	}

	/** @return goats caught this session. */
	int catches()
	{
		return catches;
	}

	/** @return Hunter experience gained this session. */
	long xpGained()
	{
		return xpGained;
	}

	/** @return goats caught per hour at the current rate, or 0 before any time has elapsed. */
	int catchesPerHour()
	{
		return (int) perHour(catches);
	}

	/** @return Hunter experience per hour at the current rate, or 0 before any time has elapsed. */
	long xpPerHour()
	{
		return perHour(xpGained);
	}

	/** @return {@code amount} scaled to an hourly rate over the elapsed time, or 0 when no time has elapsed. */
	private long perHour(long amount)
	{
		long seconds = elapsed.getSeconds();
		if (seconds <= 0)
			return 0;

		return amount * 3600L / seconds;
	}
}
