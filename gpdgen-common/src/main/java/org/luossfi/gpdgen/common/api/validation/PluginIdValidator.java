/*
 * This file is part of gpdgen-common - The Gradle Plugin Descriptor Generator Common Elements
 *
 * Copyright (C) 2018++ Steff Lukas <steff.lukas@luossfi.org>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.luossfi.gpdgen.common.api.validation;


import java.util.regex.Pattern;

/**
 * Validator for Gradle plugin IDs.
 *
 * <p>A plugin id considered to be valid meets these criteria:
 *
 * <ul>
 * <li>contains only <b>lower-case</b> alphanumeric characters as well as '.' and '-'</li>
 * <li>contains at least one '.' character</li>
 * <li>does not start or end with a '.' character</li>
 * <li>does not contain any consecutive '.' characters (i.e. '..')</li>
 * </ul>
 *
 * @author Steff Lukas
 */
public final class PluginIdValidator
{
  private static final Pattern PLUGIN_ID_PATTERN = Pattern.compile( "^[-0-9a-z]++\\.[-0-9a-z]++(?:\\.[-0-9a-z]++)*+$" );

  //hidden, this is a pure static class
  private PluginIdValidator() {}

  /**
   * Validates if the in put pluginId is valid.
   *
   * @param pluginId the plugin id to check
   *
   * @return true if pluginId is a valid plugin id, false otherwise
   */
  public static boolean isValid( final String pluginId )
  {
    return pluginId != null && PLUGIN_ID_PATTERN.matcher( pluginId ).matches();
  }
}
