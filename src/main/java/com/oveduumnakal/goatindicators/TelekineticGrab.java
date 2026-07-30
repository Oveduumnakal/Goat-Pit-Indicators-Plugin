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
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Decides whether the player can currently cast Telekinetic Grab: Magic level, standard spellbook, and
 * the runes to pay for it (1 law rune plus 1 air rune, with any air-providing staff standing in for the
 * air rune). Runes are counted from both the inventory and the rune pouch; the air staff is read from
 * worn equipment.
 *
 * <p>The requirement combination is factored into {@link #meetsRequirements} so it can be unit-tested
 * without a {@code Client}; only the live inventory/varbit reads need the client.
 */
@Singleton
class TelekineticGrab
{
	/** Magic level required to cast Telekinetic Grab. */
	static final int MAGIC_LEVEL = 33;

	/** {@link VarbitID#SPELLBOOK} value for the standard spellbook, the only book with this spell. */
	static final int STANDARD_SPELLBOOK = 0;

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

	/** Weapons whose air element covers the air rune, letting the cast run on a law rune alone. */
	private static final Set<Integer> AIR_STAVES = Set.of(
		ItemID.STAFF_OF_AIR,
		ItemID.AIR_BATTLESTAFF,
		ItemID.MYSTIC_AIR_STAFF,
		ItemID.SMOKE_BATTLESTAFF,
		ItemID.MYSTIC_SMOKE_BATTLESTAFF,
		ItemID.MIST_BATTLESTAFF,
		ItemID.MYSTIC_MIST_BATTLESTAFF,
		ItemID.DUST_BATTLESTAFF,
		ItemID.MYSTIC_DUST_BATTLESTAFF);

	@Inject
	private Client client;

	/** Whether every cast requirement is met right now. */
	boolean canCast()
	{
		return meetsRequirements(
			client.getBoostedSkillLevel(Skill.MAGIC),
			client.getVarbitValue(VarbitID.SPELLBOOK),
			hasRune(ItemID.LAWRUNE),
			hasRune(ItemID.AIRRUNE),
			airStaffEquipped());
	}

	/**
	 * Pure cast-requirement rule: enough Magic, standard spellbook, a law rune, and an air source (an
	 * air rune or an air staff).
	 *
	 * @param magicLevel the player's current (boosted) Magic level
	 * @param spellbook the {@link VarbitID#SPELLBOOK} value
	 * @param hasLawRune whether a law rune is available
	 * @param hasAirRune whether an air rune is available
	 * @param airStaff whether an air-providing staff is worn
	 * @return true when the spell can be cast
	 */
	static boolean meetsRequirements(int magicLevel, int spellbook, boolean hasLawRune, boolean hasAirRune,
		boolean airStaff)
	{
		if (magicLevel < MAGIC_LEVEL || spellbook != STANDARD_SPELLBOOK)
		{
			return false;
		}
		return hasLawRune && (hasAirRune || airStaff);
	}

	/** Whether the given rune id is held in the inventory or the rune pouch. */
	private boolean hasRune(int runeId)
	{
		return inventoryHas(runeId) || pouchHas(runeId);
	}

	private boolean inventoryHas(int runeId)
	{
		return containerHas(client.getItemContainer(InventoryID.INV), runeId);
	}

	private boolean airStaffEquipped()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return false;
		}
		for (Item item : worn.getItems())
		{
			if (item != null && AIR_STAVES.contains(item.getId()))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean containerHas(ItemContainer container, int itemId)
	{
		if (container == null)
		{
			return false;
		}
		for (Item item : container.getItems())
		{
			if (item != null && item.getId() == itemId && item.getQuantity() > 0)
			{
				return true;
			}
		}
		return false;
	}

	private boolean pouchHas(int runeId)
	{
		EnumComposition runeMap = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runeMap == null)
		{
			return false;
		}
		for (int slot = 0; slot < POUCH_TYPE_VARBITS.length; slot++)
		{
			int type = client.getVarbitValue(POUCH_TYPE_VARBITS[slot]);
			if (type <= 0 || client.getVarbitValue(POUCH_QUANTITY_VARBITS[slot]) <= 0)
			{
				continue;
			}
			if (runeMap.getIntValue(type) == runeId)
			{
				return true;
			}
		}
		return false;
	}
}
