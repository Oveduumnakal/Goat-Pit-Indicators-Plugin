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

/**
 * Sine "breathing" alpha for highlights, so an outline reads as deliberate rather than as scenery.
 *
 * <p>The same effect and rate Stockpile uses for its tracked-item highlights: a 1600&nbsp;ms period
 * swinging between 0.45 and 1.0 opacity, derived from the wall clock so it is frame-rate independent.
 */
final class Pulse
{
	private static final long PERIOD_MS = 1600L;
	private static final float MIN_ALPHA = 0.45f;
	private static final float MAX_ALPHA = 1.0f;

	private Pulse()
	{
	}

	/** Current alpha in {@code [MIN_ALPHA, MAX_ALPHA]}, phase-locked to the wall clock. */
	static float alpha()
	{
		return (float) (MIN_ALPHA + wave() * (MAX_ALPHA - MIN_ALPHA));
	}

	/** Sine in {@code [0, 1]} over {@link #PERIOD_MS}, phase-locked to the wall clock. */
	private static double wave()
	{
		double phase = (System.currentTimeMillis() % PERIOD_MS) / (double) PERIOD_MS;
		return (Math.sin(phase * 2 * Math.PI) + 1) / 2;
	}
}
