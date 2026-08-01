/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.Collections;

import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link GoatHighlightOverlay#targetPitDistance}, the per-goat gate feeding the highlight gradient:
 * a grabbable goat yields its tile distance to the nearest pit it crosses, and every disqualifying case
 * (in transit, out of range, not across a pit) yields {@code -1}. The scene is mocked; a single spiked
 * pit sits at world (100,100)-(101,101), the player stands west of it at (98,100).
 */
public class GoatHighlightTargetingTest
{
	private final Client client = mock(Client.class);
	private final GoatTransitTracker transitTracker = mock(GoatTransitTracker.class);

	private final GoatHighlightOverlay overlay = new GoatHighlightOverlay(
		client, mock(GoatIndicatorsConfig.class), mock(GoatPitTracker.class), transitTracker,
		mock(LureSpells.class));

	private final WorldPoint playerLocation = new WorldPoint(98, 100, 0);
	private final GameObject pit = pit();
	private final Player player = player();

	@Test
	public void aGrabbableGoatYieldsItsDistanceToThePit()
	{
		when(transitTracker.isInTransit(7)).thenReturn(false);
		NPC goat = goat(7, new WorldPoint(104, 100, 0));

		assertEquals(4, overlay.targetPitDistance(goat, player, playerLocation, Collections.singletonList(pit)));
	}

	@Test
	public void aGoatOnThePlayersOwnSideOfThePitIsNotATarget()
	{
		when(transitTracker.isInTransit(7)).thenReturn(false);
		NPC goat = goat(7, new WorldPoint(99, 100, 0));

		assertEquals(-1, overlay.targetPitDistance(goat, player, playerLocation, Collections.singletonList(pit)));
	}

	@Test
	public void aGoatAlreadyInTransitIsNotATarget()
	{
		when(transitTracker.isInTransit(7)).thenReturn(true);
		NPC goat = goat(7, new WorldPoint(104, 100, 0));

		assertEquals(-1, overlay.targetPitDistance(goat, player, playerLocation, Collections.singletonList(pit)));
	}

	@Test
	public void aGoatBeyondCastRangeIsNotATarget()
	{
		when(transitTracker.isInTransit(7)).thenReturn(false);
		NPC goat = goat(7, new WorldPoint(120, 100, 0));

		assertEquals(-1, overlay.targetPitDistance(goat, player, playerLocation, Collections.singletonList(pit)));
	}

	/** A goat NPC in line of sight at the given location. */
	private NPC goat(int index, WorldPoint location)
	{
		WorldArea area = mock(WorldArea.class);
		WorldArea playerArea = player.getWorldArea();
		when(playerArea.hasLineOfSightTo(any(WorldView.class), any(WorldArea.class))).thenReturn(true);
		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Goat");
		when(npc.getIndex()).thenReturn(index);
		when(npc.getWorldLocation()).thenReturn(location);
		when(npc.getWorldArea()).thenReturn(area);
		return npc;
	}

	/** The local player, standing west of the pit, with a stubbable world area for line-of-sight. */
	private Player player()
	{
		WorldArea area = mock(WorldArea.class);
		Player local = mock(Player.class);
		when(local.getWorldArea()).thenReturn(area);
		return local;
	}

	/** A spiked pit occupying world tiles (100,100) to (101,101) on plane 0. */
	private GameObject pit()
	{
		WorldView worldView = mock(WorldView.class);
		when(worldView.getBaseX()).thenReturn(0);
		when(worldView.getBaseY()).thenReturn(0);
		when(client.getTopLevelWorldView()).thenReturn(worldView);
		GameObject object = mock(GameObject.class);
		when(object.getWorldLocation()).thenReturn(new WorldPoint(100, 100, 0));
		when(object.getSceneMinLocation()).thenReturn(new Point(100, 100));
		when(object.getSceneMaxLocation()).thenReturn(new Point(101, 101));
		when(object.getWorldView()).thenReturn(worldView);
		when(object.getPlane()).thenReturn(0);
		return object;
	}
}
