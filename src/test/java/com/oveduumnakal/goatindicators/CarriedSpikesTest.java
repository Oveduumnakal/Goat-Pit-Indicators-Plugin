/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import org.junit.Test;

import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the carried-spikes read and the restock rule (#103): every spikes stack is summed, other items and a
 * missing inventory count as none, and a restock is due only for a full pit with no spikes carried.
 */
public class CarriedSpikesTest
{
	@Test
	public void sumsEverySpikesStackAndIgnoresOtherItems()
	{
		Item[] items = {
			item(GoatIds.SPIKES_ITEM_ID, 1), null, item(GoatIds.SPIKES_ITEM_ID + 1, 9), item(GoatIds.SPIKES_ITEM_ID, 2)
		};
		ItemContainer inventory = mock(ItemContainer.class);
		when(inventory.getItems()).thenReturn(items);

		assertEquals(3, CarriedSpikes.countIn(inventory));
	}

	@Test
	public void aMissingInventoryHoldsNone()
	{
		assertEquals(0, CarriedSpikes.countIn(null));
	}

	@Test
	public void restockIsDueOnlyForAFullPitWithNoSpikesCarried()
	{
		assertTrue(CarriedSpikes.needsRestock(new GoatPitState(16, true, 16), 0));
		assertFalse(CarriedSpikes.needsRestock(new GoatPitState(16, true, 16), 1));
		assertFalse(CarriedSpikes.needsRestock(new GoatPitState(15, true, 16), 0));
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
