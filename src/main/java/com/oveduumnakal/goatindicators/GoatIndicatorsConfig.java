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
		description = "Draw the X / 20 label on the pit.",
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

	@Alpha
	@ConfigItem(
		keyName = "fullColor",
		name = "Full",
		description = "Fill colour for a pit holding all 20 goats.",
		position = 4
	)
	default Color fullColor()
	{
		return new Color(140, 217, 140, 80);
	}

	@Alpha
	@ConfigItem(
		keyName = "partialColor",
		name = "Partly full",
		description = "Fill colour for a pit that is neither full nor waiting on spikes.",
		position = 5
	)
	default Color partialColor()
	{
		return new Color(230, 214, 122, 60);
	}

	@Alpha
	@ConfigItem(
		keyName = "needsSpikesColor",
		name = "Needs spikes",
		description = "Fill colour for an empty pit with no spikes set.",
		position = 6
	)
	default Color needsSpikesColor()
	{
		return new Color(227, 140, 140, 80);
	}

	@Alpha
	@ConfigItem(
		keyName = "textColor",
		name = "Label",
		description = "Colour of the count and \"Add Spikes\" text.",
		position = 7
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
		position = 8
	)
	default int maxDrawDistance()
	{
		return 32;
	}

	@ConfigItem(
		keyName = "debugLogging",
		name = "Debug logging (developer)",
		description = "Log pit ids, varbits and varbit changes to the client log for id discovery. "
			+ "Leave off unless you are pinning down the goat pit's ids.",
		position = 9
	)
	default boolean debugLogging()
	{
		return false;
	}
}
