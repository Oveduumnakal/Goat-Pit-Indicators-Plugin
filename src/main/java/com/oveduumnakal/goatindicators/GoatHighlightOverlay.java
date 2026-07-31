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
 * Glows a purple clickbox outline over every goat that is worth luring into a pit right now.
 *
 * <p>A goat is highlighted only when all of these hold: the player can cast a lure spell — Telekinetic
 * Grab or Dark Lure (level, spellbook, runes); a nearby pit has spikes and is not yet full; the goat is
 * within the spell's 10-tile reach; and the goat sits on the far side of that pit from the player, so a
 * cast lures it across the pit into the trap. The outline is the goat's convex hull with no fill,
 * breathing at Stockpile's glow rate.
 */
class GoatHighlightOverlay extends Overlay
{
	private static final Stroke GLOW_STROKE = new BasicStroke(2.0f);

	/** Alpha for the in-transit hull fill: 25% of full opacity, per the feature's 0.25 alpha. */
	private static final int IN_TRANSIT_FILL_ALPHA = (int) Math.round(255 * 0.25);

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
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldLocation() == null)
		{
			return null;
		}
		drawInTransitFills(graphics);
		drawGrabbableHighlights(graphics, player);
		return null;
	}

	/**
	 * Fills the hull of each goat currently in transit with the telegrab color at a low alpha, so the goats
	 * the player has lured stay visually tracked across their whole flight and walk to the pit. This runs
	 * regardless of whether a fresh grab is possible — a goat already committed is worth marking even when
	 * the pit is full or the player is out of runes. The solid outline is reserved for grabbable goats, so
	 * an in-transit goat is filled, never outlined.
	 */
	private void drawInTransitFills(Graphics2D graphics)
	{
		if (!config.fillInTransit())
		{
			return;
		}
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && GoatPitTracker.matchesGoatName(npc.getName())
				&& transitTracker.isInTransit(npc.getIndex()))
			{
				drawFill(graphics, npc);
			}
		}
	}

	/**
	 * Outlines every goat worth grabbing right now, breathing at Stockpile's glow rate. Silent unless the
	 * player can cast a lure and a nearby pit still has room.
	 */
	private void drawGrabbableHighlights(Graphics2D graphics, Player player)
	{
		if (!config.highlightTelegrab() || !lureSpells.canLure())
		{
			return;
		}
		List<GameObject> catchingPits = catchingPits();
		if (catchingPits.isEmpty())
		{
			return;
		}
		WorldPoint playerLocation = player.getWorldLocation();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (isTelegrabTarget(npc, player, playerLocation, catchingPits))
			{
				drawGlow(graphics, npc);
			}
		}
	}

	/** The pits that can still take a goat: spiked and not yet full. */
	private List<GameObject> catchingPits()
	{
		List<GameObject> catching = new ArrayList<>();
		for (GameObject pit : tracker.getPits())
		{
			GoatPitState state = tracker.stateOf(pit);
			if (!state.isFull() && !state.needsSpikes())
			{
				catching.add(pit);
			}
		}
		return catching;
	}

	/**
	 * Whether this goat should glow: it is a goat, not already in transit, in cast range of the player,
	 * in the player's line of sight, and on the far side of at least one catching pit. A goat mid-transit
	 * cannot be grabbed again, so it is excluded to avoid inviting a wasted cast; a goat without line of
	 * sight is excluded because casting on it makes the player walk to gain sight instead of grabbing in
	 * place.
	 */
	private boolean isTelegrabTarget(NPC npc, Player player, WorldPoint playerLocation,
		List<GameObject> catchingPits)
	{
		if (npc == null || !GoatPitTracker.matchesGoatName(npc.getName()))
		{
			return false;
		}
		if (transitTracker.isInTransit(npc.getIndex()))
		{
			return false;
		}
		WorldPoint goatLocation = npc.getWorldLocation();
		if (goatLocation == null || !TelegrabTargeting.withinCastRange(playerLocation.distanceTo(goatLocation)))
		{
			return false;
		}
		if (!hasLineOfSight(player, npc))
		{
			return false;
		}
		for (GameObject pit : catchingPits)
		{
			if (isAcrossPit(pit, playerLocation, goatLocation))
			{
				return true;
			}
		}
		return false;
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
		{
			return false;
		}
		return playerArea.hasLineOfSightTo(client.getTopLevelWorldView(), goatArea);
	}

	/** Whether player and goat sit on opposite sides of this pit, using the pit's world footprint. */
	private boolean isAcrossPit(GameObject pit, WorldPoint playerLocation, WorldPoint goatLocation)
	{
		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		WorldView worldView = pit.getWorldView();
		if (min == null || max == null || worldView == null || goatLocation.getPlane() != pit.getPlane())
		{
			return false;
		}
		int baseX = worldView.getBaseX();
		int baseY = worldView.getBaseY();
		return TelegrabTargeting.oppositeSideOfPit(
			baseX + min.getX(), baseY + min.getY(), baseX + max.getX(), baseY + max.getY(),
			playerLocation.getX(), playerLocation.getY(), goatLocation.getX(), goatLocation.getY());
	}

	/** Fills the goat's clickbox with the telegrab color at {@link #IN_TRANSIT_FILL_ALPHA}, no outline. */
	private void drawFill(Graphics2D graphics, NPC npc)
	{
		Shape hull = npc.getConvexHull();
		if (hull == null)
		{
			return;
		}
		Color base = config.telegrabColor();
		graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), IN_TRANSIT_FILL_ALPHA));
		graphics.fill(hull);
	}

	/** Draws the goat's clickbox outline with no fill, breathing at Stockpile's glow rate. */
	private void drawGlow(Graphics2D graphics, NPC npc)
	{
		Shape hull = npc.getConvexHull();
		if (hull == null)
		{
			return;
		}
		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Pulse.alpha()));
		graphics.setColor(config.telegrabColor());
		graphics.setStroke(GLOW_STROKE);
		graphics.draw(hull);
		graphics.setComposite(original);
	}
}
