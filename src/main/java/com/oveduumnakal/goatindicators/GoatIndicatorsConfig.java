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
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

/** Settings for the goat pit overlay: what is drawn, and in which colours. */
@ConfigGroup(GoatIndicatorsConfig.GROUP)
public interface GoatIndicatorsConfig extends Config
{
	/** Config group key, shared with the plugin so both agree on where settings are stored. */
	String GROUP = "goatindicators";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show pit overlay",
		description = "Draw the coloured fill over goat pits in the scene.",
		position = 1
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCount",
		name = "Show goat count",
		description = "Draw the goat count (e.g. 12 / 20) on the pit. The capacity scales with your Hunter level.",
		position = 2
	)
	default boolean showCount()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAddSpikes",
		name = "Show \"Add Spikes\"",
		description = "Label an empty, unspiked pit so it is obvious it needs spikes before it will catch anything.",
		position = 3
	)
	default boolean showAddSpikes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fullOutlineOnly",
		name = "Full: outline only",
		description = "Draw a full pit with just its outline, leaving the footprint unfilled.",
		position = 4
	)
	default boolean fullOutlineOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spikesOutlineOnly",
		name = "Needs spikes: outline only",
		description = "Draw a spikes-needed pit with just its outline, leaving the footprint unfilled.",
		position = 5
	)
	default boolean spikesOutlineOnly()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "fullColor",
		name = "Full",
		description = "Fill colour for a pit that is full.",
		position = 6
	)
	default Color fullColor()
	{
		return new Color(0, 255, 0, 30);
	}

	@Alpha
	@ConfigItem(
		keyName = "partialColor",
		name = "Partly full",
		description = "Fill colour for a pit that is neither full nor waiting on spikes.",
		position = 7
	)
	default Color partialColor()
	{
		return new Color(255, 221, 0, 30);
	}

	@Alpha
	@ConfigItem(
		keyName = "needsSpikesColor",
		name = "Needs spikes",
		description = "Fill colour for an empty pit with no spikes set.",
		position = 8
	)
	default Color needsSpikesColor()
	{
		return new Color(255, 0, 0, 30);
	}

	@Alpha
	@ConfigItem(
		keyName = "textColor",
		name = "Label",
		description = "Colour of the count and \"Add Spikes\" text.",
		position = 9
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	@Range(min = 1, max = 104)
	@ConfigItem(
		keyName = "maxDrawDistance",
		name = "Draw distance",
		description = "Stop drawing the overlay for pits further away than this many tiles.",
		position = 10
	)
	default int maxDrawDistance()
	{
		return 32;
	}

	@ConfigItem(
		keyName = "highlightTelegrab",
		name = "Highlight telegrab?",
		description = "Glow a purple outline on goats you can telegrab into a spiked, non-full pit from "
			+ "where you stand, when you can cast Telekinetic Grab.",
		position = 11
	)
	default boolean highlightTelegrab()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "telegrabColor",
		name = "Telegrab colour",
		description = "Outline colour for telegrabbable goats.",
		position = 12
	)
	default Color telegrabColor()
	{
		return new Color(231, 0, 255, 161);
	}
}
