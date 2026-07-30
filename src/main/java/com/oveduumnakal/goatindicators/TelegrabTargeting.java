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
 * Pure geometry for deciding which goats are worth a Telekinetic Grab into the pit.
 *
 * <p>Two rules live here, both taking primitives so they can be unit-tested without a {@code Client}.
 * {@link #withinCastRange(int)} is the spell's 10-tile reach. {@link #oppositeSideOfPit} encodes the
 * lure rule from the Goat hunting guide: the caster must stand on the far side of the pit so the goat
 * is dragged across it into the trap. Concretely, the pit edge nearest the player is the cut line, and
 * a goat is a target only when it sits on that line or beyond it — never on the player's own side.
 *
 * <p>Range is necessary but not sufficient: Telekinetic Grab also needs line of sight to the goat, which
 * depends on the scene collision map and so cannot be decided here. The overlay pairs this range check
 * with a live line-of-sight test before highlighting a goat.
 */
final class TelegrabTargeting
{
	/**
	 * Telekinetic Grab's maximum cast range in tiles. Range alone does not make a goat grabbable — the
	 * spell also requires line of sight, checked separately against the live scene.
	 */
	static final int MAX_RANGE = 10;

	private TelegrabTargeting()
	{
	}

	/**
	 * Whether a goat that many tiles away can be grabbed without the player moving.
	 *
	 * @param distance the Chebyshev tile distance from player to goat
	 * @return true when the goat is within cast range
	 */
	static boolean withinCastRange(int distance)
	{
		return distance >= 0 && distance <= MAX_RANGE;
	}

	/**
	 * Whether the pit is effectively full for the purpose of a fresh lure: the goats already in it plus
	 * those in transit toward it have reached its capacity, so another cast would be wasted. This is a
	 * stricter test than {@code isFull()} — it counts the goats still on their way in.
	 *
	 * @param goatsInPit     goats currently in the pit
	 * @param goatsInTransit goats currently being lured toward the pit
	 * @param limit          the pit's capacity
	 * @return true when the pit's count plus in-transit goats meet or exceed the capacity
	 */
	static boolean effectivelyFull(int goatsInPit, int goatsInTransit, int limit)
	{
		return goatsInPit + goatsInTransit >= limit;
	}

	/**
	 * Whether the goat is on the opposite side of the pit from the player, so a grab lures it across the
	 * pit and into the trap. The player's position picks the axis: whichever pit edge they stand beyond
	 * becomes the cut line, and only goats at or past the pit on that axis qualify. A player level with
	 * the pit on both axes has no clear far side and yields false.
	 *
	 * @param minX pit footprint minimum world x (west edge)
	 * @param minY pit footprint minimum world y (south edge)
	 * @param maxX pit footprint maximum world x (east edge)
	 * @param maxY pit footprint maximum world y (north edge)
	 * @param px player world x
	 * @param py player world y
	 * @param gx goat world x
	 * @param gy goat world y
	 * @return true when player and goat sit on opposite sides of the pit
	 */
	static boolean oppositeSideOfPit(int minX, int minY, int maxX, int maxY, int px, int py, int gx, int gy)
	{
		if (py < minY)
		{
			return gy >= minY;
		}
		if (py > maxY)
		{
			return gy <= maxY;
		}
		if (px < minX)
		{
			return gx >= minX;
		}
		if (px > maxX)
		{
			return gx <= maxX;
		}
		return false;
	}
}
