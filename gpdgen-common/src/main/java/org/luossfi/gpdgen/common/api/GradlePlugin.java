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

package org.luossfi.gpdgen.common.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * An annotation for Gradle plugin classes. It defines the plugin's identifier and generates the descriptor file for this plugin into
 * META-INF/gradle-plugins.
 *
 * <p>The annotated class must fulfill these requirements:
 *
 * <ul>
 * <li>non-abstract public class</li>
 * <li>implements org.gradle.api.Plugin</li>
 * </ul>
 *
 * @author Steff Lukas
 */
@Documented
@Repeatable( GradlePlugins.class )
@Retention( SOURCE )
@Target( TYPE )
public @interface GradlePlugin
{
  /**
   * The Gradle plugin id, must not be empty.
   *
   * @return the Gradle plugin's id
   */
  String value();
}
