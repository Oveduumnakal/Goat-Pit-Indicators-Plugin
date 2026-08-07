/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.awt.Color;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Covers the shared color blend: endpoint fidelity, midpoint mixing, alpha handling, and clamping. */
public class ColorUtilTest
{
	@Test
	public void fractionZeroReturnsFromColor()
	{
		Color from = new Color(10, 20, 30, 40);
		Color result = ColorUtil.lerp(from, new Color(200, 200, 200, 200), 0.0f, true);
		assertEquals(new Color(10, 20, 30, 40), result);
	}

	@Test
	public void fractionOneReturnsToColor()
	{
		Color to = new Color(200, 100, 50, 150);
		Color result = ColorUtil.lerp(new Color(0, 0, 0, 0), to, 1.0f, true);
		assertEquals(new Color(200, 100, 50, 150), result);
	}

	@Test
	public void midpointBlendsEachChannel()
	{
		Color result = ColorUtil.lerp(new Color(0, 0, 0), new Color(100, 200, 40), 0.5f, false);
		assertEquals(new Color(50, 100, 20), result);
	}

	@Test
	public void withoutAlphaBlendTheResultIsOpaque()
	{
		Color result = ColorUtil.lerp(new Color(0, 0, 0, 0), new Color(255, 255, 255, 0), 0.5f, false);
		assertEquals(255, result.getAlpha());
	}

	@Test
	public void withAlphaBlendTheAlphaIsInterpolated()
	{
		Color result = ColorUtil.lerp(new Color(0, 0, 0, 0), new Color(0, 0, 0, 200), 0.5f, true);
		assertEquals(100, result.getAlpha());
	}

	@Test
	public void fractionIsClampedToTheUnitRange()
	{
		Color from = new Color(10, 10, 10);
		Color to = new Color(90, 90, 90);
		assertEquals(from, ColorUtil.lerp(from, to, -1.0f, false));
		assertEquals(to, ColorUtil.lerp(from, to, 2.0f, false));
	}
}
