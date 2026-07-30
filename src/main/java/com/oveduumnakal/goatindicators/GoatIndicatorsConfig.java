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
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/** Settings for the goat pit overlay: what is drawn, in which colors, and how the labels read. */
@ConfigGroup(GoatIndicatorsConfig.GROUP)
public interface GoatIndicatorsConfig extends Config
{
	/** Config group key, shared with the plugin so both agree on where settings are stored. */
	String GROUP = "goatindicators";

	/** What is drawn: the pit color indicators, the spikes prompt, and the telegrab highlight. */
	@ConfigSection(
		name = "Indicators",
		description = "Which indicators are drawn.",
		position = 0
	)
	String indicatorsSection = "indicators";

	/** Colors of the pit outline gradient, the reminder fills, and the telegrab highlight. */
	@ConfigSection(
		name = "Indicator Colors",
		description = "Colors of the outline, the reminder fills, and the telegrab highlight.",
		position = 1
	)
	String colorsSection = "colors";

	/** The on-pit text labels and their colors. */
	@ConfigSection(
		name = "Labels",
		description = "The on-pit text labels and their colors.",
		position = 2
	)
	String labelsSection = "labels";

	/** Everything else. */
	@ConfigSection(
		name = "Misc",
		description = "Everything else.",
		position = 3
	)
	String miscSection = "misc";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Color Indicators",
		description = "Draw the colored outline and fills over goat pits in the scene.",
		section = indicatorsSection,
		position = 1
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAddSpikes",
		name = "Show \"Add Spikes\"",
		description = "Label an empty, unspiked pit so it is obvious it needs spikes before it will catch anything.",
		section = indicatorsSection,
		position = 2
	)
	default boolean showAddSpikes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightTelegrab",
		name = "Highlight Telegrabbable",
		description = "Glow an outline on goats you can telegrab into a spiked, non-full pit from where "
			+ "you stand, when you can cast Telekinetic Grab.",
		section = indicatorsSection,
		position = 3
	)
	default boolean highlightTelegrab()
	{
		return true;
	}

	@ConfigItem(
		keyName = "emptyOutlineColor",
		name = "Empty Outline",
		description = "Outline color for an empty or unspiked pit, and the low end of the fill gradient. "
			+ "Alpha is ignored.",
		section = colorsSection,
		position = 1
	)
	default Color emptyOutlineColor()
	{
		return new Color(255, 0, 0);
	}

	@ConfigItem(
		keyName = "partialColor",
		name = "Midpoint Outline",
		description = "Outline color at the middle of the fill gradient. Alpha is ignored.",
		section = colorsSection,
		position = 2
	)
	default Color midpointOutlineColor()
	{
		return new Color(255, 221, 0);
	}

	@ConfigItem(
		keyName = "fullOutlineColor",
		name = "Full Outline",
		description = "Outline color for a full pit, and the high end of the fill gradient. Alpha is "
			+ "ignored.",
		section = colorsSection,
		position = 3
	)
	default Color fullOutlineColor()
	{
		return new Color(0, 255, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "spikeReminderFill",
		name = "Spike Reminder Fill",
		description = "Fill for an empty pit that needs spikes. Set the alpha to 0 for outline only.",
		section = colorsSection,
		position = 4
	)
	default Color spikeReminderFill()
	{
		return new Color(255, 0, 0, 30);
	}

	@Alpha
	@ConfigItem(
		keyName = "fullReminderFill",
		name = "Full Reminder Fill",
		description = "Fill for a full pit that is ready to empty. Set the alpha to 0 for outline only.",
		section = colorsSection,
		position = 5
	)
	default Color fullReminderFill()
	{
		return new Color(0, 255, 0, 30);
	}

	@Alpha
	@ConfigItem(
		keyName = "telegrabColor",
		name = "Telegrab Color",
		description = "Outline color for telegrabbable goats.",
		section = colorsSection,
		position = 6
	)
	default Color telegrabColor()
	{
		return new Color(255, 0, 202, 255);
	}

	@ConfigItem(
		keyName = "showCount",
		name = "Show Goats in Pit",
		description = "Draw the goat count (e.g. 12 / 20) on the pit. The capacity scales with your Hunter level.",
		section = labelsSection,
		position = 1
	)
	default boolean showCount()
	{
		return true;
	}

	@ConfigItem(
		keyName = "totalCaughtPosition",
		name = "Show Total Caught",
		description = "Where to draw the lifetime total of goats caught on the pit: off, or one of its "
			+ "compass points. The game keeps no total of its own, so the plugin counts every catch and "
			+ "keeps the tally across logins and restarts.",
		section = labelsSection,
		position = 2
	)
	default TotalCaughtPosition totalCaughtPosition()
	{
		return TotalCaughtPosition.SOUTH_EAST;
	}

	@Alpha
	@ConfigItem(
		keyName = "countLabelColor",
		name = "Count Label Color",
		description = "Color of the goat count and \"Add Spikes\" text.",
		section = labelsSection,
		position = 3
	)
	default Color countLabelColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "totalLabelColor",
		name = "Total Label Color",
		description = "Color of the total-caught text.",
		section = labelsSection,
		position = 4
	)
	default Color totalLabelColor()
	{
		return Color.WHITE;
	}

	@Range(min = 1, max = 104)
	@ConfigItem(
		keyName = "maxDrawDistance",
		name = "Draw Distance",
		description = "Stop drawing the overlay for pits further away than this many tiles.",
		section = miscSection,
		position = 1
	)
	default int maxDrawDistance()
	{
		return 32;
	}
}
