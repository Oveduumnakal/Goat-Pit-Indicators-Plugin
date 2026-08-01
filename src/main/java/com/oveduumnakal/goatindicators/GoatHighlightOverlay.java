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
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws a purple clickbox outline over every goat that is worth luring into a pit right now.
 *
 * <p>A goat is highlighted only when all of these hold: the player can cast a lure spell — Telekinetic
 * Grab or Dark Lure (level, spellbook, runes); a nearby pit has spikes and is not yet full; the goat is
 * within the spell's 10-tile reach; and the goat sits on the far side of that pit from the player, so a
 * cast lures it across the pit into the trap. The outline is the goat's convex hull with no fill, drawn
 * steadily at the outline color's own alpha.
 */
class GoatHighlightOverlay extends Overlay
{
	private static final Stroke OUTLINE_STROKE = new BasicStroke(2.0f);

	private final Client client;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;
	private final GoatTransitTracker transitTracker;
	private final LureSpells lureSpells;

	@Inject
	GoatHighlightOverlay(Client client, GoatIndicatorsConfig config, GoatPitTracker tracker,
		GoatTransitTracker transitTracker, LureSpells lureSpells)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.transitTracker = transitTracker;
		this.lureSpells = lureSpells;
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightTelegrab() || !lureSpells.canLure())
			return null;

		Player player = client.getLocalPlayer();
		if (player == null)
			return null;

		WorldPoint playerLocation = player.getWorldLocation();
		if (playerLocation == null)
			return null;

		List<GameObject> catchingPits = catchingPits();
		if (catchingPits.isEmpty())
			return null;

		List<NPC> targets = new ArrayList<>();
		List<Integer> distances = new ArrayList<>();
		int nearest = Integer.MAX_VALUE;
		int farthest = Integer.MIN_VALUE;
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			int distance = targetPitDistance(npc, player, playerLocation, catchingPits);
			if (distance < 0)
				continue;

			targets.add(npc);
			distances.add(distance);
			nearest = Math.min(nearest, distance);
			farthest = Math.max(farthest, distance);
		}

		Color closest = config.telegrabColor();
		Color furthest = config.telegrabGradient() ? config.telegrabFurthestColor() : closest;
		for (int i = 0; i < targets.size(); i++)
		{
			float fraction = TelegrabTargeting.priorityFraction(distances.get(i), nearest, farthest);
			drawOutline(graphics, targets.get(i), lerp(closest, furthest, fraction));
		}

		return null;
	}

	/**
	 * The pits that can still take a goat: spiked and not yet effectively full. Effectively full — the
	 * count plus the goats already lured or prodded toward the pit — is the same test the menu swapper
	 * uses, so the highlight stops glowing fresh targets exactly when a further cast would overfill,
	 * rather than only once the pit's raw count reaches capacity.
	 */
	private List<GameObject> catchingPits()
	{
		int inTransit = transitTracker.inTransitCount();
		List<GameObject> catching = new ArrayList<>();
		for (GameObject pit : tracker.getPits())
		{
			GoatPitState state = tracker.stateOf(pit);
			if (!state.needsSpikes()
					&& !TelegrabTargeting.effectivelyFull(state.getCount(), inTransit, state.getCapacity()))
				catching.add(pit);
		}

		return catching;
	}

	/**
	 * The tile distance from a grabbable goat to the nearest catching pit it can be lured across, or
	 * {@code -1} when the goat is not a valid target. A goat qualifies when it is a goat, not already in
	 * transit, in cast range of the player, in the player's line of sight, and on the far side of at least
	 * one catching pit. A goat mid-transit cannot be grabbed again, so it is excluded to avoid inviting a
	 * wasted cast; a goat without line of sight is excluded because casting on it makes the player walk to
	 * gain sight instead of grabbing in place. The returned distance drives the closest-to-furthest
	 * gradient — the smaller it is, the higher the goat's grab priority.
	 */
	int targetPitDistance(NPC npc, Player player, WorldPoint playerLocation,
		List<GameObject> catchingPits)
	{
		if (npc == null || !GoatPitTracker.matchesGoatName(npc.getName()))
			return -1;

		if (transitTracker.isInTransit(npc.getIndex()))
			return -1;

		WorldPoint goatLocation = npc.getWorldLocation();
		if (goatLocation == null || !TelegrabTargeting.withinCastRange(playerLocation.distanceTo(goatLocation)))
			return -1;

		if (!hasLineOfSight(player, npc))
			return -1;

		int best = -1;
		for (GameObject pit : catchingPits)
		{
			WorldPoint pitLocation = pit.getWorldLocation();
			if (pitLocation == null || !isAcrossPit(pit, playerLocation, goatLocation))
				continue;

			int distance = goatLocation.distanceTo(pitLocation);
			if (best < 0 || distance < best)
				best = distance;
		}

		return best;
	}

	/**
	 * Whether the player has line of sight to the goat, using the scene collision map. Telekinetic Grab
	 * needs sight of its target; a goat within range but behind the pit wall or terrain cannot be grabbed
	 * in place, so highlighting it would only lure the player into walking. A missing world area (goat or
	 * player off the loaded scene) is treated as no sight.
	 */
	private boolean hasLineOfSight(Player player, NPC npc)
	{
		WorldArea playerArea = player.getWorldArea();
		WorldArea goatArea = npc.getWorldArea();
		if (playerArea == null || goatArea == null)
			return false;

		return playerArea.hasLineOfSightTo(client.getTopLevelWorldView(), goatArea);
	}

	/** Whether player and goat sit on opposite sides of this pit, using the pit's world footprint. */
	private boolean isAcrossPit(GameObject pit, WorldPoint playerLocation, WorldPoint goatLocation)
	{
		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		WorldView worldView = pit.getWorldView();
		if (min == null || max == null || worldView == null || goatLocation.getPlane() != pit.getPlane())
			return false;

		int baseX = worldView.getBaseX();
		int baseY = worldView.getBaseY();
		return TelegrabTargeting.oppositeSideOfPit(
			baseX + min.getX(), baseY + min.getY(), baseX + max.getX(), baseY + max.getY(),
			playerLocation.getX(), playerLocation.getY(), goatLocation.getX(), goatLocation.getY());
	}

	/** Draws the goat's clickbox outline steadily in the given color with no fill, at the color's own alpha. */
	private void drawOutline(Graphics2D graphics, NPC npc, Color color)
	{
		Shape hull = npc.getConvexHull();
		if (hull == null)
			return;

		graphics.setColor(color);
		graphics.setStroke(OUTLINE_STROKE);
		graphics.draw(hull);
	}

	/**
	 * Linearly blends two colors including their alpha; {@code fraction} is clamped to 0..1. Used to place
	 * each grabbable goat's outline along the closest-to-furthest gradient.
	 */
	private static Color lerp(Color from, Color to, float fraction)
	{
		float f = Math.max(0.0f, Math.min(1.0f, fraction));
		int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * f);
		int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * f);
		int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * f);
		int a = Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * f);
		return new Color(r, g, b, a);
	}
}
