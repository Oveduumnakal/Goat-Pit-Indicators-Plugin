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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Area;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws each loaded goat pit as an outlined footprint carrying its {@code X / N} count, where
 * {@code N} is the pit's capacity for the player's Hunter level.
 *
 * <p>The outline is the signal. A pit with no spikes is drawn solid red — it will catch nothing until
 * spikes go back in. A spiked pit's outline runs from red toward green as the count climbs, so a
 * glance tells you how close it is to full, and a full pit also gets a solid green fill so it stands
 * out as ready to empty. The {@code X / N} count sits in the centre, and the "Add Spikes" line only
 * appears while the pit is unspiked.
 */
class GoatPitOverlay extends Overlay
{
	private static final Stroke OUTLINE = new BasicStroke(2.0f);
	private static final int OUTLINE_ALPHA = 220;
	private static final String ADD_SPIKES_TEXT = "Add Spikes";

	private final Client client;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;

	@Inject
	GoatPitOverlay(Client client, GoatIndicatorsConfig config, GoatPitTracker tracker)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		WorldPoint playerLocation = player.getWorldLocation();
		if (playerLocation == null)
		{
			return null;
		}
		for (GameObject pit : tracker.getPits())
		{
			renderPit(graphics, pit, playerLocation);
		}
		return null;
	}

	private void renderPit(Graphics2D graphics, GameObject pit, WorldPoint playerLocation)
	{
		WorldPoint pitLocation = pit.getWorldLocation();
		if (pitLocation == null || playerLocation.distanceTo(pitLocation) > config.maxDrawDistance())
		{
			return;
		}
		Area footprint = footprintOf(pit);
		if (footprint == null || footprint.isEmpty())
		{
			return;
		}
		GoatPitState state = tracker.stateOf(pit);
		Color fill = fillColorFor(state);
		if (fill != null)
		{
			graphics.setColor(fill);
			graphics.fill(footprint);
		}
		graphics.setColor(outlineColorFor(state));
		graphics.setStroke(OUTLINE);
		graphics.draw(footprint);
		renderLabels(graphics, pit, state);
	}

	/**
	 * Whether to prompt for spikes: only when the pit is both empty and unspiked, i.e. showing
	 * {@code 0 / N}. In that state the count label is replaced by the prompt.
	 */
	private boolean promptAddSpikes(GoatPitState state)
	{
		return state.isEmpty() && state.needsSpikes();
	}

	/**
	 * The footprint fill colour, or {@code null} to leave the pit unfilled. A full pit fills green; a
	 * spikes-needed pit fills with the needs-spikes colour; every in-between state is outline only.
	 */
	private Color fillColorFor(GoatPitState state)
	{
		if (state.isFull())
		{
			return config.fullOutlineOnly() ? null : config.fullColor();
		}
		if (promptAddSpikes(state))
		{
			return config.spikesOutlineOnly() ? null : config.needsSpikesColor();
		}
		return null;
	}

	private void renderLabels(Graphics2D graphics, GameObject pit, GoatPitState state)
	{
		if (config.showAddSpikes() && promptAddSpikes(state))
		{
			Point at = pit.getCanvasTextLocation(graphics, ADD_SPIKES_TEXT, 0);
			if (at != null)
			{
				OverlayUtil.renderTextLocation(graphics, at, ADD_SPIKES_TEXT, config.textColor());
			}
			return;
		}
		if (config.showCount())
		{
			Point at = pit.getCanvasTextLocation(graphics, state.label(), 0);
			if (at != null)
			{
				OverlayUtil.renderTextLocation(graphics, at, state.label(), config.textColor());
			}
		}
	}

	/**
	 * The outline colour: solid red while the pit is unspiked, otherwise a blend running from the
	 * needs-spikes colour through the partial colour to the full colour in step with how full it is.
	 */
	private Color outlineColorFor(GoatPitState state)
	{
		if (state.needsSpikes())
		{
			return withAlpha(config.needsSpikesColor(), OUTLINE_ALPHA);
		}
		float fraction = (float) state.getCount() / state.getCapacity();
		Color blended = lerp3(config.needsSpikesColor(), config.partialColor(), config.fullColor(), fraction);
		return withAlpha(blended, OUTLINE_ALPHA);
	}

	private static Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * Blends across three colour stops — {@code from} at 0, {@code mid} at 0.5, {@code to} at 1 —
	 * ignoring alpha; {@code fraction} is clamped to 0..1.
	 */
	private static Color lerp3(Color from, Color mid, Color to, float fraction)
	{
		float f = Math.max(0.0f, Math.min(1.0f, fraction));
		if (f <= 0.5f)
		{
			return lerp(from, mid, f * 2.0f);
		}
		return lerp(mid, to, (f - 0.5f) * 2.0f);
	}

	/** Linearly blends two colours, ignoring their alpha; {@code fraction} is clamped to 0..1. */
	private static Color lerp(Color from, Color to, float fraction)
	{
		float f = Math.max(0.0f, Math.min(1.0f, fraction));
		int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * f);
		int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * f);
		int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * f);
		return new Color(r, g, b);
	}

	/**
	 * Unions the canvas polygon of every tile the pit stands on, so a multi-tile pit reads as one
	 * shape rather than a grid of separate squares. Returns {@code null} when the pit is off screen.
	 */
	private Area footprintOf(GameObject pit)
	{
		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		if (min == null || max == null)
		{
			return null;
		}
		Area area = new Area();
		for (int x = min.getX(); x <= max.getX(); x++)
		{
			for (int y = min.getY(); y <= max.getY(); y++)
			{
				LocalPoint tile = LocalPoint.fromScene(x, y, pit.getWorldView());
				Polygon poly = Perspective.getCanvasTilePoly(client, tile);
				if (poly != null)
				{
					area.add(new Area(poly));
				}
			}
		}
		return area;
	}
}
