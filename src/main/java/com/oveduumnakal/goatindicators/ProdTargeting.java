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
 * Pure geometry for deciding which goats are worth a Cattleprod into the pit, and where to stand to do it.
 *
 * <p>A prod pushes a goat one tile <em>directly away</em> from the player, and a prodded goat travels a set
 * distance before losing interest. So a goat is prodable when it sits within {@link #MAX_RANGE} tiles of a
 * catching pit, and the tile to stand on is the one just beyond the goat on the far side from the pit — prod
 * from there and the goat steps toward the trap. All three rules take primitives so they can be unit-tested
 * without a {@code Client}; the overlay supplies the live goat and pit-footprint coordinates.
 */
final class ProdTargeting
{
	/**
	 * How many tiles from a catching pit a goat may be and still be prodded in before it loses interest.
	 * Placeholder pending a developer-mode / wiki confirmation of the goat's prod-travel distance (see
	 * {@code docs/discovery.md}); it is a plain tunable constant.
	 */
	static final int MAX_RANGE = 5;

	private ProdTargeting()
	{
	}

	/**
	 * Whether a goat that many tiles from the pit is close enough to prod in before it loses interest.
	 *
	 * @param distance the Chebyshev tile distance from goat to the nearest pit tile
	 * @return true when the goat is within prod range
	 */
	static boolean withinProdRange(int distance)
	{
		return distance >= 0 && distance <= MAX_RANGE;
	}

	/**
	 * Clamps a goat coordinate onto the pit's span on one axis, giving the coordinate of the nearest pit
	 * tile to the goat along that axis. Used to measure distance to, and direction from, the pit edge
	 * rather than its far corner.
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
	 * The stand coordinate on one axis: one tile beyond the goat on the side away from the pit, so a prod
	 * pushes the goat back toward the pit. When the goat is already level with the pit on this axis the
	 * coordinate is unchanged, so an axis-aligned goat yields an orthogonal stand tile and an offset goat a
	 * diagonal one.
	 *
	 * @param goatCoord the goat coordinate on this axis
	 * @param pitCoord  the nearest pit-tile coordinate on this axis (see {@link #clampToPit(int, int, int)})
	 * @return the stand-tile coordinate on this axis
	 */
	static int standTile(int goatCoord, int pitCoord)
	{
		return goatCoord + Integer.signum(goatCoord - pitCoord);
	}
}
