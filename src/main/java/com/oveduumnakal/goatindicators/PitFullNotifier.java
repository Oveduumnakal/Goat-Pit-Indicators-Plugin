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

import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.GameObject;
import net.runelite.client.Notifier;

/**
 * Fires a single notification when the goat pit fills up and needs emptying (#100).
 *
 * <p>The one moment that matters while filling a semi-AFK pit is when it reaches capacity, so this watches
 * for the pit crossing into full each tick and fires RuneLite's {@link Notifier} once — tray, sound and
 * flash all follow the user's configured {@link net.runelite.client.config.Notification}. It re-arms once a
 * pit drops back below full (emptied or re-spiking), so one fill yields exactly one alert rather than a
 * stream. "Full" reuses the same effectively-full test the menu swaps use (landed goats plus, optionally,
 * goats still in transit), so no new pit tracking is introduced. Ticked from {@link GoatIndicatorsPlugin}.
 *
 * <p>With the restock warning on, a fill reached while no spikes are carried adds a reminder to the same
 * notification (#103) rather than firing a second one, since emptying the pit uses up its spikes.
 */
@Singleton
class PitFullNotifier
{
	/** The base pit-full alert. */
	static final String FULL_MESSAGE = "Your goat pit is full.";

	/** Appended when no spikes are carried, since emptying the pit uses up its spikes. */
	static final String RESTOCK_REMINDER = "You have no spikes — grab one before emptying it.";

	private final Notifier notifier;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;
	private final GoatTransitTracker transitTracker;
	private final CarriedSpikes carriedSpikes;

	/** Whether the current fill has already been notified, so it fires once per fill rather than every tick. */
	private boolean notified;

	@Inject
	PitFullNotifier(Notifier notifier, GoatIndicatorsConfig config, GoatPitTracker tracker,
		GoatTransitTracker transitTracker, CarriedSpikes carriedSpikes)
	{
		this.notifier = notifier;
		this.config = config;
		this.tracker = tracker;
		this.transitTracker = transitTracker;
		this.carriedSpikes = carriedSpikes;
	}

	/**
	 * Checks the pit's fullness this tick and fires the notification on the transition into full, re-arming
	 * once it drops back below full. Run on the client thread from the plugin's game-tick handler.
	 */
	void onTick()
	{
		boolean full = allCatchingPitsFull();
		if (full && !notified)
		{
			notifier.notify(config.pitFullNotification(), message());
			notified = true;
		}
		else if (!full)
		{
			notified = false;
		}
	}

	/** The alert text, with a restock reminder when the warning is on and no spikes are carried. */
	private String message()
	{
		if (config.warnRestockSpikes() && carriedSpikes.count() <= 0)
			return FULL_MESSAGE + " " + RESTOCK_REMINDER;

		return FULL_MESSAGE;
	}

	/**
	 * Whether at least one spiked pit is loaded and every spiked pit is effectively full, so there is no room
	 * left to catch. Unspiked pits are skipped, since they cannot catch and so cannot be "full". In-transit
	 * goats count toward the trigger only when the matching config option is on.
	 */
	private boolean allCatchingPitsFull()
	{
		int inTransit = config.notifyCountInTransit() ? transitTracker.inTransitCount() : 0;
		boolean sawCatchingPit = false;
		for (GameObject pit : tracker.getPits())
		{
			GoatPitState state = tracker.stateOf(pit);
			if (state.needsSpikes())
				continue;

			sawCatchingPit = true;
			if (!TelegrabTargeting.effectivelyFull(state.getCount(), inTransit, state.getCapacity()))
				return false;
		}

		return sawCatchingPit;
	}
}
