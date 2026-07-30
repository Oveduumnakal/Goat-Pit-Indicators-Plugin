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
 * How a pit's fill is shown on the overlay: as the {@code X / N} text count, as a horizontal progress
 * bar, or as both. Existing users default to {@link #TEXT} so the display is unchanged until they opt in.
 */
public enum PitCountStyle
{
	/** The {@code X / N} count as text, with no bar. */
	TEXT("Text"),

	/** A progress bar filling with the goat count, with no text. */
	BAR("Bar"),

	/** A progress bar with the {@code X / N} count drawn over it. */
	BAR_AND_TEXT("Bar + Text");

	private final String label;

	PitCountStyle(String label)
	{
		this.label = label;
	}

	/** Whether this style draws the progress bar. */
	boolean showsBar()
	{
		return this == BAR || this == BAR_AND_TEXT;
	}

	/** Whether this style draws the {@code X / N} count as text. */
	boolean showsText()
	{
		return this == TEXT || this == BAR_AND_TEXT;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
