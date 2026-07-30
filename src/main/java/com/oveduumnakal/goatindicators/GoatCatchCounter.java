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

import javax.inject.Singleton;

/**
 * Counts goats caught this session by watching the pit's count varbit ({@link GoatIds#COUNT_VARBIT_OVERRIDE}).
 *
 * <p>The game keeps no lifetime catch total: the count varbit only holds a pit's current fill and
 * resets to zero when the pit is emptied. So the total is built going forward, adding every upward
 * step of the varbit and ignoring the drop back to zero on emptying.
 *
 * <p>The counter must be {@link #seed(int) seeded} with the live varbit value on login before it will
 * count, so a pit that is already part-full when the player logs in does not register as fresh
 * catches. {@link #suspend()} drops that seed for a logout or scene teardown; the running total
 * survives until {@link #reset()} or the client restarts. All state is package-private, so the
 * decision logic can be unit-tested without a {@code Client}.
 */
@Singleton
class GoatCatchCounter
{
	private int total;

	/** Last observed count, or {@code -1} while unseeded (nothing is counted until seeded). */
	private int lastCount = -1;

	/** Goats caught since the last {@link #reset()} or client start. */
	int getTotal()
	{
		return total;
	}

	/**
	 * Seeds the baseline from the live count varbit, e.g. on login. Counting resumes from this value,
	 * so the goats already sitting in the pit are not counted as fresh catches.
	 *
	 * @param currentCount the pit's current count varbit value
	 */
	void seed(int currentCount)
	{
		lastCount = Math.max(0, currentCount);
	}

	/** Drops the baseline so nothing is counted until the next {@link #seed(int)}. Keeps the total. */
	void suspend()
	{
		lastCount = -1;
	}

	/** Clears the running total and the baseline, for a manual session reset. */
	void reset()
	{
		total = 0;
		lastCount = -1;
	}

	/**
	 * Records a new count varbit value, adding any upward step to the total. A drop (the pit being
	 * emptied) advances the baseline without counting. Does nothing while unseeded.
	 *
	 * @param newCount the pit's count varbit value after the change
	 */
	void onCountChanged(int newCount)
	{
		if (lastCount < 0)
		{
			return;
		}
		if (newCount > lastCount)
		{
			total += newCount - lastCount;
		}
		lastCount = Math.max(0, newCount);
	}
}
