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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure eight-directional pathfinding over the walkable tiles, used to predict which tile clicking a goat
 * would actually walk the player to before the prod lands.
 *
 * <p>Prodding a goat into the pit only works when the player stands on the right side of the goat, but a
 * click first paths the player to a tile adjacent to the goat — and an obstacle in the way can route them
 * to the wrong side. This class runs a breadth-first flood from the player's tile across tiles the game
 * says are walkable ({@link StepFn}), then picks the goat-adjacent tile the flood reaches soonest as the
 * tile the player would end up prodding from. The game's own interaction pathing is approximated, not
 * mirrored exactly: shortest reach with a fixed tie-break order stands in for its internal rules.
 *
 * <p>Everything here takes primitives and a {@link StepFn} so it can be unit-tested against a synthetic
 * grid without a {@code Client}; the overlay supplies the live collision-backed step test.
 */
final class ProdPathing
{
	/**
	 * Tests whether a one-tile actor at {@code (x, y)} may step by {@code (dx, dy)} — the walkability the
	 * flood expands across. Supplied by the caller so the geometry stays free of the client.
	 */
	@FunctionalInterface
	interface StepFn
	{
		/**
		 * Whether a step from {@code (x, y)} in direction {@code (dx, dy)} is walkable.
		 *
		 * @param x  the tile world x
		 * @param y  the tile world y
		 * @param dx the step's x delta, one of -1, 0, 1
		 * @param dy the step's y delta, one of -1, 0, 1
		 * @return true when the step is allowed
		 */
		boolean canStep(int x, int y, int dx, int dy);
	}

	/**
	 * The eight neighbour deltas, in the order ties are broken when two tiles are reached in the same number
	 * of steps: cardinals first (west, east, south, north), then diagonals. This is a stand-in for the
	 * game's own interaction tie-break, which is not exposed.
	 */
	private static final int[][] NEIGHBOURS =
	{
		{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
	};

	private ProdPathing()
	{
	}

	/**
	 * Floods outward from the origin tile, recording the fewest steps to reach every walkable tile within
	 * {@code maxRadius} Chebyshev tiles. Diagonal steps cost one, matching in-game movement.
	 *
	 * @param originX   the player's tile world x
	 * @param originY   the player's tile world y
	 * @param step      the walkability test between adjacent tiles
	 * @param maxRadius the furthest Chebyshev distance from the origin to flood
	 * @return a map from packed {@link #key(int, int)} tile to its step distance from the origin
	 */
	static Map<Long, Integer> reachDistances(int originX, int originY, StepFn step, int maxRadius)
	{
		Map<Long, Integer> distance = new HashMap<>();
		Deque<int[]> queue = new ArrayDeque<>();
		distance.put(key(originX, originY), 0);
		queue.add(new int[]{originX, originY});
		while (!queue.isEmpty())
		{
			int[] tile = queue.poll();
			int here = distance.get(key(tile[0], tile[1]));
			if (here >= maxRadius)
				continue;

			for (int[] dir : NEIGHBOURS)
			{
				int nx = tile[0] + dir[0];
				int ny = tile[1] + dir[1];
				if (distance.containsKey(key(nx, ny)) || !step.canStep(tile[0], tile[1], dir[0], dir[1]))
					continue;

				distance.put(key(nx, ny), here + 1);
				queue.add(new int[]{nx, ny});
			}
		}

		return distance;
	}

	/**
	 * The goat-adjacent tile the player would path to, or {@code null} when no tile beside the goat is
	 * reachable. Of the eight tiles around the goat, the one reached in the fewest steps wins; equal steps
	 * are broken by {@link #NEIGHBOURS} order.
	 *
	 * @param goatX    the goat's tile world x
	 * @param goatY    the goat's tile world y
	 * @param distance the reach map from {@link #reachDistances}
	 * @return the tile as {@code [x, y]}, or {@code null} if the goat cannot be reached
	 */
	static int[] landingTile(int goatX, int goatY, Map<Long, Integer> distance)
	{
		int best = Integer.MAX_VALUE;
		int[] landing = null;
		for (int[] dir : NEIGHBOURS)
		{
			int nx = goatX + dir[0];
			int ny = goatY + dir[1];
			Integer steps = distance.get(key(nx, ny));
			if (steps != null && steps < best)
			{
				best = steps;
				landing = new int[]{nx, ny};
			}
		}

		return landing;
	}

	/** Packs a tile's world x and y into one long key for the reach map. */
	static long key(int x, int y)
	{
		return ((long) x << 32) | (y & 0xffffffffL);
	}
}
