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
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * The container annotation for the repeatable {@link GradlePlugin @GradlePlugin} annotation.
 *
 * @author Steff Lukas
 */
@Documented
@Retention( SOURCE )
@Target( TYPE )
public @interface GradlePlugins
{
  /**
   * The {@link GradlePlugin @GradlePlugin} annotations contained in the annotation.
   *
   * @return The wrapped annotations
   */
  GradlePlugin[] value();
}
