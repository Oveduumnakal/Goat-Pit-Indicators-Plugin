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

import java.awt.Color;

/** Color helpers shared by the overlays. */
final class ColorUtil
{
	private ColorUtil()
	{
		throw new AssertionError("utility class");
	}

	/**
	 * Linearly blends two colors; {@code fraction} is clamped to 0..1. When {@code blendAlpha} is set the
	 * result's alpha is interpolated between the two colors as well; otherwise the result is fully opaque and
	 * the input alphas are ignored. The pit overlay blends opaque gradient stops, while the highlight overlay
	 * carries alpha through its outline gradient — this one helper serves both, with the difference explicit.
	 *
	 * @param from       the color at {@code fraction} 0
	 * @param to         the color at {@code fraction} 1
	 * @param fraction   the blend position, clamped to {@code [0, 1]}
	 * @param blendAlpha whether to interpolate alpha too ({@code false} yields an opaque color)
	 * @return the blended color
	 */
	static Color lerp(Color from, Color to, float fraction, boolean blendAlpha)
	{
		float f = Math.max(0.0f, Math.min(1.0f, fraction));
		int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * f);
		int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * f);
		int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * f);
		if (!blendAlpha)
			return new Color(r, g, b);

		int a = Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * f);
		return new Color(r, g, b, a);
	}
}
