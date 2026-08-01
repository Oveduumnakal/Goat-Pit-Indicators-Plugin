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
 * Where the lifetime total-caught label is drawn on a pit, or off. The eight positions are the pit's
 * compass points — four corners and four edge midpoints — in world space, where north is +y and east
 * is +x.
 */
public enum TotalCaughtPosition
{
	/** Do not draw the total. */
	OFF("Off", Edge.MID, Edge.MID),

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

	TotalCaughtPosition(String label, Edge xEdge, Edge yEdge)
	{
		this.label = label;
		this.xEdge = xEdge;
		this.yEdge = yEdge;
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
