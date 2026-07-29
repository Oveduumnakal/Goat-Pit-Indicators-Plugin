/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Covers the spiked-to-unspiked transition rule that drives the needs-spikes notification. */
public class SpikesNotificationTest
{
	@Test
	public void firesOnSpikedToUnspikedWithPitLoaded()
	{
		assertTrue(GoatIndicatorsPlugin.shouldNotifyNeedsSpikes(true, false, true));
	}

	@Test
	public void silentWhenNoPitIsLoaded()
	{
		assertFalse(GoatIndicatorsPlugin.shouldNotifyNeedsSpikes(true, false, false));
	}

	@Test
	public void silentWhenSpikesAreAdded()
	{
		assertFalse(GoatIndicatorsPlugin.shouldNotifyNeedsSpikes(false, true, true));
	}

	@Test
	public void silentWhenStateIsUnchanged()
	{
		assertFalse(GoatIndicatorsPlugin.shouldNotifyNeedsSpikes(true, true, true));
		assertFalse(GoatIndicatorsPlugin.shouldNotifyNeedsSpikes(false, false, true));
	}
}
