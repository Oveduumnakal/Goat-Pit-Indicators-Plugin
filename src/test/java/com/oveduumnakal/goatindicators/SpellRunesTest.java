/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers rune availability across the inventory and the rune pouch, and in particular the guard that keeps
 * the pouch's type/quantity varbits from being trusted once the pouch has been banked — the varbits keep
 * their last values, so reading them unconditionally would count runes the player no longer carries.
 */
public class SpellRunesTest
{
	@Mock
	private Client client;

	@InjectMocks
	private SpellRunes runes;

	@Before
	public void setUp()
	{
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void aRuneInTheInventoryIsHeld()
	{
		ItemContainer inv = container(item(ItemID.LAWRUNE, 1));
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inv);
		assertTrue(runes.has(ItemID.LAWRUNE));
	}

	@Test
	public void nothingIsHeldWhenTheInventoryIsUnavailable()
	{
		when(client.getItemContainer(InventoryID.INV)).thenReturn(null);
		assertFalse(runes.has(ItemID.LAWRUNE));
	}

	@Test
	public void bankedPouchVarbitsAreNotTrustedWithoutThePouch()
	{
		ItemContainer inv = container(item(ItemID.AIRRUNE, 1));
		EnumComposition map = runeMap(5, ItemID.NATURERUNE);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inv);
		when(client.getEnum(EnumID.RUNEPOUCH_RUNE)).thenReturn(map);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_1)).thenReturn(5);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_1)).thenReturn(1);

		assertFalse(runes.has(ItemID.NATURERUNE));
	}

	@Test
	public void aRuneInACarriedPouchIsHeld()
	{
		ItemContainer inv = container(item(ItemID.BH_RUNE_POUCH, 1));
		EnumComposition map = runeMap(5, ItemID.NATURERUNE);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inv);
		when(client.getEnum(EnumID.RUNEPOUCH_RUNE)).thenReturn(map);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_1)).thenReturn(5);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_1)).thenReturn(1);

		assertTrue(runes.has(ItemID.NATURERUNE));
	}

	@Test
	public void hasAnyIsTrueOnlyWhenOneOfTheRunesIsPresent()
	{
		ItemContainer inv = container(item(ItemID.AIRRUNE, 1));
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inv);
		assertTrue(runes.hasAny(Set.of(ItemID.AIRRUNE, ItemID.DUSTRUNE)));
		assertFalse(runes.hasAny(Set.of(ItemID.DUSTRUNE, ItemID.MISTRUNE)));
	}

	/** A mock item container returning exactly the given items. */
	private static ItemContainer container(Item... items)
	{
		ItemContainer inv = mock(ItemContainer.class);
		when(inv.getItems()).thenReturn(items);
		return inv;
	}

	/** A mock item with the given id and quantity. */
	private static Item item(int id, int quantity)
	{
		Item item = mock(Item.class);
		when(item.getId()).thenReturn(id);
		when(item.getQuantity()).thenReturn(quantity);
		return item;
	}

	/** A mock rune-pouch enum mapping one type key to one rune item id. */
	private static EnumComposition runeMap(int typeKey, int runeId)
	{
		EnumComposition map = mock(EnumComposition.class);
		when(map.getIntValue(typeKey)).thenReturn(runeId);
		return map;
	}
}
