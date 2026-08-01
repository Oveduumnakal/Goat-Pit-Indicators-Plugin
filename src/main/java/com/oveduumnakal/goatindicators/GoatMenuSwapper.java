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

import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;

/**
 * Reorders the goat-area menu so a stray click never wastes a lure and movement stays free while prodding.
 *
 * <p>Two independent swaps, each toggleable in config. When the player points a luring spell — Telekinetic
 * Grab or Dark Lure — at a goat but the pit is effectively full (goats in it plus goats in transit have
 * reached its capacity), {@code "Cancel"} is moved to the top of the menu so the cast cannot fire by
 * accident. Separately, and under the same effectively-full condition, while a Cattleprod is equipped
 * {@code "Walk here"} is moved to the top so a click near a goat walks instead of interacting. Both swaps
 * only apply once a pit is full, so an in-progress catch is left alone. Run every frame from
 * {@link GoatIndicatorsPlugin} on {@code PostMenuSort}, so it fixes both the left-click default and the
 * right-click ordering.
 */
@Singleton
class GoatMenuSwapper
{
	private final Client client;
	private final GoatIndicatorsConfig config;
	private final GoatPitTracker tracker;
	private final GoatTransitTracker transitTracker;
	private final LureSpells lureSpells;

	@Inject
	GoatMenuSwapper(Client client, GoatIndicatorsConfig config, GoatPitTracker tracker,
		GoatTransitTracker transitTracker, LureSpells lureSpells)
	{
		this.client = client;
		this.config = config;
		this.tracker = tracker;
		this.transitTracker = transitTracker;
		this.lureSpells = lureSpells;
	}

	/**
	 * Applies the configured swap to the current menu, if one applies. The Cancel-when-full swap wins over
	 * the Walk-here-with-prod swap when both would fire, since guarding against a wasted cast matters more.
	 */
	void onPostMenuSort()
	{
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries.length < 2)
			return;

		MenuEntry promote = null;
		if (config.swapWalkWhenProd() && cattleprodEquipped() && goatEntryPresent(entries) && noCatchingPitHasRoom())
			promote = firstOfType(entries, MenuAction.WALK);

		if (config.swapCancelWhenFull() && castOnGoatPresent(entries) && noCatchingPitHasRoom())
		{
			MenuEntry cancel = firstOfType(entries, MenuAction.CANCEL);
			if (cancel != null)
				promote = cancel;
		}

		if (promote != null)
			menu.setMenuEntries(promoteToTop(entries, promote));
	}

	/** The first entry of the given action in menu order, or {@code null} if the menu has none. */
	private static MenuEntry firstOfType(MenuEntry[] entries, MenuAction type)
	{
		for (MenuEntry entry : entries)
		{
			if (entry.getType() == type)
				return entry;
		}

		return null;
	}

	/**
	 * Whether the menu contains a spell-on-goat cast entry and the player can actually lure. A selected
	 * spell used on an NPC is a {@code WIDGET_TARGET_ON_NPC} entry; gating on {@link LureSpells#canLure()}
	 * keeps the swap to the case that matters — a lure aimed at a goat.
	 */
	private boolean castOnGoatPresent(MenuEntry[] entries)
	{
		if (!lureSpells.canLure())
			return false;

		for (MenuEntry entry : entries)
		{
			if (entry.getType() == MenuAction.WIDGET_TARGET_ON_NPC
					&& GoatPitTracker.matchesGoatName(entry.getTarget()))
				return true;
		}

		return false;
	}

	/**
	 * Whether the menu contains an entry for a goat NPC, so the Walk-here swap only fires while a goat is
	 * under the cursor rather than on any tile or object. Reads the entry's NPC directly, which keeps the
	 * pit (a game object) from counting even though its name also contains "goat".
	 */
	private static boolean goatEntryPresent(MenuEntry[] entries)
	{
		for (MenuEntry entry : entries)
		{
			NPC npc = entry.getNpc();
			if (npc != null && GoatPitTracker.matchesGoatName(npc.getName()))
				return true;
		}

		return false;
	}

	/**
	 * Whether no spiked pit can still take a goat once in-transit goats are counted, so a fresh cast would
	 * be wasted. Returns false when no spiked pit is loaded, since without one there is nothing to judge the
	 * cast against.
	 */
	private boolean noCatchingPitHasRoom()
	{
		int inTransit = transitTracker.inTransitCount();
		boolean sawCatchingPit = false;
		for (GameObject pit : tracker.getPits())
		{
			GoatPitState state = tracker.stateOf(pit);
			if (state.needsSpikes())
				continue;

			sawCatchingPit = true;
			if (!TelegrabTargeting.effectivelyFull(state.getCount(), inTransit, state.getCapacity()))
				return false;
		}

		return sawCatchingPit;
	}

	/** Whether a Cattleprod is in the worn-equipment container, so movement should take menu priority. */
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
	 * Returns a copy of the menu with {@code promote} moved to the last slot — the top of the visible menu
	 * and the left-click default — leaving every other entry in its existing order.
	 */
	static MenuEntry[] promoteToTop(MenuEntry[] entries, MenuEntry promote)
	{
		MenuEntry[] reordered = new MenuEntry[entries.length];
		int index = 0;
		for (MenuEntry entry : entries)
		{
			if (entry != promote)
				reordered[index++] = entry;
		}

		reordered[index] = promote;
		return reordered;
	}
}
