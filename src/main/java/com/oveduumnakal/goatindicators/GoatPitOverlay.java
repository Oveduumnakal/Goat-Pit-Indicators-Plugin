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
import java.util.concurrent.ThreadLocalRandom;
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

	/** Progress bar dimensions in pixels, and the translucent backing drawn behind the fill. */
	private static final int BAR_WIDTH = 48;
	private static final int BAR_HEIGHT = 16;
	private static final Color BAR_BACKGROUND = new Color(0, 0, 0, 150);

	/** Height in pixels icons (in-transit) are scaled to. */
	private static final int ICON_HEIGHT = 16;

	/** Height in pixels the animated goat icon is scaled to for the total-caught label. */
	private static final int TOTAL_ICON_HEIGHT = 22;

	/** Gap in pixels between the goat icon and the total number. */
	private static final int ICON_GAP = 2;

	/** Number of frames in the leaping-goat animation strip. */
	private static final int TOTAL_ICON_FRAMES = 11;

	/** Leap-strip frame (0-based) shown as the static "Icon" prefix: the goat half in the hole. */
	private static final int STATIC_ICON_FRAME = 8;

	/** Milliseconds the goat spends walking in from the left before it stands and pauses. */
	private static final int ANIM_WALK_MS = 800;

	/** Milliseconds between walk-cycle pose swaps (standing frame vs stride frame) while walking in. */
	private static final int ANIM_WALK_STEP_MS = 107;

	/** Pixels to the left of its resting spot the goat starts its walk-in from. */
	private static final int ANIM_WALK_DISTANCE = 24;

	/** Milliseconds each leap frame is shown while the animation is playing. */
	private static final int ANIM_FRAME_MS = 100;

	/** Milliseconds the final hole frame is held before it fades out. */
	private static final int ANIM_HOLD_MS = 250;

	/** Milliseconds the final hole frame takes to fade from opaque to fully transparent. */
	private static final int ANIM_FADE_MS = 250;

	/** Shortest time in milliseconds the goat stands still before a leap (2 minutes). */
	private static final long STAND_MIN_MS = 120_000L;

	/** Longest time in milliseconds the goat stands still before a leap (3.5 minutes). */
	private static final long STAND_MAX_MS = 210_000L;

	private final Client client;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;
	private final GoatTransitTracker transitTracker;
	private final GoatCatchCounter catchCounter;
	private final BufferedImage[] totalIconFrames;
	private final BufferedImage totalWalkFrame;
	private final BufferedImage inTransitIcon;

	/** Current phase of the total-goat animation, its start time, and the current random stand length. */
	private AnimPhase totalPhase = AnimPhase.STANDING;
	private long totalPhaseStart = System.currentTimeMillis();
	private long totalStandMs = randomStandMs();

	@Inject
	GoatPitOverlay(Client client, GoatIndicatorsConfig config, GoatPitTracker tracker,
		GoatTransitTracker transitTracker, GoatCatchCounter catchCounter)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.transitTracker = transitTracker;
		this.catchCounter = catchCounter;
		this.totalIconFrames = loadIconStrip("goat_leap_strip.png", TOTAL_ICON_FRAMES, TOTAL_ICON_HEIGHT);
		BufferedImage[] walk = loadIconStrip("goat_walk.png", 1, TOTAL_ICON_HEIGHT);
		this.totalWalkFrame = walk == null ? null : walk[0];
		this.inTransitIcon = loadIcon("in_transit_icon.png");
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
			return null;

		WorldPoint playerLocation = player.getWorldLocation();
		if (playerLocation == null)
			return null;

		for (GameObject pit : tracker.getPits())
			renderPit(graphics, pit, playerLocation);

		return null;
	}

	private void renderPit(Graphics2D graphics, GameObject pit, WorldPoint playerLocation)
	{
		WorldPoint pitLocation = pit.getWorldLocation();
		if (pitLocation == null || playerLocation.distanceTo(pitLocation) > config.maxDrawDistance())
			return;

		GoatPitState state = tracker.stateOf(pit);
		if (config.showOverlay())
			drawColorIndicators(graphics, pit, state);

		renderLabels(graphics, pit, state);
		renderInTransit(graphics, pit);
		renderTotalCaught(graphics, pit);
	}

	/**
	 * Draws the colored outline and reminder fill over the pit footprint. Gated on the "Show Color
	 * Indicators" toggle, separately from the text labels so the count, total and in-transit lines can
	 * still be shown with the coloring turned off.
	 */
	private void drawColorIndicators(Graphics2D graphics, GameObject pit, GoatPitState state)
	{
		Area footprint = footprintOf(pit);
		if (footprint == null || footprint.isEmpty())
			return;

		Color fill = fillColorFor(state);
		if (fill != null)
		{
			graphics.setColor(fill);
			graphics.fill(footprint);
		}

		graphics.setColor(outlineColorFor(state));
		graphics.setStroke(OUTLINE);
		graphics.draw(footprint);
	}

	/**
	 * Draws the current in-transit goat count on the pit, e.g. {@code "In transit: 2"}, so the player can see
	 * at a glance how many of their goats — lured or prodded — are on their way in. The number is preceded by
	 * the configured {@link InTransitPrefix} (nothing, an "In transit: " label, or the icon) and placed by the
	 * configured {@link InTransitPosition}.
	 */
	private void renderInTransit(Graphics2D graphics, GameObject pit)
	{
		InTransitPosition position = config.inTransitPosition();
		if (!position.isDrawn())
			return;

		InTransitPrefix prefix = config.inTransitPrefix();
		String text = inTransitText(prefix, transitTracker.inTransitCount());
		Point at = inTransitAnchor(graphics, pit, position, text);
		if (at == null)
			return;

		Color color = config.countLabelColor();
		if (prefix == InTransitPrefix.ICON && inTransitIcon != null)
			drawIconBefore(graphics, at, inTransitIcon, color, 1.0f, 0);

		drawText(graphics, at, text, color);
	}

	/**
	 * The in-transit label text for the configured prefix. "Text" prepends {@code "In transit: "}; "None" and
	 * "Icon" show the bare number (the icon draws separately, to the left of the number).
	 *
	 * @param prefix the configured in-transit prefix
	 * @param count  the number of goats in transit
	 * @return the label text to draw
	 */
	private static String inTransitText(InTransitPrefix prefix, int count)
	{
		if (prefix == InTransitPrefix.TEXT)
			return "In transit: " + count;

		return Integer.toString(count);
	}

	/**
	 * The canvas point the in-transit line is drawn at. The centre placement sits one line under the pit
	 * count; a compass placement sits on that pit tile, dropped one line under the total-caught label when
	 * both share the tile so the two stack instead of overlapping.
	 */
	private Point inTransitAnchor(Graphics2D graphics, GameObject pit, InTransitPosition position, String text)
	{
		if (position.isCenter())
		{
			Point at = pit.getCanvasTextLocation(graphics, text, 0);
			return at == null ? null : belowLine(graphics, at);
		}

		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		if (min == null || max == null)
			return null;

		int sceneX = position.sceneX(min.getX(), max.getX());
		int sceneY = position.sceneY(min.getY(), max.getY());
		LocalPoint tile = LocalPoint.fromScene(sceneX, sceneY, pit.getWorldView());
		Point at = Perspective.getCanvasTextLocation(client, graphics, tile, text, 0);
		if (at == null)
			return null;

		if (position.sharesTileWith(config.totalCaughtPosition()))
			return belowLine(graphics, at);

		return at;
	}

	/** Shifts a canvas point down by one text line so a second label stacks under the first. */
	private static Point belowLine(Graphics2D graphics, Point at)
	{
		return new Point(at.getX(), at.getY() + graphics.getFontMetrics().getHeight());
	}

	/**
	 * Draws the lifetime catch total on the pit corner chosen in the config, in Stockpile short form, so
	 * it sits clear of the centred count. North is +y and east is +x.
	 */
	private void renderTotalCaught(Graphics2D graphics, GameObject pit)
	{
		TotalCaughtPosition position = config.totalCaughtPosition();
		if (position == TotalCaughtPosition.OFF)
			return;

		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		if (min == null || max == null)
			return;

		int sceneX = position.sceneX(min.getX(), max.getX());
		int sceneY = position.sceneY(min.getY(), max.getY());
		String text = totalCaughtText();
		LocalPoint tile = LocalPoint.fromScene(sceneX, sceneY, pit.getWorldView());
		Point at = Perspective.getCanvasTextLocation(client, graphics, tile, text, 0);
		if (at == null)
			return;

		Color color = config.totalLabelColor();
		if (config.totalPrefix() == TotalPrefix.ANIMATED && totalIconFrames != null)
		{
			drawTotalIcon(graphics, at, color);
		}
		else if (config.totalPrefix() == TotalPrefix.ICON && totalIconFrames != null)
		{
			int idx = Math.min(STATIC_ICON_FRAME, totalIconFrames.length - 1);
			drawIconBefore(graphics, at, totalIconFrames[idx], color, 1.0f, 0);
		}

		drawText(graphics, at, text, color);
	}

	/**
	 * The total-caught label text for the configured prefix. "Text" prepends {@code "Total: "}; "None"
	 * and "Icon" show the bare number (the icon draws separately, to the left of the number).
	 */
	private String totalCaughtText()
	{
		String number = config.goatTotalFormat() == TotalCountFormat.FULL
			? ShortFormat.exact(catchCounter.getTotal())
			: ShortFormat.value(catchCounter.getTotal());
		if (config.totalPrefix() == TotalPrefix.TEXT)
			return "Total: " + number;

		return number;
	}

	/**
	 * Draws an icon just left of a label, vertically centred on the text and faded to the label color's
	 * alpha so it matches the number.
	 *
	 * @param graphics the overlay graphics
	 * @param at       the label's canvas anchor (the icon sits to its left)
	 * @param icon     the icon to draw
	 * @param color    the label color, whose alpha the icon is faded to
	 * @param fade     an extra opacity multiplier in {@code [0, 1]} for animated fade-outs
	 * @param xShift   extra pixels to draw the icon left of its resting spot, for the walk-in slide
	 */
	private static void drawIconBefore(Graphics2D graphics, Point at, BufferedImage icon, Color color, float fade,
		int xShift)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int iconX = at.getX() - icon.getWidth() - ICON_GAP - xShift;
		int iconY = at.getY() - metrics.getAscent() / 2 - icon.getHeight() / 2;
		Composite original = graphics.getComposite();
		float alpha = (color.getAlpha() / 255.0f) * fade;
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.drawImage(icon, iconX, iconY, null);
		graphics.setComposite(original);
	}

	/**
	 * Draws the total goat for the current animation phase. The goat rests on its neutral pose for a
	 * random 3-to-5-minute stretch, leaps into the hole, holds and fades the hole, then walks back into
	 * frame from the left before standing again. See {@link #advanceTotalAnim()} for the phase timing.
	 *
	 * @param graphics the overlay graphics
	 * @param at       the total label's canvas anchor (the icon sits to its left)
	 * @param color    the label color, whose alpha the icon is faded to
	 */
	private void drawTotalIcon(Graphics2D graphics, Point at, Color color)
	{
		advanceTotalAnim();
		long localT = System.currentTimeMillis() - totalPhaseStart;
		int last = totalIconFrames.length - 1;
		BufferedImage frame;
		float fade = 1.0f;
		int xShift = 0;
		switch (totalPhase)
		{
			case LEAPING:
				frame = totalIconFrames[Math.min((int) (localT / ANIM_FRAME_MS), last)];
				break;
			case HOLDING:
				frame = totalIconFrames[last];
				break;
			case FADING:
				frame = totalIconFrames[last];
				fade = 1.0f - localT / (float) ANIM_FADE_MS;
				break;
			case WALKING_IN:
				xShift = Math.round(ANIM_WALK_DISTANCE * (1.0f - localT / (float) ANIM_WALK_MS));
				boolean stride = (localT / ANIM_WALK_STEP_MS) % 2 == 0 && totalWalkFrame != null;
				frame = stride ? totalWalkFrame : totalIconFrames[0];
				break;
			case STANDING:
			default:
				frame = totalIconFrames[0];
				break;
		}
		drawIconBefore(graphics, at, frame, color, fade, xShift);
	}

	/**
	 * Advances the total-goat animation state machine to the current wall-clock time. Phases run
	 * STANDING (a random {@link #STAND_MIN_MS}-to-{@link #STAND_MAX_MS} rest on the neutral pose) then
	 * LEAPING, HOLDING, FADING and WALKING_IN before returning to a freshly-randomised STANDING. The
	 * loop catches up across any number of elapsed phases, so it stays correct when the overlay was not
	 * rendered for a while.
	 */
	private void advanceTotalAnim()
	{
		long now = System.currentTimeMillis();
		while (now - totalPhaseStart >= totalPhaseDuration())
		{
			totalPhaseStart += totalPhaseDuration();
			totalPhase = totalPhase.next();
			if (totalPhase == AnimPhase.STANDING)
				totalStandMs = randomStandMs();
		}
	}

	/** The duration in milliseconds of the current animation phase. */
	private long totalPhaseDuration()
	{
		switch (totalPhase)
		{
			case LEAPING:
				return (long) totalIconFrames.length * ANIM_FRAME_MS;
			case HOLDING:
				return ANIM_HOLD_MS;
			case FADING:
				return ANIM_FADE_MS;
			case WALKING_IN:
				return ANIM_WALK_MS;
			case STANDING:
			default:
				return totalStandMs;
		}
	}

	/** A fresh random standing duration in milliseconds, in {@code [STAND_MIN_MS, STAND_MAX_MS]}. */
	private static long randomStandMs()
	{
		return ThreadLocalRandom.current().nextLong(STAND_MIN_MS, STAND_MAX_MS + 1);
	}

	/**
	 * Loads a horizontal sprite strip of equal-width frames. The committed strip is pre-scaled to
	 * {@code height} with a sharp filter, so frames already at that height are used as-is to keep the
	 * pixel art crisp; a strip of any other height is scaled here as a fallback. A missing resource
	 * yields null so the caller falls back to drawing no icon.
	 *
	 * @param resource the strip resource name
	 * @param frames   the number of equal-width frames packed left to right
	 * @param height   the height in pixels each frame is presented at
	 * @return the frames at {@code height}, or null if the resource is missing
	 */
	private static BufferedImage[] loadIconStrip(String resource, int frames, int height)
	{
		BufferedImage raw = ImageUtil.loadImageResource(GoatPitOverlay.class, resource);
		if (raw == null)
			return null;

		int frameWidth = raw.getWidth() / frames;
		int scaledWidth = Math.max(1, Math.round(frameWidth * (height / (float) raw.getHeight())));
		BufferedImage[] result = new BufferedImage[frames];
		for (int i = 0; i < frames; i++)
		{
			BufferedImage frame = raw.getSubimage(i * frameWidth, 0, frameWidth, raw.getHeight());
			if (raw.getHeight() == height)
				result[i] = frame;
			else
				result[i] = ImageUtil.resizeImage(frame, scaledWidth, height);
		}

		return result;
	}

	/** Loads an icon from resources and scales it to {@link #ICON_HEIGHT}, or null if missing. */
	private static BufferedImage loadIcon(String resource)
	{
		BufferedImage raw = ImageUtil.loadImageResource(GoatPitOverlay.class, resource);
		if (raw == null)
			return null;

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
			return config.fullReminderFill();

		if (promptAddSpikes(state))
			return config.spikeReminderFill();

		return null;
	}

	private void renderLabels(Graphics2D graphics, GameObject pit, GoatPitState state)
	{
		if (config.showAddSpikes() && promptAddSpikes(state))
		{
			Point at = pit.getCanvasTextLocation(graphics, ADD_SPIKES_TEXT, 0);
			if (at != null)
				drawText(graphics, at, ADD_SPIKES_TEXT, config.countLabelColor());

			return;
		}

		if (!config.showCount())
			return;

		PitCountStyle style = config.pitCountStyle();
		if (style.showsBar())
		{
			drawProgressBar(graphics, pit, state, style.showsText());
			return;
		}

		Point at = pit.getCanvasTextLocation(graphics, state.label(), 0);
		if (at != null)
			drawText(graphics, at, state.label(), config.countLabelColor());
	}

	/**
	 * Draws a horizontal progress bar centred on the pit, filled from empty to full in step with the
	 * count and coloured with the same red-through-green gradient as the outline, so the bar reads as the
	 * pit's fill at a glance. When {@code withText} is set the {@code X / N} count is drawn over the bar.
	 */
	private void drawProgressBar(Graphics2D graphics, GameObject pit, GoatPitState state, boolean withText)
	{
		Point at = pit.getCanvasTextLocation(graphics, state.label(), 0);
		if (at == null)
			return;

		FontMetrics metrics = graphics.getFontMetrics();
		int centerX = at.getX() + metrics.stringWidth(state.label()) / 2;
		int left = centerX - BAR_WIDTH / 2;
		int top = at.getY() - metrics.getAscent() / 2 - BAR_HEIGHT / 2;
		float fraction = Math.max(0.0f, Math.min(1.0f, (float) state.getCount() / state.getCapacity()));
		Color progress = progressColor(state);
		graphics.setColor(BAR_BACKGROUND);
		graphics.fillRect(left, top, BAR_WIDTH, BAR_HEIGHT);
		graphics.setColor(progress);
		graphics.fillRect(left, top, Math.round(BAR_WIDTH * fraction), BAR_HEIGHT);
		graphics.setColor(withAlpha(progress, OUTLINE_ALPHA));
		graphics.setStroke(OUTLINE);
		graphics.drawRect(left, top, BAR_WIDTH, BAR_HEIGHT);
		if (withText)
		{
			int textX = left + (BAR_WIDTH - metrics.stringWidth(state.label())) / 2;
			int textY = top + (BAR_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2;
			drawText(graphics, new Point(textX, textY), state.label(), config.countLabelColor());
		}
	}

	/**
	 * The progress bar's fill color: the solid empty-outline color while the pit is unspiked, otherwise the
	 * same red-through-green gradient the outline uses, at full opacity so the fill reads clearly.
	 */
	private Color progressColor(GoatPitState state)
	{
		if (state.needsSpikes())
			return config.emptyOutlineColor();

		float fraction = (float) state.getCount() / state.getCapacity();
		return lerp3(
			config.emptyOutlineColor(), config.midpointOutlineColor(), config.fullOutlineColor(), fraction);
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
			return withAlpha(config.emptyOutlineColor(), OUTLINE_ALPHA);

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
			return lerp(from, mid, f * 2.0f);

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
			return null;

		Area area = new Area();
		for (int x = min.getX(); x <= max.getX(); x++)
		{
			for (int y = min.getY(); y <= max.getY(); y++)
			{
				LocalPoint tile = LocalPoint.fromScene(x, y, pit.getWorldView());
				Polygon poly = Perspective.getCanvasTilePoly(client, tile);
				if (poly != null)
					area.add(new Area(poly));
			}
		}

		return area;
	}

	/** Phases of the total-goat animation, cycled in order by {@link #next()}. */
	private enum AnimPhase
	{
		/** The goat rests on its neutral pose for a random interval; its default state. */
		STANDING,

		/** The goat plays its leap into the hole. */
		LEAPING,

		/** The hole is held fully opaque on screen. */
		HOLDING,

		/** The hole fades to fully transparent. */
		FADING,

		/** The goat walks back into frame from the left. */
		WALKING_IN;

		/** The next phase in the cycle; WALKING_IN wraps back to STANDING. */
		private AnimPhase next()
		{
			AnimPhase[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}
}
