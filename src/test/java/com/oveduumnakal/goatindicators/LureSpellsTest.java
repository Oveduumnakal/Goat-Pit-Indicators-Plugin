/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import net.runelite.api.Client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the per-tick cache on the lure check (#129): repeat calls within a tick reuse the answer without
 * re-checking either spell, and a new tick re-checks and picks up a change.
 */
public class LureSpellsTest
{
	private final Client client = mock(Client.class);
	private final TelekineticGrab telekineticGrab = mock(TelekineticGrab.class);
	private final DarkLure darkLure = mock(DarkLure.class);

	private final LureSpells lureSpells = new LureSpells(client, telekineticGrab, darkLure);

	@Test
	public void repeatCallsWithinATickReuseTheAnswer()
	{
		when(client.getTickCount()).thenReturn(10);
		when(telekineticGrab.canCast()).thenReturn(false);
		when(darkLure.canCast()).thenReturn(true);

		assertTrue(lureSpells.canLure());
		assertTrue(lureSpells.canLure());
		assertTrue(lureSpells.canLure());

		verify(telekineticGrab, times(1)).canCast();
		verify(darkLure, times(1)).canCast();
	}

	@Test
	public void aNewTickRechecksAndPicksUpAChange()
	{
		when(client.getTickCount()).thenReturn(10);
		when(telekineticGrab.canCast()).thenReturn(true);
		assertTrue(lureSpells.canLure());

		when(client.getTickCount()).thenReturn(11);
		when(telekineticGrab.canCast()).thenReturn(false);
		when(darkLure.canCast()).thenReturn(false);
		assertFalse(lureSpells.canLure());
	}
}
