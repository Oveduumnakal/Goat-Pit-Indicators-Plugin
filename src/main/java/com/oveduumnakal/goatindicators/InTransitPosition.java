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
 * Where the in-transit goat count is drawn on a pit, or off. It offers the same eight compass points as
 * {@link TotalCaughtPosition}, plus a {@link #CENTER} that sits under the pit count. The eight compass
 * constants share their names with {@link TotalCaughtPosition} so a position can be matched to the
 * total's tile: when both land on the same tile, the in-transit line is stacked under the total.
 */
public enum InTransitPosition
{
	/** Do not draw the in-transit count. */
	OFF("Off", Edge.MID, Edge.MID),

	/** Under the pit count in the centre of the pit. */
	CENTER("Center", Edge.MID, Edge.MID),

	/** North edge midpoint. */
	NORTH("North", Edge.MID, Edge.MAX),

	/** North-east corner. */
	NORTH_EAST("North-East", Edge.MAX, Edge.MAX),

	/** East edge midpoint. */
	EAST("East", Edge.MAX, Edge.MID),

	/** South-east corner. */
	SOUTH_EAST("South-East", Edge.MAX, Edge.MIN),

	/** South edge midpoint. */
	SOUTH("South", Edge.MID, Edge.MIN),

	/** South-west corner. */
	SOUTH_WEST("South-West", Edge.MIN, Edge.MIN),

	/** West edge midpoint. */
	WEST("West", Edge.MIN, Edge.MID),

	/** North-west corner. */
	NORTH_WEST("North-West", Edge.MIN, Edge.MAX);

	/** Which end of a pit axis a position sits on: the low edge, the midpoint, or the high edge. */
	private enum Edge
	{
		MIN,
		MID,
		MAX;

		private int resolve(int min, int max)
		{
			if (this == MIN)
				return min;

			if (this == MAX)
				return max;

			return (min + max) / 2;
		}
	}

	private final String label;
	private final Edge xEdge;
	private final Edge yEdge;

	InTransitPosition(String label, Edge xEdge, Edge yEdge)
	{
		this.label = label;
		this.xEdge = xEdge;
		this.yEdge = yEdge;
	}

	/** Whether this position draws the in-transit count at all. */
	boolean isDrawn()
	{
		return this != OFF;
	}

	/** Whether this position is the centre placement, drawn under the pit count rather than on an edge. */
	boolean isCenter()
	{
		return this == CENTER;
	}

	/**
	 * Whether this compass position lands on the same pit tile as the given total-caught position, so the
	 * in-transit line should be stacked beneath the total. Only the eight compass points can match; the
	 * off and centre placements never do.
	 *
	 * @param total the total-caught position to compare against
	 * @return true when both sit on the same compass tile and this is a drawn compass position
	 */
	boolean sharesTileWith(TotalCaughtPosition total)
	{
		if (this == OFF || this == CENTER || total == TotalCaughtPosition.OFF)
			return false;

		return name().equals(total.name());
	}

	/**
	 * The scene x of this position across the pit's x span.
	 *
	 * @param minX the pit's minimum (west) scene x
	 * @param maxX the pit's maximum (east) scene x
	 * @return the scene x the label sits on
	 */
	int sceneX(int minX, int maxX)
	{
		return xEdge.resolve(minX, maxX);
	}

	/**
	 * The scene y of this position across the pit's y span.
	 *
	 * @param minY the pit's minimum (south) scene y
	 * @param maxY the pit's maximum (north) scene y
	 * @return the scene y the label sits on
	 */
	int sceneY(int minY, int maxY)
	{
		return yEdge.resolve(minY, maxY);
	}

	@Override
	public String toString()
	{
		return label;
	}
}
