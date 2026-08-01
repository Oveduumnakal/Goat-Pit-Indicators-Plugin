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
 * A snapshot of one goat pit: how many goats it holds and whether it is spiked.
 *
 * <p>Deliberately free of RuneLite types so the fullness and "needs spikes" rules can be unit
 * tested without a client. Capacity is not fixed — it scales with Hunter level (see
 * {@link GoatIds#capacityForHunterLevel(int)}) — so it is passed in per snapshot. The count is
 * clamped into {@code 0 .. capacity} on construction, so a varbit that turns out to carry an
 * unexpected value can never produce a label like {@code 37 / 20}.
 */
final class GoatPitState
{
	private final int count;
	private final boolean spiked;
	private final int capacity;

	GoatPitState(int count, boolean spiked, int capacity)
	{
		this.capacity = Math.max(GoatIds.MIN_CAPACITY, Math.min(GoatIds.MAX_CAPACITY, capacity));
		this.count = Math.max(0, Math.min(this.capacity, count));
		this.spiked = spiked;
	}

	/** Number of goats currently in the pit, clamped to the pit's capacity. */
	int getCount()
	{
		return count;
	}

	/** How many goats this pit holds when full, for the player's current Hunter level. */
	int getCapacity()
	{
		return capacity;
	}

	/** Whether the pit currently has spikes set. */
	boolean isSpiked()
	{
		return spiked;
	}

	/** Whether the pit is at capacity and cannot trap any more goats. */
	boolean isFull()
	{
		return count >= capacity;
	}

	/** Whether the pit holds no goats at all. */
	boolean isEmpty()
	{
		return count == 0;
	}

	/**
	 * Whether the pit is missing its spikes and so cannot catch anything until they are re-added. This
	 * is simply the unspiked state: on this pit spikes are consumed when it is emptied, so an unspiked
	 * pit always needs player action regardless of how many goats it currently holds.
	 */
	boolean needsSpikes()
	{
		return !spiked;
	}

	/** The overlay label, e.g. {@code "12 / 20"} (the denominator is this pit's capacity). */
	String label()
	{
		return count + " / " + capacity;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof GoatPitState))
			return false;

		GoatPitState that = (GoatPitState) other;
		return count == that.count && spiked == that.spiked && capacity == that.capacity;
	}

	@Override
	public int hashCode()
	{
		return (count * 31 + capacity) * 31 + (spiked ? 1 : 0);
	}

	@Override
	public String toString()
	{
		return "GoatPitState[" + label() + (spiked ? ", spiked]" : ", unspiked]");
	}
}
