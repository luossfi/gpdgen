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

package org.luossfi.gpdgen.common.api.validation


import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Title( 'a plugin id validator' )
class PluginIdValidatorTest extends Specification
{
  @Unroll
  def 'returns true for "#pluginId"'()
  {
    expect:
    PluginIdValidator.isValid( pluginId )

    where:
    pluginId << [ 'luossfi.foo',
                  'org.luossfi.foo',
                  'org.luossfi.foo.bar',
                  'org.luossfi.foo.bar.baz',
                  'org.luossfi.foo-bar.baz',
                  'org.luossfi.42',
                  'org.luossfi.42-foo.bar' ]
  }

  @Unroll
  def 'returns false for "#pluginId"'()
  {
    expect:
    !PluginIdValidator.isValid( pluginId )

    where:
    pluginId << [ '.org.luossfi.foo',
                  'org.luossfi.foo.',
                  '.org.luossfi.foo.',
                  'luossfi',
                  'org..luossfi.foo',
                  '',
                  'org luossfi.foo',
                  'Org.Luossfi',
                  'ORG.LUOSSFI',
                  'org.luossfi:foo$3' ]
  }
}

