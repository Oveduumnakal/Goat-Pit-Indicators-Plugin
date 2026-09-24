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

import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;

/**
 * Nudges the player when catching has stalled at the pit (#105): nothing has changed for the configured time
 * while a goat pit is loaded — no catch, no goat on its way in, no Hunter XP, no spikes added or pit emptied.
 *
 * <p>Any of those changes counts as activity and restarts the timer, so the nudge fires once per stall and
 * re-arms on the next activity. That also keeps it from doubling up with the pit-full alert: the pit filling
 * is itself a change, so the earliest this can follow it is a full idle window later — which is exactly the
 * "full pit left un-emptied" case worth a second ping. Leaving the pit area pauses the watch rather than
 * counting as idle. The message names the likely cause: a full pit, a pit without spikes, or no catches.
 * Ticked from {@link GoatIndicatorsPlugin}.
 */
@Singleton
class IdleNotifier
{
	/** Sent when the pit has sat full through the idle window. */
	static final String FULL_MESSAGE = "Your goat pit has been full for a while — empty it.";

	/** Sent when the pit has sat unspiked through the idle window. */
	static final String SPIKES_MESSAGE = "Your goat pit still needs spikes.";

	/** Sent when a catching pit has seen no activity through the idle window. */
	static final String STALLED_MESSAGE = "No goats caught in a while.";

	/** Length of one game tick in milliseconds, for turning the configured seconds into ticks. */
	private static final int TICK_MS = 600;

	private final Notifier notifier;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;
	private final GoatTransitTracker transitTracker;

	/** The activity signature seen last tick, or {@code null} before the first reading or after a pause. */
	private Long lastSignature;

	/** Tick of the last activity, which the idle window is measured from. */
	private int lastActivityTick;

	/** Whether the current stall has already been nudged, so it fires once per stall. */
	private boolean nudged;

	@Inject
	IdleNotifier(Notifier notifier, GoatIndicatorsConfig config, GoatPitTracker tracker,
		GoatTransitTracker transitTracker)
	{
		this.notifier = notifier;
		this.config = config;
		this.tracker = tracker;
		this.transitTracker = transitTracker;
	}

	/**
	 * Checks for a stall this tick and fires the nudge when the idle window has passed without activity. Run on
	 * the client thread from the plugin's game-tick handler.
	 *
	 * @param tick the client's current tick count
	 * @param hunterXp the player's current Hunter experience
	 */
	void onTick(int tick, long hunterXp)
	{
		Notification notification = config.idleNotification();
		GoatPitState state = pitState();
		if (notification == null || !notification.isEnabled() || state == null)
		{
			lastSignature = null;
			return;
		}

		long signature = signature(state, transitTracker.inTransitCount(), hunterXp);
		if (stalled(tick, signature, thresholdTicks(config.idleThresholdSeconds())))
			notifier.notify(notification, messageFor(state));
	}

	/**
	 * Advances the idle watch with this tick's signature and reports whether the nudge is due now. A new
	 * signature is activity and restarts the window; the nudge is due once, when the window first elapses.
	 *
	 * @param tick the current tick
	 * @param signature this tick's activity signature
	 * @param thresholdTicks the idle window in ticks
	 * @return true exactly once per stall, on the tick the window elapses
	 */
	boolean stalled(int tick, long signature, int thresholdTicks)
	{
		if (lastSignature == null || lastSignature != signature)
		{
			lastSignature = signature;
			lastActivityTick = tick;
			nudged = false;
			return false;
		}

		if (nudged || tick - lastActivityTick < thresholdTicks)
			return false;

		nudged = true;
		return true;
	}

	/**
	 * Packs everything that counts as activity into one value, so a change in any of them reads as activity.
	 *
	 * @param state the pit's state (count and spikes)
	 * @param inTransit goats on their way in
	 * @param hunterXp current Hunter experience
	 * @return the combined signature
	 */
	static long signature(GoatPitState state, int inTransit, long hunterXp)
	{
		long pit = state.getCount() * 2L + (state.isSpiked() ? 1 : 0);
		return ((hunterXp * 64 + inTransit) * 64 + pit);
	}

	/**
	 * The configured idle window in whole ticks, rounded up so the nudge never fires early.
	 *
	 * @param seconds the idle window in seconds
	 * @return the window in ticks, at least one
	 */
	static int thresholdTicks(int seconds)
	{
		return Math.max(1, (seconds * 1000 + TICK_MS - 1) / TICK_MS);
	}

	/**
	 * The nudge text for the pit's state: empty a full pit, spike an unspiked one, or otherwise a plain stall.
	 *
	 * @param state the pit's state when the nudge fires
	 * @return the message to send
	 */
	static String messageFor(GoatPitState state)
	{
		if (state.isFull())
			return FULL_MESSAGE;

		if (state.needsSpikes())
			return SPIKES_MESSAGE;

		return STALLED_MESSAGE;
	}

	/** The loaded pit's state, or {@code null} when no pit is in the scene (away from the pit area). */
	private GoatPitState pitState()
	{
		return tracker.getPits()
			.stream()
			.findFirst()
			.map(tracker::stateOf)
			.orElse(null);
	}
}
