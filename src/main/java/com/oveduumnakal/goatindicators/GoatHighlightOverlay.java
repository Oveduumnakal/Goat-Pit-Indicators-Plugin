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
import java.util.Map;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
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
 *
 * <p>While a Cattleprod is equipped (and the prodable highlight is enabled) this takes over the outline
 * instead: goats within prod range of a catching pit are outlined in the prod color, shaded from closest
 * to furthest by distance to the pit under the same near/far gradient. The prod highlight supersedes the
 * telegrab one so the two never fight over the outline.
 */
class GoatHighlightOverlay extends Overlay
{
	private static final Stroke OUTLINE_STROKE = new BasicStroke(2.0f);

	/**
	 * How far, in tiles, the prod-from-location flood searches for a way to the goat. A walk longer than
	 * this counts as repositioning rather than prodding from where you stand, so the goat is left out.
	 */
	private static final int PATH_RADIUS = 16;

	private final Client client;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;
	private final GoatTransitTracker transitTracker;
	private final LureSpells lureSpells;

	/** The tile the last prod-from-location flood started from, so the flood is reused until the player moves. */
	private WorldPoint cachedPathOrigin;

	/** The reach map from the last flood, keyed by {@link ProdPathing#key(int, int)}. */
	private Map<Long, Integer> cachedReach;

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
		if (player == null)
			return null;

		if (config.highlightProdable() && cattleprodEquipped())
		{
			renderProdable(graphics, player.getWorldLocation());
			return null;
		}

		if (!config.highlightTelegrab() || !lureSpells.canLure())
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
	 * Outlines every prodable goat, shaded from the closest to the furthest prod color by tile distance to
	 * the pit. A goat is prodable when it is within the configured prod range of a catching pit and not
	 * already committed to one. Drawn in place of the telegrab highlight while a Cattleprod is equipped (the
	 * caller gates on that), so the two never fight over the outline. The near/far gradient is shared with
	 * the telegrab highlight — off draws every prodable goat in the closest prod color. When the
	 * prod-from-location toggle is on, only goats the player can prod in without repositioning are drawn —
	 * judged from the tile a click would actually walk them to, not their current tile.
	 */
	private void renderProdable(Graphics2D graphics, WorldPoint playerLocation)
	{
		List<GameObject> catchingPits = catchingPits();
		if (catchingPits.isEmpty())
			return;

		boolean fromLocation = config.prodFromLocation();
		Map<Long, Integer> reach = fromLocation ? reachFromPlayer(playerLocation) : null;

		List<NPC> targets = new ArrayList<>();
		List<Integer> distances = new ArrayList<>();
		int nearest = Integer.MAX_VALUE;
		int farthest = Integer.MIN_VALUE;
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			WorldPoint prodOrigin = fromLocation ? landingTileFor(npc, reach, playerLocation) : null;
			GameObject pit = prodPitFor(npc, prodOrigin, catchingPits);
			if (pit == null)
				continue;

			int distance = prodDistance(pit, npc.getWorldLocation());
			targets.add(npc);
			distances.add(distance);
			nearest = Math.min(nearest, distance);
			farthest = Math.max(farthest, distance);
		}

		Color closest = config.prodColor();
		Color furthest = config.telegrabGradient() ? config.prodFurthestColor() : closest;
		for (int i = 0; i < targets.size(); i++)
		{
			float fraction = TelegrabTargeting.priorityFraction(distances.get(i), nearest, farthest);
			drawOutline(graphics, targets.get(i), lerp(closest, furthest, fraction));
		}
	}

	/**
	 * The catching pit a goat can be prodded into, or {@code null} when it cannot. A goat qualifies when it
	 * is a goat, not already in transit to a pit, and within prod range of at least one catching pit; the
	 * nearest such pit is returned. When the prod-from-location toggle is on, the prod is judged from
	 * {@code prodOrigin} — the tile a click would path the player to — so a pit only counts if a prod from
	 * that tile shoves the goat into it; an unreachable goat (null origin) never qualifies.
	 */
	GameObject prodPitFor(NPC npc, WorldPoint prodOrigin, List<GameObject> catchingPits)
	{
		if (npc == null || !GoatPitTracker.matchesGoatName(npc.getName()))
			return null;

		if (transitTracker.isInTransit(npc.getIndex()))
			return null;

		WorldPoint goatLocation = npc.getWorldLocation();
		if (goatLocation == null)
			return null;

		boolean fromLocation = config.prodFromLocation();
		if (fromLocation && prodOrigin == null)
			return null;

		GameObject nearest = null;
		int best = -1;
		for (GameObject pit : catchingPits)
		{
			int distance = prodDistance(pit, goatLocation);
			if (distance < 0 || !ProdTargeting.withinProdRange(distance))
				continue;

			if (fromLocation && !pitInPushDirection(pit, prodOrigin, goatLocation))
				continue;

			if (best < 0 || distance < best)
			{
				best = distance;
				nearest = pit;
			}
		}

		return nearest;
	}

	/**
	 * Floods the walkable tiles out from the player to predict where a click would land, reusing the last
	 * result until the player moves. The walkability of each step is taken from the scene collision map via
	 * {@link WorldArea#canTravelInDirection}. Returns {@code null} when the player location is unknown.
	 */
	private Map<Long, Integer> reachFromPlayer(WorldPoint playerLocation)
	{
		if (playerLocation == null)
			return null;

		if (playerLocation.equals(cachedPathOrigin))
			return cachedReach;

		WorldView worldView = client.getTopLevelWorldView();
		int plane = playerLocation.getPlane();
		ProdPathing.StepFn step = (x, y, dx, dy) ->
			new WorldArea(new WorldPoint(x, y, plane), 1, 1).canTravelInDirection(worldView, dx, dy);
		cachedReach = ProdPathing.reachDistances(playerLocation.getX(), playerLocation.getY(), step, PATH_RADIUS);
		cachedPathOrigin = playerLocation;
		return cachedReach;
	}

	/**
	 * The tile a click on this goat would path the player to, or {@code null} when no tile beside the goat
	 * is reachable within {@link #PATH_RADIUS}. That tile is the origin the prod's shove is judged from.
	 */
	private WorldPoint landingTileFor(NPC npc, Map<Long, Integer> reach, WorldPoint playerLocation)
	{
		if (reach == null)
			return null;

		WorldPoint goatLocation = npc.getWorldLocation();
		if (goatLocation == null)
			return null;

		int[] tile = ProdPathing.landingTile(goatLocation.getX(), goatLocation.getY(), reach);
		if (tile == null)
			return null;

		return new WorldPoint(tile[0], tile[1], playerLocation.getPlane());
	}

	/**
	 * Whether a prod from {@code prodOrigin} — the tile a click would walk the player to — would shove the
	 * goat into this pit. Resolves the pit's world footprint and defers the geometry to
	 * {@link ProdTargeting#pitInPushDirection}. A missing origin or pit footprint yields false.
	 */
	private boolean pitInPushDirection(GameObject pit, WorldPoint prodOrigin, WorldPoint goatLocation)
	{
		if (prodOrigin == null)
			return false;

		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		WorldView worldView = pit.getWorldView();
		if (min == null || max == null || worldView == null)
			return false;

		int baseX = worldView.getBaseX();
		int baseY = worldView.getBaseY();
		return ProdTargeting.pitInPushDirection(
			prodOrigin.getX(), prodOrigin.getY(), goatLocation.getX(), goatLocation.getY(),
			baseX + min.getX(), baseY + min.getY(), baseX + max.getX(), baseY + max.getY());
	}

	/**
	 * The Chebyshev tile distance from a goat to the nearest tile of a pit, or {@code -1} when the pit's
	 * footprint or plane is unavailable or differs from the goat's.
	 */
	private static int prodDistance(GameObject pit, WorldPoint goatLocation)
	{
		Point min = pit.getSceneMinLocation();
		Point max = pit.getSceneMaxLocation();
		WorldView worldView = pit.getWorldView();
		if (min == null || max == null || worldView == null || goatLocation.getPlane() != pit.getPlane())
			return -1;

		int goatSceneX = goatLocation.getX() - worldView.getBaseX();
		int goatSceneY = goatLocation.getY() - worldView.getBaseY();
		int nearestX = ProdTargeting.clampToPit(goatSceneX, min.getX(), max.getX());
		int nearestY = ProdTargeting.clampToPit(goatSceneY, min.getY(), max.getY());
		return Math.max(Math.abs(goatSceneX - nearestX), Math.abs(goatSceneY - nearestY));
	}

	/** Whether a Cattleprod is in the worn-equipment container, enabling the prodable highlight. */
	private boolean cattleprodEquipped()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
			return false;

		for (Item item : worn.getItems())
		{
			if (item != null && item.getId() == ItemID.CATTLEPROD)
				return true;
		}

		return false;
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
