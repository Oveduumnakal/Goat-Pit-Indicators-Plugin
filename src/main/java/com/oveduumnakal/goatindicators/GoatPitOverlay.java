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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
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
import net.runelite.client.util.ImageUtil;

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

	/** Height in pixels the goat icon is scaled to for the total-caught label. */
	private static final int ICON_HEIGHT = 16;

	/** Gap in pixels between the goat icon and the total number. */
	private static final int ICON_GAP = 2;

	private final Client client;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;
	private final GoatCatchCounter catchCounter;
	private final BufferedImage totalIcon;

	@Inject
	GoatPitOverlay(Client client, GoatIndicatorsConfig config, GoatPitTracker tracker,
		GoatCatchCounter catchCounter)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.catchCounter = catchCounter;
		this.totalIcon = loadTotalIcon();
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
		renderTotalCaught(graphics, pit);
	}

	/**
	 * Draws the lifetime catch total on the pit corner chosen in the config, in Stockpile short form, so
	 * it sits clear of the centred count. North is +y and east is +x.
	 */
	private void renderTotalCaught(Graphics2D graphics, GameObject pit)
	{
		TotalCaughtPosition position = config.totalCaughtPosition();
		if (position == TotalCaughtPosition.OFF)
		{
			return;
		}
		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		if (min == null || max == null)
		{
			return;
		}
		int sceneX = position.sceneX(min.getX(), max.getX());
		int sceneY = position.sceneY(min.getY(), max.getY());
		String text = totalCaughtText();
		LocalPoint tile = LocalPoint.fromScene(sceneX, sceneY, pit.getWorldView());
		Point at = Perspective.getCanvasTextLocation(client, graphics, tile, text, 0);
		if (at == null)
		{
			return;
		}
		Color color = config.totalLabelColor();
		if (drawIcon())
		{
			drawTotalIcon(graphics, at, color);
		}
		drawText(graphics, at, text, color);
	}

	/**
	 * The total-caught label text for the configured prefix. "Text" prepends {@code "Total: "}; "None"
	 * and "Icon" show the bare number (the icon draws separately, to the left of the number).
	 */
	private String totalCaughtText()
	{
		String number = ShortFormat.value(catchCounter.getTotal());
		if (config.totalPrefix() == TotalPrefix.TEXT)
		{
			return "Total: " + number;
		}
		return number;
	}

	/** Whether the goat icon should precede the total: the prefix is set to icon and the icon loaded. */
	private boolean drawIcon()
	{
		return config.totalPrefix() == TotalPrefix.ICON && totalIcon != null;
	}

	/**
	 * Draws the goat icon just left of the total number, vertically centred on the text and faded to the
	 * label color's alpha so it matches the number.
	 */
	private void drawTotalIcon(Graphics2D graphics, Point at, Color color)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int iconX = at.getX() - totalIcon.getWidth() - ICON_GAP;
		int iconY = at.getY() - metrics.getAscent() / 2 - totalIcon.getHeight() / 2;
		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, color.getAlpha() / 255.0f));
		graphics.drawImage(totalIcon, iconX, iconY, null);
		graphics.setComposite(original);
	}

	/** Loads the goat icon from resources and scales it to {@link #ICON_HEIGHT}, or null if missing. */
	private static BufferedImage loadTotalIcon()
	{
		BufferedImage raw = ImageUtil.loadImageResource(GoatPitOverlay.class, "goat_head.png");
		if (raw == null)
		{
			return null;
		}
		int width = Math.max(1, Math.round(raw.getWidth() * (ICON_HEIGHT / (float) raw.getHeight())));
		return ImageUtil.resizeImage(raw, width, ICON_HEIGHT);
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
	 * The footprint fill color, or {@code null} to leave the pit unfilled. A full pit takes the full
	 * reminder fill, a spikes-needed pit takes the spike reminder fill, and every in-between state is
	 * outline only. A fill's own alpha decides how strong it is, so alpha 0 reads as outline only.
	 */
	private Color fillColorFor(GoatPitState state)
	{
		if (state.isFull())
		{
			return config.fullReminderFill();
		}
		if (promptAddSpikes(state))
		{
			return config.spikeReminderFill();
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
				drawText(graphics, at, ADD_SPIKES_TEXT, config.countLabelColor());
			}
			return;
		}
		if (config.showCount())
		{
			Point at = pit.getCanvasTextLocation(graphics, state.label(), 0);
			if (at != null)
			{
				drawText(graphics, at, state.label(), config.countLabelColor());
			}
		}
	}

	/**
	 * Draws label text with its own drop shadow, honouring the color's alpha. The built-in overlay text
	 * helper leaves the shadow opaque, so a translucent label never actually looks translucent; here both
	 * the shadow and the glyphs are drawn under an {@link AlphaComposite} keyed to the color's alpha, so
	 * the whole label fades together and alpha 0 is fully invisible.
	 */
	private static void drawText(Graphics2D graphics, Point at, String text, Color color)
	{
		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, color.getAlpha() / 255.0f));
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, at.getX() + 1, at.getY() + 1);
		graphics.setColor(new Color(color.getRGB()));
		graphics.drawString(text, at.getX(), at.getY());
		graphics.setComposite(original);
	}

	/**
	 * The outline color: the solid empty-outline color while the pit is unspiked, otherwise a blend
	 * running from the empty-outline color through the midpoint color to the full-outline color in step
	 * with how full it is.
	 */
	private Color outlineColorFor(GoatPitState state)
	{
		if (state.needsSpikes())
		{
			return withAlpha(config.emptyOutlineColor(), OUTLINE_ALPHA);
		}
		float fraction = (float) state.getCount() / state.getCapacity();
		Color blended = lerp3(
			config.emptyOutlineColor(), config.midpointOutlineColor(), config.fullOutlineColor(), fraction);
		return withAlpha(blended, OUTLINE_ALPHA);
	}

	private static Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * Blends across three color stops — {@code from} at 0, {@code mid} at 0.5, {@code to} at 1 —
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

	/** Linearly blends two colors, ignoring their alpha; {@code fraction} is clamped to 0..1. */
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
