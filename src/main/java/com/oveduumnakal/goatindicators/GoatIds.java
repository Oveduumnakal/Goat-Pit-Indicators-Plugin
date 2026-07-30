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
	/**
	 * Smallest and largest goat pit capacity. The pit's capacity is not fixed: it scales with the
	 * player's Hunter level, from {@code 16} at level 60 up to {@code 24} at level 93+. See
	 * {@link #capacityForHunterLevel(int)}.
	 */
	static final int MIN_CAPACITY = 16;
	static final int MAX_CAPACITY = 24;

	/**
	 * How many goats a pit holds when full at the given Hunter level. Capacity rises in steps with the
	 * level, per the OSRS Wiki (<a href="https://oldschool.runescape.wiki/w/Goat_pit">Goat pit</a>):
	 *
	 * <pre>
	 *   Hunter 60-69 -&gt; 16    Hunter 85-92 -&gt; 22
	 *   Hunter 70-76 -&gt; 18    Hunter 93+   -&gt; 24
	 *   Hunter 77-84 -&gt; 20
	 * </pre>
	 *
	 * The wiki lists levels 68-69 as unconfirmed; they are treated as {@code 16} (the last confirmed
	 * value below 70) here. Levels below 60 cannot use the pit, so they also return {@link #MIN_CAPACITY}.
	 *
	 * @param hunterLevel the player's Hunter level (real, unboosted)
	 * @return the pit capacity for that level, always within {@link #MIN_CAPACITY}..{@link #MAX_CAPACITY}
	 */
	static int capacityForHunterLevel(int hunterLevel)
	{
		if (hunterLevel >= 93)
		{
			return 24;
		}
		if (hunterLevel >= 85)
		{
			return 22;
		}
		if (hunterLevel >= 77)
		{
			return 20;
		}
		if (hunterLevel >= 70)
		{
			return 18;
		}
		return MIN_CAPACITY;
	}

	/**
	 * Exact game-object ids of the goat pit, the object the overlay draws its footprint on. Confirmed
	 * in a developer-mode session (see {@code docs/discovery.md}): the pit renders as game object
	 * {@code 62343} (id {@code 19750} is the ground object beneath it, which the tracker does not use).
	 * Name matching via {@link #PIT_NAME_FRAGMENT} still runs as a fallback if this set is emptied.
	 */
	static final Set<Integer> PIT_OBJECT_IDS =
		Collections.unmodifiableSet(new HashSet<>(Arrays.asList(62343)));

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

	/**
	 * Spot-anim (graphic) a goat carries while it is being lured toward the pit by a telegrab. Confirmed
	 * in-game: it appears on a goat the moment it is grabbed and clears when the goat lands, and the game
	 * never shows it on more than {@link #MAX_GOATS_IN_TRANSIT} goats at once.
	 */
	static final int IN_TRANSIT_SPOTANIM = 144;

	/**
	 * How many goats can be lured toward the pit at once. The trap only takes two in transit, so a third
	 * grab will not land until one of the two falls in. Matches the OSRS Wiki's "2 will fall into the
	 * trap".
	 */
	static final int MAX_GOATS_IN_TRANSIT = 2;

	private GoatIds()
	{
		throw new AssertionError("constants holder");
	}
}
