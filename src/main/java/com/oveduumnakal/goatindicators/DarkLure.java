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
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Decides whether the player can currently cast Dark Lure, the Arceuus alternative to Telekinetic Grab for
 * luring goats into the pit: Magic 50, the Arceuus spellbook, 1 death rune plus 1 nature rune, and A Kingdom
 * Divided complete (which unlocks the spell). Runes are counted through {@link SpellRunes} from both the
 * inventory and the rune pouch.
 *
 * <p>As with {@link TelekineticGrab}, the requirement combination is factored into
 * {@link #meetsRequirements} so it can be unit-tested without a {@code Client}; only the live level,
 * spellbook, rune, and quest reads need the client.
 */
@Singleton
class DarkLure
{
	/** Magic level required to cast Dark Lure. */
	static final int MAGIC_LEVEL = 50;

	/** {@link VarbitID#SPELLBOOK} value for the Arceuus spellbook, the only book with this spell. */
	static final int ARCEUUS_SPELLBOOK = 3;

	@Inject
	private Client client;

	@Inject
	private SpellRunes runes;

	/** Whether every cast requirement is met right now. */
	boolean canCast()
	{
		return meetsRequirements(
			client.getBoostedSkillLevel(Skill.MAGIC),
			client.getVarbitValue(VarbitID.SPELLBOOK),
			runes.has(ItemID.DEATHRUNE),
			runes.has(ItemID.NATURERUNE),
			questComplete());
	}

	/**
	 * Pure cast-requirement rule: enough Magic, the Arceuus spellbook, the quest that unlocks the spell,
	 * and a death rune plus a nature rune.
	 *
	 * @param magicLevel    the player's current (boosted) Magic level
	 * @param spellbook     the {@link VarbitID#SPELLBOOK} value
	 * @param hasDeathRune  whether a death rune is available
	 * @param hasNatureRune whether a nature rune is available
	 * @param questComplete whether A Kingdom Divided is complete, unlocking the spell
	 * @return true when the spell can be cast
	 */
	static boolean meetsRequirements(int magicLevel, int spellbook, boolean hasDeathRune, boolean hasNatureRune,
		boolean questComplete)
	{
		if (magicLevel < MAGIC_LEVEL || spellbook != ARCEUUS_SPELLBOOK || !questComplete)
			return false;

		return hasDeathRune && hasNatureRune;
	}

	/** Whether A Kingdom Divided is complete, the quest that unlocks Dark Lure. */
	private boolean questComplete()
	{
		return Quest.A_KINGDOM_DIVIDED.getState(client) == QuestState.FINISHED;
	}
}
