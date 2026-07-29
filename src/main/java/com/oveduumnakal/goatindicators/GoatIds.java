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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Every game-side identifier and name fragment the plugin recognises, in one place.
 *
 * <p>The goat pit is not present in the {@code net.runelite.api.gameval} constants shipped with the
 * RuneLite API, so nothing here can be imported from there. The plugin therefore identifies the pit
 * <em>structurally</em> rather than by a hard-coded id: it matches the object's name, and reads the
 * count from the varbit the object composition itself declares
 * ({@link net.runelite.api.ObjectComposition#getVarbitId()}). That needs no known id and survives a
 * re-release of the content under new ids.
 *
 * <p>{@link #PIT_OBJECT_IDS} is an optional allowlist. Leave it empty to use name matching; fill it
 * in from a developer-mode session (see {@code docs/discovery.md}) to pin detection to exact ids.
 */
final class GoatIds
{
	/** How many goats a pit holds when full. Fixed by the content, not configurable. */
	static final int PIT_CAPACITY = 20;

	/**
	 * Exact object ids of the goat pit. Confirmed in a developer-mode session (see
	 * {@code docs/discovery.md}); {@code 19750} is the pit object. Name matching via
	 * {@link #PIT_NAME_FRAGMENT} still runs as a fallback if this set is emptied.
	 */
	static final Set<Integer> PIT_OBJECT_IDS =
		Collections.unmodifiableSet(new HashSet<>(Arrays.asList(19750)));

	/**
	 * Lower-cased fragment matched against {@link net.runelite.api.ObjectComposition#getName()} when
	 * {@link #PIT_OBJECT_IDS} is empty.
	 */
	static final String PIT_NAME_FRAGMENT = "goat pit";

	/**
	 * Lower-cased fragment matched against {@link net.runelite.api.NPC#getName()} when counting goats
	 * inside the pit footprint, used only if the pit exposes no varbit.
	 */
	static final String GOAT_NAME_FRAGMENT = "goat";

	/**
	 * Lower-cased menu action offered by an unspiked pit. Its presence on the pit's current
	 * composition is what marks the pit as needing spikes; a spiked pit does not offer it.
	 */
	static final String ADD_SPIKES_ACTION = "add spikes";

	/**
	 * Varbit holding the goat count. Object {@code 19750} carries no composition varbit, so the count
	 * lives in this player varbit (part of VarPlayer 5706) instead. Confirmed in a developer-mode
	 * session stepping 0 → 1 as a goat was caught; see {@code docs/discovery.md}. Set to {@code -1} to
	 * fall back to the object's declared varbit and then to NPC counting.
	 */
	static final int COUNT_VARBIT_OVERRIDE = 15725;

	/**
	 * Varbit holding the spikes state ({@code 0} = unspiked, {@code 1} = spiked), also part of
	 * VarPlayer 5706. Observed dropping to 0 when the pit was emptied and returning to 1 when spikes
	 * were re-added. Set to {@code -1} to fall back to detecting {@link #ADD_SPIKES_ACTION}.
	 */
	static final int SPIKES_VARBIT_OVERRIDE = 15724;

	private GoatIds()
	{
		throw new AssertionError("constants holder");
	}
}
