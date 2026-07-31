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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.oveduumnakal.goatindicators.GoatTransitTracker.Phase;
import org.junit.Test;

/**
 * Covers the lure phase machine that keeps a telegrabbed goat suppressed across its invisible walk to the
 * pit, using the confirmed in-game sequence flight to walk to jump.
 */
public class GoatTransitTrackerTest
{
	@Test
	public void flightStartsTransit()
	{
		assertEquals(Phase.FLIGHT, GoatTransitTracker.nextPhase(null, true, false));
	}

	@Test
	public void untrackedGoatWithNoMarkerStaysUntracked()
	{
		assertNull(GoatTransitTracker.nextPhase(null, false, false));
	}

	@Test
	public void walkAfterFlightStaysInTransitWithNoMarker()
	{
		assertEquals(Phase.LURED, GoatTransitTracker.nextPhase(Phase.FLIGHT, false, false));
	}

	@Test
	public void walkPersistsAcrossManyMarkerlessTicks()
	{
		assertEquals(Phase.LURED, GoatTransitTracker.nextPhase(Phase.LURED, false, false));
	}

	@Test
	public void jumpAnimationCountsAsTransit()
	{
		assertEquals(Phase.JUMPING, GoatTransitTracker.nextPhase(Phase.LURED, false, true));
	}

	@Test
	public void goatIsReleasedAfterJumpCompletes()
	{
		assertNull(GoatTransitTracker.nextPhase(Phase.JUMPING, false, false));
	}

	@Test
	public void freshFlightOutranksAStaleWalk()
	{
		assertEquals(Phase.FLIGHT, GoatTransitTracker.nextPhase(Phase.LURED, true, false));
	}

	@Test
	public void ownFlightIsAttributedToTheLocalPlayer()
	{
		assertTrue(GoatTransitTracker.ownedThisTick(Phase.FLIGHT, true));
	}

	@Test
	public void anotherPlayersFlightIsNotOwned()
	{
		assertFalse(GoatTransitTracker.ownedThisTick(Phase.FLIGHT, false));
	}

	@Test
	public void walkAndJumpDoNotEstablishOwnership()
	{
		assertFalse(GoatTransitTracker.ownedThisTick(Phase.LURED, true));
		assertFalse(GoatTransitTracker.ownedThisTick(Phase.JUMPING, true));
	}

	@Test
	public void anIdleGoatIsNeverOwned()
	{
		assertFalse(GoatTransitTracker.ownedThisTick(null, true));
	}
}
