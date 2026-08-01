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

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Counts whether a rune is available to pay for a spell, reading both the inventory and the rune pouch.
 *
 * <p>Every lure spell pays in runes, and each counts them the same way, so the inventory-plus-pouch lookup
 * lives here once rather than being copied into {@link TelekineticGrab} and {@link DarkLure}. The pouch is
 * read through the standard type/quantity varbit pairs (three standard slots plus the divine fourth) mapped
 * to item ids by the {@link EnumID#RUNEPOUCH_RUNE} enum, but only when a rune pouch is actually carried:
 * those varbits keep their last values after the pouch is banked, so reading them unconditionally would
 * count runes the player no longer has.
 */
@Singleton
class SpellRunes
{
	/** Rune-pouch item ids whose presence in the inventory means the pouch varbits can be trusted. */
	private static final Set<Integer> RUNE_POUCH_IDS = Set.of(
		ItemID.BH_RUNE_POUCH,
		ItemID.BH_RUNE_POUCH_TROUVER,
		ItemID.DIVINE_RUNE_POUCH,
		ItemID.DIVINE_RUNE_POUCH_TROUVER);

	/** Rune-pouch type/quantity varbit pairs, covering the standard (1-3) and divine (4th) pouches. */
	private static final int[] POUCH_TYPE_VARBITS =
	{
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
		VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4,
	};

	private static final int[] POUCH_QUANTITY_VARBITS =
	{
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4,
	};

	@Inject
	private Client client;

	/** Whether the given rune id is held in the inventory or the rune pouch. */
	boolean has(int runeId)
	{
		return inventoryHas(runeId) || pouchHas(runeId);
	}

	/** Whether any one of the given rune ids is available, for a cost met by several combination runes. */
	boolean hasAny(Set<Integer> runeIds)
	{
		for (int runeId : runeIds)
		{
			if (has(runeId))
				return true;
		}
		return false;
	}

	private boolean inventoryHas(int runeId)
	{
		return containerHas(client.getItemContainer(InventoryID.INV), runeId);
	}

	private static boolean containerHas(ItemContainer container, int itemId)
	{
		if (container == null)
			return false;

		for (Item item : container.getItems())
		{
			if (item != null && item.getId() == itemId && item.getQuantity() > 0)
				return true;
		}
		return false;
	}

	private boolean pouchHas(int runeId)
	{
		if (!pouchCarried())
			return false;

		EnumComposition runeMap = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runeMap == null)
			return false;

		for (int slot = 0; slot < POUCH_TYPE_VARBITS.length; slot++)
		{
			int type = client.getVarbitValue(POUCH_TYPE_VARBITS[slot]);
			if (type <= 0 || client.getVarbitValue(POUCH_QUANTITY_VARBITS[slot]) <= 0)
				continue;

			if (runeMap.getIntValue(type) == runeId)
				return true;
		}
		return false;
	}

	/**
	 * Whether a rune pouch (regular, divine, or a trouver-locked variant) is in the inventory. The pouch
	 * varbits keep their last values after the pouch is banked, so they are only trusted while the pouch is
	 * actually carried.
	 *
	 * @return true when a rune pouch item is held in the inventory
	 */
	private boolean pouchCarried()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
			return false;

		for (Item item : inventory.getItems())
		{
			if (item != null && RUNE_POUCH_IDS.contains(item.getId()))
				return true;
		}
		return false;
	}
}
