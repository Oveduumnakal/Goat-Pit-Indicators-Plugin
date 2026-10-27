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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure scoring for the "stand here" hint (#104): where the player can walk to put the most goats in reach of the
 * current catching method at once.
 *
 * <p>Candidates come from the tiles the prod-reach flood ({@link ProdPathing#reachDistances}) found walkable from
 * the player, minus the pit's own footprint, and are scored by how many goats the method could send into a
 * catching pit from there, reusing the same rules the goat highlight applies to the player's own tile:
 * <ul>
 *   <li><b>Lure</b> (Telekinetic Grab / Dark Lure): goats within cast range, on the far side of a pit, and in
 *   line of sight ({@link TelegrabTargeting}). A lure is best cast from right beside the pit, so the candidates
 *   are a pit's four sides: the strip of tiles sharing that side's edge (3 on a 3x3 pit, corners excluded),
 *   scored by the goats any of its reachable tiles could lure (#151).</li>
 *   <li><b>Prod</b>: goats right beside the tile (a prod needs you adjacent), within prod range of a pit, and
 *   with that pit in the push direction ({@link ProdTargeting}). Candidates are single tiles.</li>
 * </ul>
 * The highest score wins; ties go to the candidate the player reaches in the fewest steps, so standing still
 * wins whenever the current tile is already as good as any.
 *
 * <p>Goats are passed as {@code int[]{x, y, id}} and pit footprints as {@code int[]{minX, minY, maxX, maxY}},
 * all in world coordinates, so the rules can be unit-tested without a {@code Client}. Line of sight depends on
 * the scene, so it is supplied by the caller as a {@link SightFn}.
 */
final class BestTile
{
	/** Tests whether a lure could be cast from a tile to a goat, i.e. the tile has line of sight to it. */
	@FunctionalInterface
	interface SightFn
	{
		/**
		 * Whether the tile has line of sight to the goat.
		 *
		 * @param x the tile world x
		 * @param y the tile world y
		 * @param goatId the goat's id, as passed in its {@code int[]{x, y, id}}
		 * @return true when a lure can be cast from the tile to the goat
		 */
		boolean canSee(int x, int y, int goatId);
	}

	/** Scores one candidate tile. */
	@FunctionalInterface
	interface ScoreFn
	{
		/**
		 * How many goats the method could send in from the tile.
		 *
		 * @param x the tile world x
		 * @param y the tile world y
		 * @return the tile's score
		 */
		int score(int x, int y);
	}

	private BestTile()
	{
	}

	/**
	 * The best pit side to stand beside for a lure, as {@code int[]{minX, minY, maxX, maxY, score}}: the strip of
	 * tiles along one edge of a pit, scored by how many goats a lure from any of its reachable tiles could send
	 * into a catching pit. {@code null} when no side has a reachable tile that scores above zero.
	 *
	 * @param reach the walkable tiles and their step distance from the player, from {@link ProdPathing}
	 * @param pits the catching pits' world footprints
	 * @param goats the goats as {@code int[]{x, y, id}}
	 * @param sight the line-of-sight test
	 * @return the winning side and its score, or {@code null}
	 */
	static int[] pickLureSide(Map<Long, Integer> reach, List<int[]> pits, List<int[]> goats, SightFn sight)
	{
		int[] best = null;
		int bestSteps = Integer.MAX_VALUE;
		long bestKey = Long.MAX_VALUE;
		for (int[] pit : pits)
		{
			for (int[] side : sides(pit))
			{
				int steps = Integer.MAX_VALUE;
				for (int x = side[0]; x <= side[2]; x++)
				{
					for (int y = side[1]; y <= side[3]; y++)
					{
						Integer distance = reach.get(ProdPathing.key(x, y));
						if (distance != null && !insideAnyPit(x, y, pits))
							steps = Math.min(steps, distance);
					}
				}

				if (steps == Integer.MAX_VALUE)
					continue;

				int score = sideLureCount(side, reach, goats, pits, sight);
				long key = ProdPathing.key(side[0], side[1]);
				if (score > 0 && (best == null || beats(score, steps, key, best[4], bestSteps, bestKey)))
				{
					best = new int[]{side[0], side[1], side[2], side[3], score};
					bestSteps = steps;
					bestKey = key;
				}
			}
		}

		return best;
	}

	/**
	 * The best tile to stand on, as {@code int[]{x, y, score}}, or {@code null} when no reachable tile scores
	 * above zero. Pit tiles are never suggested.
	 *
	 * @param reach the walkable tiles and their step distance from the player, from {@link ProdPathing}
	 * @param pits the catching pits' world footprints
	 * @param scorer the method's score for a tile
	 * @return the winning tile and its score, or {@code null}
	 */
	static int[] pick(Map<Long, Integer> reach, List<int[]> pits, ScoreFn scorer)
	{
		int[] best = null;
		int bestSteps = Integer.MAX_VALUE;
		long bestKey = Long.MAX_VALUE;
		for (Map.Entry<Long, Integer> entry : reach.entrySet())
		{
			long key = entry.getKey();
			int x = (int) (key >> 32);
			int y = (int) key;
			if (insideAnyPit(x, y, pits))
				continue;

			int score = scorer.score(x, y);
			if (score <= 0)
				continue;

			int steps = entry.getValue();
			if (best == null || beats(score, steps, key, best[2], bestSteps, bestKey))
			{
				best = new int[]{x, y, score};
				bestSteps = steps;
				bestKey = key;
			}
		}

		return best;
	}

	/**
	 * How many goats a lure from the tile could drag into a catching pit: in cast range, across a pit from the
	 * tile, and in sight.
	 *
	 * @param x the tile world x
	 * @param y the tile world y
	 * @param goats the goats as {@code int[]{x, y, id}}
	 * @param pits the catching pits' world footprints
	 * @param sight the line-of-sight test
	 * @return the number of goats the tile could lure
	 */
	static int lureCount(int x, int y, List<int[]> goats, List<int[]> pits, SightFn sight)
	{
		int count = 0;
		for (int[] goat : goats)
		{
			if (canLure(x, y, goat, pits, sight))
				count++;
		}

		return count;
	}

	/**
	 * The edges to outline around a stand-here spot {@code int[]{minX, minY, maxX, maxY, ...}}: every tile edge
	 * except those shared with another tile of the spot or with a pit, so only the spot's outer border shows.
	 * Each edge is {@code int[]{x1, y1, x2, y2}} in tile-corner coordinates, where corner {@code (x, y)} is the
	 * south-west corner of tile {@code (x, y)}.
	 *
	 * @param spot the spot's world bounds
	 * @param pits the catching pits' world footprints
	 * @return the outer edges to draw
	 */
	static List<int[]> outerEdges(int[] spot, List<int[]> pits)
	{
		List<int[]> edges = new ArrayList<>();
		for (int x = spot[0]; x <= spot[2]; x++)
		{
			for (int y = spot[1]; y <= spot[3]; y++)
			{
				addEdgeUnlessShared(edges, spot, pits, x, y - 1, new int[]{x, y, x + 1, y});
				addEdgeUnlessShared(edges, spot, pits, x, y + 1, new int[]{x, y + 1, x + 1, y + 1});
				addEdgeUnlessShared(edges, spot, pits, x - 1, y, new int[]{x, y, x, y + 1});
				addEdgeUnlessShared(edges, spot, pits, x + 1, y, new int[]{x + 1, y, x + 1, y + 1});
			}
		}

		return edges;
	}

	/** Adds the edge unless the tile across it, {@code (nx, ny)}, is part of the spot or of a pit. */
	private static void addEdgeUnlessShared(List<int[]> edges, int[] spot, List<int[]> pits, int nx, int ny,
		int[] edge)
	{
		boolean inSpot = nx >= spot[0] && nx <= spot[2] && ny >= spot[1] && ny <= spot[3];
		if (!inSpot && !insideAnyPit(nx, ny, pits))
			edges.add(edge);
	}

	/** The four strips of tiles sharing an edge with the pit, as {@code int[]{minX, minY, maxX, maxY}}. */
	private static int[][] sides(int[] pit)
	{
		return new int[][]{
			{pit[0] - 1, pit[1], pit[0] - 1, pit[3]},
			{pit[2] + 1, pit[1], pit[2] + 1, pit[3]},
			{pit[0], pit[1] - 1, pit[2], pit[1] - 1},
			{pit[0], pit[3] + 1, pit[2], pit[3] + 1},
		};
	}

	/** How many goats a lure from at least one reachable, non-pit tile of the side could send into a pit. */
	private static int sideLureCount(int[] side, Map<Long, Integer> reach, List<int[]> goats, List<int[]> pits,
		SightFn sight)
	{
		int count = 0;
		for (int[] goat : goats)
		{
			if (sideCanLure(side, reach, goat, pits, sight))
				count++;
		}

		return count;
	}

	/** Whether a lure from any reachable, non-pit tile of the side could send the goat into a pit. */
	private static boolean sideCanLure(int[] side, Map<Long, Integer> reach, int[] goat, List<int[]> pits,
		SightFn sight)
	{
		for (int x = side[0]; x <= side[2]; x++)
		{
			for (int y = side[1]; y <= side[3]; y++)
			{
				if (reach.containsKey(ProdPathing.key(x, y)) && !insideAnyPit(x, y, pits)
						&& canLure(x, y, goat, pits, sight))
					return true;
			}
		}

		return false;
	}

	/** Whether a lure from the tile could send the goat into a pit: in cast range, across a pit, and in sight. */
	private static boolean canLure(int x, int y, int[] goat, List<int[]> pits, SightFn sight)
	{
		return TelegrabTargeting.withinCastRange(chebyshev(x, y, goat[0], goat[1]))
			&& acrossAnyPit(x, y, goat, pits) && sight.canSee(x, y, goat[2]);
	}

	/**
	 * How many goats standing on the tile could prod into a catching pit: right beside it, within prod range of
	 * a pit, and with that pit in the direction the prod pushes.
	 *
	 * @param x the tile world x
	 * @param y the tile world y
	 * @param goats the goats as {@code int[]{x, y, id}}
	 * @param pits the catching pits' world footprints
	 * @return the number of goats the tile could prod in
	 */
	static int prodCount(int x, int y, List<int[]> goats, List<int[]> pits)
	{
		int count = 0;
		for (int[] goat : goats)
		{
			if (chebyshev(x, y, goat[0], goat[1]) == 1 && prodableIntoAnyPit(x, y, goat, pits))
				count++;
		}

		return count;
	}

	/** Whether a goat sits across at least one pit from the tile. */
	private static boolean acrossAnyPit(int x, int y, int[] goat, List<int[]> pits)
	{
		for (int[] pit : pits)
		{
			if (TelegrabTargeting.oppositeSideOfPit(pit[0], pit[1], pit[2], pit[3], x, y, goat[0], goat[1]))
				return true;
		}

		return false;
	}

	/** Whether a prod from the tile would send the goat into at least one pit within prod range. */
	private static boolean prodableIntoAnyPit(int x, int y, int[] goat, List<int[]> pits)
	{
		for (int[] pit : pits)
		{
			int nearestX = ProdTargeting.clampToPit(goat[0], pit[0], pit[2]);
			int nearestY = ProdTargeting.clampToPit(goat[1], pit[1], pit[3]);
			int distance = chebyshev(goat[0], goat[1], nearestX, nearestY);
			if (ProdTargeting.withinProdRange(distance)
					&& ProdTargeting.pitInPushDirection(x, y, goat[0], goat[1], pit[0], pit[1], pit[2], pit[3]))
				return true;
		}

		return false;
	}

	/** Whether the tile lies on any pit's footprint. */
	private static boolean insideAnyPit(int x, int y, List<int[]> pits)
	{
		for (int[] pit : pits)
		{
			if (x >= pit[0] && x <= pit[2] && y >= pit[1] && y <= pit[3])
				return true;
		}

		return false;
	}

	/**
	 * Whether a candidate beats the current best: a higher score, or an equal score reached in fewer steps, with
	 * the packed tile key as a final tie-break so the pick never depends on map iteration order.
	 */
	private static boolean beats(int score, int steps, long key, int bestScore, int bestSteps, long bestKey)
	{
		if (score != bestScore)
			return score > bestScore;

		if (steps != bestSteps)
			return steps < bestSteps;

		return key < bestKey;
	}

	/** The Chebyshev (king-move) distance between two tiles. */
	private static int chebyshev(int x1, int y1, int x2, int y2)
	{
		return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
}
