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
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link GoatHighlightOverlay#prodPitFor}, the gate feeding the prodable highlight: a goat within
 * prod range of a catching pit yields that pit, and every disqualifying case (out of range, in transit,
 * wrong plane) yields {@code null}. The pit occupies world tiles (100,100) to (101,101) on plane 0.
 */
public class GoatProdTargetingTest
{
	private final GoatTransitTracker transitTracker = mock(GoatTransitTracker.class);

	private final GoatHighlightOverlay overlay = new GoatHighlightOverlay(
		mock(Client.class), mock(GoatIndicatorsConfig.class), mock(GoatPitTracker.class), transitTracker,
		mock(LureSpells.class));

	private final GameObject pit = pit();

	@Test
	public void aGoatWithinProdRangeYieldsThePit()
	{
		when(transitTracker.isInTransit(7)).thenReturn(false);
		NPC goat = goat(7, new WorldPoint(103, 100, 0));

		assertSame(pit, overlay.prodPitFor(goat, Collections.singletonList(pit)));
	}

	@Test
	public void aGoatBeyondProdRangeYieldsNothing()
	{
		when(transitTracker.isInTransit(7)).thenReturn(false);
		NPC goat = goat(7, new WorldPoint(110, 100, 0));

		assertNull(overlay.prodPitFor(goat, Collections.singletonList(pit)));
	}

	@Test
	public void aGoatAlreadyInTransitYieldsNothing()
	{
		when(transitTracker.isInTransit(7)).thenReturn(true);
		NPC goat = goat(7, new WorldPoint(103, 100, 0));

		assertNull(overlay.prodPitFor(goat, Collections.singletonList(pit)));
	}

	@Test
	public void aGoatOnAnotherPlaneYieldsNothing()
	{
		when(transitTracker.isInTransit(7)).thenReturn(false);
		NPC goat = goat(7, new WorldPoint(103, 100, 1));

		assertNull(overlay.prodPitFor(goat, Collections.singletonList(pit)));
	}

	/** A goat NPC at the given location. */
	private static NPC goat(int index, WorldPoint location)
	{
		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Goat");
		when(npc.getIndex()).thenReturn(index);
		when(npc.getWorldLocation()).thenReturn(location);
		return npc;
	}

	/** A pit occupying world tiles (100,100) to (101,101) on plane 0, at scene base (0,0). */
	private static GameObject pit()
	{
		WorldView worldView = mock(WorldView.class);
		when(worldView.getBaseX()).thenReturn(0);
		when(worldView.getBaseY()).thenReturn(0);
		GameObject object = mock(GameObject.class);
		when(object.getSceneMinLocation()).thenReturn(new Point(100, 100));
		when(object.getSceneMaxLocation()).thenReturn(new Point(101, 101));
		when(object.getWorldView()).thenReturn(worldView);
		when(object.getPlane()).thenReturn(0);
		return object;
	}
}
