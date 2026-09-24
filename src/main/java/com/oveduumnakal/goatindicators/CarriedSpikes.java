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
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * Reads how many goat-pit spikes the player is carrying, shared by the supply highlight, the spike-count label
 * and the restock warning (#103). Only the inventory is read; the count is taken live on each call, which is
 * cheap and avoids a cache going stale between the inventory event and the next frame.
 */
@Singleton
class CarriedSpikes
{
	private final Client client;

	@Inject
	CarriedSpikes(Client client)
	{
		this.client = client;
	}

	/** @return the spikes in the inventory right now, or {@code 0} when it is unavailable. */
	int count()
	{
		return countIn(client.getItemContainer(InventoryID.INV));
	}

	/**
	 * Sums every spikes stack in a container.
	 *
	 * @param container the container to read, or {@code null}
	 * @return the spikes held, or {@code 0} for a missing container
	 */
	static int countIn(ItemContainer container)
	{
		if (container == null)
			return 0;

		int count = 0;
		for (Item item : container.getItems())
		{
			if (item != null && item.getId() == GoatIds.SPIKES_ITEM_ID)
				count += Math.max(0, item.getQuantity());
		}

		return count;
	}

	/**
	 * Whether the player should restock spikes before emptying: the pit is full, so the next action empties it
	 * and uses up its spikes, and none are carried to re-line it with.
	 *
	 * @param state the pit's state
	 * @param carried spikes carried
	 * @return true when emptying now would leave the pit unable to catch
	 */
	static boolean needsRestock(GoatPitState state, int carried)
	{
		return state.isFull() && carried <= 0;
	}
}
