/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.util.Collections;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the menu reordering: the pure {@link GoatMenuSwapper#promoteToTop} move, and the
 * {@code onPostMenuSort} decision that a Cancel swap outranks a Walk swap, fires only once no catching pit
 * has room, and leaves the menu untouched otherwise. RuneLite menu, item and pit types are mocked.
 */
public class GoatMenuSwapperTest
{
	private final Client client = mock(Client.class);
	private final GoatIndicatorsConfig config = mock(GoatIndicatorsConfig.class);
	private final GoatPitTracker tracker = mock(GoatPitTracker.class);
	private final GoatTransitTracker transitTracker = mock(GoatTransitTracker.class);
	private final LureSpells lureSpells = mock(LureSpells.class);

	private final GoatMenuSwapper swapper =
		new GoatMenuSwapper(client, config, tracker, transitTracker, lureSpells);

	@Test
	public void promoteToTopMovesTheEntryToTheLastSlotPreservingOrder()
	{
		MenuEntry a = mock(MenuEntry.class);
		MenuEntry b = mock(MenuEntry.class);
		MenuEntry c = mock(MenuEntry.class);

		assertArrayEquals(
			new MenuEntry[]{a, c, b},
			GoatMenuSwapper.promoteToTop(new MenuEntry[]{a, b, c}, b));
	}

	@Test
	public void cancelSwapWinsOverWalkSwapWhenBothApply()
	{
		enableBothSwaps();
		when(lureSpells.canLure()).thenReturn(true);
		equipCattleprod();
		onlyPitIsFull();

		MenuEntry walk = entryOfType(MenuAction.WALK);
		MenuEntry cast = castOnGoatEntry();
		MenuEntry cancel = entryOfType(MenuAction.CANCEL);
		MenuEntry goat = goatNpcEntry();
		Menu menu = menuOf(walk, cast, cancel, goat);

		swapper.onPostMenuSort();

		assertSame(cancel, promotedTopOf(menu));
	}

	@Test
	public void noSwapHappensWhileACatchingPitStillHasRoom()
	{
		enableBothSwaps();
		when(lureSpells.canLure()).thenReturn(true);
		equipCattleprod();
		onlyPitHasRoom();

		Menu menu = menuOf(entryOfType(MenuAction.WALK), castOnGoatEntry(),
			entryOfType(MenuAction.CANCEL), goatNpcEntry());

		swapper.onPostMenuSort();

		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void walkHereIsPromotedWithAProdWhenThePitIsFull()
	{
		when(config.swapCancelWhenFull()).thenReturn(false);
		when(config.swapWalkWhenProd()).thenReturn(true);
		equipCattleprod();
		onlyPitIsFull();

		MenuEntry walk = entryOfType(MenuAction.WALK);
		MenuEntry goat = goatNpcEntry();
		Menu menu = menuOf(walk, goat);

		swapper.onPostMenuSort();

		assertSame(walk, promotedTopOf(menu));
	}

	@Test
	public void clearIsDemotedOffTheTopWhileAPitIsStillCatching()
	{
		when(config.swapClearWhenNotFull()).thenReturn(true);
		onlyPitHasRoom();

		MenuEntry examine = entryOfType(MenuAction.EXAMINE_OBJECT);
		MenuEntry clear = clearOnPitEntry();
		Menu menu = menuOf(examine, clear);

		swapper.onPostMenuSort();

		ArgumentCaptor<MenuEntry[]> captor = ArgumentCaptor.forClass(MenuEntry[].class);
		verify(menu).setMenuEntries(captor.capture());
		MenuEntry[] reordered = captor.getValue();
		assertSame(clear, reordered[0]);
		assertSame(examine, reordered[reordered.length - 1]);
	}

	@Test
	public void clearIsLeftOnTopWhenThePitIsFull()
	{
		when(config.swapClearWhenNotFull()).thenReturn(true);
		onlyPitIsFull();

		Menu menu = menuOf(entryOfType(MenuAction.EXAMINE_OBJECT), clearOnPitEntry());

		swapper.onPostMenuSort();

		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void clearIsLeftOnTopWhileTheUnspikedPitIsBeingClearedOut()
	{
		when(config.swapClearWhenNotFull()).thenReturn(true);
		onlyPitNeedsSpikesAndIsNotFull();

		Menu menu = menuOf(entryOfType(MenuAction.EXAMINE_OBJECT), clearOnPitEntry());

		swapper.onPostMenuSort();

		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void clearIsLeftAloneWhenTheToggleIsOff()
	{
		when(config.swapClearWhenNotFull()).thenReturn(false);
		onlyPitHasRoom();

		Menu menu = menuOf(entryOfType(MenuAction.EXAMINE_OBJECT), clearOnPitEntry());

		swapper.onPostMenuSort();

		verify(menu, never()).setMenuEntries(any());
	}

	private void enableBothSwaps()
	{
		when(config.swapCancelWhenFull()).thenReturn(true);
		when(config.swapWalkWhenProd()).thenReturn(true);
	}

	/** Points the tracker at a single spiked, at-capacity pit, so no pit can take another goat. */
	private void onlyPitIsFull()
	{
		GameObject pit = mock(GameObject.class);
		when(tracker.getPits()).thenReturn(Collections.singletonList(pit));
		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(16, true, 16));
		when(transitTracker.inTransitCount()).thenReturn(0);
	}

	/** Points the tracker at a single spiked, part-full pit, so a fresh goat still fits. */
	private void onlyPitHasRoom()
	{
		GameObject pit = mock(GameObject.class);
		when(tracker.getPits()).thenReturn(Collections.singletonList(pit));
		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(5, true, 16));
		when(transitTracker.inTransitCount()).thenReturn(0);
	}

	/**
	 * Points the tracker at a single unspiked, part-full pit: the clear-out phase, where the pit must be
	 * emptied over several partial clears before it can be re-lined, so "Clear" should stay handy.
	 */
	private void onlyPitNeedsSpikesAndIsNotFull()
	{
		GameObject pit = mock(GameObject.class);
		when(tracker.getPits()).thenReturn(Collections.singletonList(pit));
		when(tracker.stateOf(pit)).thenReturn(new GoatPitState(5, false, 16));
		when(transitTracker.inTransitCount()).thenReturn(0);
	}

	private void equipCattleprod()
	{
		Item cattleprod = mock(Item.class);
		when(cattleprod.getId()).thenReturn(ItemID.CATTLEPROD);
		ItemContainer worn = mock(ItemContainer.class);
		when(worn.getItems()).thenReturn(new Item[]{cattleprod});
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
	}

	private Menu menuOf(MenuEntry... entries)
	{
		Menu menu = mock(Menu.class);
		when(menu.getMenuEntries()).thenReturn(entries);
		when(client.getMenu()).thenReturn(menu);
		return menu;
	}

	/** The entry left at the top slot by the swap: the last element handed to {@code setMenuEntries}. */
	private static MenuEntry promotedTopOf(Menu menu)
	{
		ArgumentCaptor<MenuEntry[]> captor = ArgumentCaptor.forClass(MenuEntry[].class);
		verify(menu).setMenuEntries(captor.capture());
		MenuEntry[] reordered = captor.getValue();
		return reordered[reordered.length - 1];
	}

	private static MenuEntry entryOfType(MenuAction type)
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getType()).thenReturn(type);
		return entry;
	}

	/** A "cast luring spell on goat" entry: a spell selected onto a goat NPC. */
	private static MenuEntry castOnGoatEntry()
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getType()).thenReturn(MenuAction.WIDGET_TARGET_ON_NPC);
		when(entry.getTarget()).thenReturn("Goat");
		return entry;
	}

	/** A "Clear Goat Pit" option standing on the pit object. */
	private static MenuEntry clearOnPitEntry()
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getOption()).thenReturn("Clear");
		when(entry.getTarget()).thenReturn("Goat pit");
		return entry;
	}

	/** An entry standing over a goat NPC, so the Walk-here swap sees a goat under the cursor. */
	private static MenuEntry goatNpcEntry()
	{
		NPC goat = mock(NPC.class);
		when(goat.getName()).thenReturn("Goat");
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getNpc()).thenReturn(goat);
		return entry;
	}
}
