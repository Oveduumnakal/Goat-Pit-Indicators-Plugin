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

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Compact number formatting for the overlay, mirroring the Stockpile plugin's short form: values
 * scale by the largest fitting magnitude with an uppercase suffix and trailing zeros dropped
 * ({@code 28}, {@code 1.7K}, {@code 12.3K}, {@code 1.5M}), and values under 1,000 are shown as
 * grouped digits. Keeps the total-caught count readable without widening the tile. Stateless
 * utility; cannot be instantiated.
 */
final class ShortFormat
{
	private static final NumberFormat GROUPED = NumberFormat.getIntegerInstance(Locale.US);

	private ShortFormat()
	{
	}

	/**
	 * Compact form to at most 3 significant figures: {@code 234K}, {@code 2.34K}, {@code 1.5M}.
	 *
	 * @param value the number to format
	 * @return its compact string form
	 */
	static String value(long value)
	{
		long abs = Math.abs(value);
		String sign = value < 0 ? "-" : "";
		if (abs >= 1_000_000_000L)
		{
			return sign + mantissa(abs / 1_000_000_000.0) + "B";
		}
		if (abs >= 1_000_000L)
		{
			return sign + mantissa(abs / 1_000_000.0) + "M";
		}
		if (abs >= 1_000L)
		{
			return sign + mantissa(abs / 1_000.0) + "K";
		}
		return sign + GROUPED.format(abs);
	}

	/**
	 * The exact value as grouped digits, e.g. {@code 1,012} or {@code 1,234,567}, for players who want
	 * the precise total rather than the compact {@link #value(long) short form}.
	 *
	 * @param value the number to format
	 * @return its full grouped-digit string form
	 */
	static String exact(long value)
	{
		return GROUPED.format(value);
	}

	/**
	 * Formats a scaled mantissa in {@code [1, 1000)} to 3 significant figures, dropping any trailing
	 * zeros and a dangling decimal point.
	 *
	 * @param d the scaled value in {@code [1, 1000)}
	 * @return the trimmed mantissa string
	 */
	private static String mantissa(double d)
	{
		String s;
		if (d >= 100)
		{
			s = String.format(Locale.US, "%.0f", d);
		}
		else if (d >= 10)
		{
			s = String.format(Locale.US, "%.1f", d);
		}
		else
		{
			s = String.format(Locale.US, "%.2f", d);
		}
		if (s.contains("."))
		{
			s = s.replaceAll("0+$", "");
			if (s.endsWith("."))
			{
				s = s.substring(0, s.length() - 1);
			}
		}
		return s;
	}
}
