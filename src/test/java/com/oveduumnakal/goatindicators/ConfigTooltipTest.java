/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.goatindicators;

import java.lang.reflect.Method;

import org.junit.Test;

import net.runelite.client.config.ConfigItem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Keeps setting tooltips readable (#153): RuneLite renders each description as HTML, so every sentence after the
 * first starts on its own line via {@code <br>} rather than running on after a period.
 */
public class ConfigTooltipTest
{
	@Test
	public void everySentenceBreakStartsANewLine()
	{
		int checked = 0;
		for (Method method : GoatIndicatorsConfig.class.getMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item == null)
				continue;

			String description = item.description()
				.replace("e.g. ", "")
				.replace("i.e. ", "");
			assertFalse(item.keyName() + " runs sentences together: " + description,
				description.matches("(?s).*[.?!] .*"));
			assertFalse(item.keyName() + " ends with a dangling line break", description.endsWith("<br>"));
			checked++;
		}

		assertTrue(checked > 0);
	}
}
