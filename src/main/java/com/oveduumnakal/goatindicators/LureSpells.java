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

import net.runelite.api.Client;

/**
 * Answers whether the player is set up to lure a goat into the pit by either supported spell — Telekinetic
 * Grab or Dark Lure. Both drag the goat toward the caster across the pit, so the highlight and targeting
 * geometry treat them alike; only the cast requirements differ, and each spell owns its own. The book the
 * player is on is detected automatically, so no config choice between the two is needed.
 *
 * <p>The answer is cached per game tick (#129). It is asked every frame by the menu swapper and the goat
 * highlight, and each check scans the inventory, the rune pouch and the spellbook, yet everything it depends on
 * — Magic level, spellbook, runes, worn staff, quest state — only changes on a tick.
 */
@Singleton
class LureSpells
{
	private final Client client;
	private final TelekineticGrab telekineticGrab;
	private final DarkLure darkLure;

	/** Tick the cached answer was computed on, or {@code -1} before the first check. */
	private int cachedTick = -1;

	/** Whether either spell could be cast, as of {@link #cachedTick}. */
	private boolean cachedCanLure;

	@Inject
	LureSpells(Client client, TelekineticGrab telekineticGrab, DarkLure darkLure)
	{
		this.client = client;
		this.telekineticGrab = telekineticGrab;
		this.darkLure = darkLure;
	}

	/** Whether the player can cast either lure spell right now, so goats are worth highlighting. */
	boolean canLure()
	{
		int tick = client.getTickCount();
		if (tick != cachedTick)
		{
			cachedCanLure = telekineticGrab.canCast() || darkLure.canCast();
			cachedTick = tick;
		}

		return cachedCanLure;
	}
}
