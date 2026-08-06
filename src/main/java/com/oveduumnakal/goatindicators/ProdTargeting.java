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
 * Pure geometry for deciding which goats are worth a Cattleprod into the pit.
 *
 * <p>A prodded goat travels a set distance toward the trap before losing interest, so a goat is prodable
 * when it sits within a configurable number of tiles of a catching pit. The rules take primitives so they
 * can be unit-tested without a {@code Client}; the overlay supplies the live goat and pit-footprint
 * coordinates and the range from config.
 */
final class ProdTargeting
{
	/**
	 * The furthest a goat may be from the pit and still be prodded in before it loses interest. Measured
	 * in-game by prodding goats at known tile distances and logging which reached the pit: every prod at a
	 * Chebyshev distance of 7 or fewer landed, every prod at 8 or more failed.
	 */
	static final int MAX_RANGE = 7;

	private ProdTargeting()
	{
	}

	/**
	 * Whether a goat that many tiles from the pit is close enough to prod in before it loses interest.
	 *
	 * @param distance the Chebyshev tile distance from goat to the nearest pit tile
	 * @return true when the goat is within {@link #MAX_RANGE} tiles of the pit
	 */
	static boolean withinProdRange(int distance)
	{
		return distance >= 0 && distance <= MAX_RANGE;
	}

	/**
	 * Clamps a goat coordinate onto the pit's span on one axis, giving the coordinate of the nearest pit
	 * tile to the goat along that axis. Used to measure distance to the pit edge rather than its far corner.
	 *
	 * @param coord the goat coordinate on this axis
	 * @param min   the pit footprint minimum on this axis
	 * @param max   the pit footprint maximum on this axis
	 * @return the nearest pit-tile coordinate on this axis
	 */
	static int clampToPit(int coord, int min, int max)
	{
		return Math.max(min, Math.min(max, coord));
	}

	/**
	 * Whether the pit lies in the direction a prod would shove the goat, so it can be prodded in from the
	 * player's current tile without first repositioning. A prod pushes the goat directly away from the
	 * player, snapped to one of the eight compass directions; the goat then curves the last tile or two into
	 * a pit that sits that way rather than needing to land dead on the push line. The test is therefore a
	 * forgiving half-plane one: the pit counts when the step from the goat toward its nearest pit tile has a
	 * positive component along the push direction. A player standing on the goat's own tile has no push
	 * direction and yields false. Range is bounded separately by {@link #withinProdRange}.
	 *
	 * @param px   player world x
	 * @param py   player world y
	 * @param gx   goat world x
	 * @param gy   goat world y
	 * @param minX pit footprint minimum world x
	 * @param minY pit footprint minimum world y
	 * @param maxX pit footprint maximum world x
	 * @param maxY pit footprint maximum world y
	 * @return true when a prod from the player's tile shoves the goat toward the pit
	 */
	static boolean pitInPushDirection(int px, int py, int gx, int gy, int minX, int minY, int maxX, int maxY)
	{
		int dx = Integer.signum(gx - px);
		int dy = Integer.signum(gy - py);
		if (dx == 0 && dy == 0)
			return false;

		int toPitX = clampToPit(gx, minX, maxX) - gx;
		int toPitY = clampToPit(gy, minY, maxY) - gy;
		return toPitX * dx + toPitY * dy > 0;
	}
}
