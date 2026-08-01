/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.Collections;

import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the two conditions gating the spike-supply highlight: whether any loaded pit is prompting for
 * spikes, and whether the player is already carrying spikes (in which case the supply stays quiet). Both
 * read the live client, so tracker, inventory and pit state are mocked.
 */
public class GoatPitOverlaySupplyTest
{
	private final Client client = mock(Client.class);
	private final GoatIndicatorsConfig config = mock(GoatIndicatorsConfig.class);
	private final GoatPitTracker tracker = mock(GoatPitTracker.class);
	private final GoatTransitTracker transitTracker = mock(GoatTransitTracker.class);
	private final GoatCatchCounter catchCounter = mock(GoatCatchCounter.class);

	private final GoatPitOverlay overlay =
		new GoatPitOverlay(client, config, tracker, transitTracker, catchCounter);

	@Test
	public void spikesInTheInventoryCount()
	{
		ItemContainer inventory = container(item(GoatIds.SPIKES_ITEM_ID, 1));
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		assertTrue(overlay.playerHasSpikes());
	}

	@Test
	public void noSpikesWhenTheInventoryLacksThemOrIsUnavailable()
	{
		ItemContainer inventory = container(item(GoatIds.SPIKES_ITEM_ID + 1, 5));
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		assertFalse(overlay.playerHasSpikes());

		when(client.getItemContainer(InventoryID.INV)).thenReturn(null);
		assertFalse(overlay.playerHasSpikes());
	}

	@Test
	public void aPitPromptsForSpikesOnlyWhileEmptyAndUnspiked()
	{
		GameObject pit = mock(GameObject.class);
		when(tracker.getPits()).thenReturn(Collections.singletonList(pit));

		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(0, false, 16));
		assertTrue(overlay.anyPitNeedsSpikes());

		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(0, true, 16));
		assertFalse(overlay.anyPitNeedsSpikes());

		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(5, false, 16));
		assertFalse(overlay.anyPitNeedsSpikes());
	}

	/** A mock item container returning exactly the given items. */
	private static ItemContainer container(Item... items)
	{
		ItemContainer inventory = mock(ItemContainer.class);
		when(inventory.getItems()).thenReturn(items);
		return inventory;
	}

	/** A mock item with the given id and quantity. */
	private static Item item(int id, int quantity)
	{
		Item item = mock(Item.class);
		when(item.getId()).thenReturn(id);
		when(item.getQuantity()).thenReturn(quantity);
		return item;
	}
}
