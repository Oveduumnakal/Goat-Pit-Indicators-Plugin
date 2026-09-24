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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.util.Locale;
import javax.inject.Inject;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A movable infobox showing this session's catching stats (#101): elapsed time, goats caught, goats per
 * hour, Hunter XP gained, and XP per hour. Reads {@link SessionStats}, which the plugin samples each tick;
 * hidden until the session has a sample and while the config toggle is off. A standard {@link OverlayPanel},
 * so the user can drag it anywhere from the overlay menu.
 */
class SessionStatsOverlay extends OverlayPanel
{
	private final GoatIndicatorsConfig config;
	private final SessionStats stats;

	@Inject
	SessionStatsOverlay(GoatIndicatorsConfig config, SessionStats stats)
	{
		this.config = config;
		this.stats = stats;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showSessionStats() || !stats.started())
			return null;

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Goat Session")
			.build());
		panelComponent.getChildren().add(line("Time", formatDuration(stats.elapsed())));
		panelComponent.getChildren().add(line("Caught", ShortFormat.exact(stats.catches())));
		panelComponent.getChildren().add(line("Goats/hr", ShortFormat.exact(stats.catchesPerHour())));
		panelComponent.getChildren().add(line("Hunter xp", ShortFormat.value(stats.xpGained())));
		panelComponent.getChildren().add(line("Xp/hr", ShortFormat.value(stats.xpPerHour())));

		return super.render(graphics);
	}

	/** @return a two-column stat row with {@code label} on the left and {@code value} on the right. */
	private static LineComponent line(String label, String value)
	{
		return LineComponent.builder()
			.left(label)
			.right(value)
			.build();
	}

	/** @return {@code duration} as {@code H:MM:SS}, dropping the hours field while under an hour. */
	static String formatDuration(Duration duration)
	{
		long seconds = Math.max(0, duration.getSeconds());
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long secs = seconds % 60;
		if (hours > 0)
			return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs);

		return String.format(Locale.US, "%02d:%02d", minutes, secs);
	}
}
