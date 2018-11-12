/*
 * This file is part of gpdgen-groovy - The Gradle Plugin Descriptor Generator for Groovy
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

package org.luossfi.gpdgen.groovy.transform


import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Title( 'a Gradle plugin AST transformation' )
class GradlePluginAstTransformationTest extends Specification
{
  @Shared
  File tmpDir

  def setupSpec()
  {
    tmpDir = File.createTempDir()
  }

  def cleanupSpec()
  {
    tmpDir?.deleteDir()
  }


  def 'is instantiated with new GradlePluginAstTransformation()'()
  {
    when:
    def transformation = new GradlePluginAstTransformation()

    then:
    noExceptionThrown()

    and:
    transformation != null
  }

  def 'generates plugin descriptor for public non-abstract Gradle plugin implementation with single annotation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( 'org.luossfi.test' )
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    def pluginDescriptor = new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' )
    pluginDescriptor.isFile()

    and:
    def descriptorContent = getDescriptorContentWithoutComments( pluginDescriptor )
    descriptorContent == [ 'implementation-class = org.luossfi.gradle.test.TestPlugin' ]
  }

  def 'generates plugin descriptors for public non-abstract Gradle plugin implementation with multiple annotations'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
    package org.luossfi.gradle.test

    import org.gradle.api.Plugin
    import org.gradle.api.Project
    import org.luossfi.gpdgen.common.api.GradlePlugin
    
    @GradlePlugin( 'org.luossfi.test.first' )
    @GradlePlugin( 'org.luossfi.test.second' )
    @GradlePlugin( 'org.luossfi.test.third' )
    class TestPlugin implements Plugin<Project> {
      void apply( final Project project ) {}
    }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    def pluginDescriptorDir = new File( outDir, 'META-INF/gradle-plugins' )
    pluginDescriptorDir.isDirectory()

    and:
    pluginDescriptorDir.list().length == 3

    and:
    def firstPluginDescriptor = new File( pluginDescriptorDir, 'org.luossfi.test.first.properties' )
    firstPluginDescriptor.isFile()

    and:
    def firstContent = getDescriptorContentWithoutComments( firstPluginDescriptor )
    firstContent == [ 'implementation-class = org.luossfi.gradle.test.TestPlugin' ]

    and:
    def secondPluginDescriptor = new File( pluginDescriptorDir, 'org.luossfi.test.second.properties' )
    secondPluginDescriptor.isFile()

    and:
    def secondContent = getDescriptorContentWithoutComments( secondPluginDescriptor )
    secondContent == [ 'implementation-class = org.luossfi.gradle.test.TestPlugin' ]

    and:
    def thirdPluginDescriptor = new File( pluginDescriptorDir, 'org.luossfi.test.third.properties' )
    thirdPluginDescriptor.isFile()

    and:
    def thirdContent = getDescriptorContentWithoutComments( thirdPluginDescriptor )
    thirdContent == [ 'implementation-class = org.luossfi.gradle.test.TestPlugin' ]
  }

  def 'generates plugin descriptors multiple public non-abstract Gradle plugin implementation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
    package org.luossfi.gradle.test

    import org.gradle.api.Plugin
    import org.gradle.api.Project
    import org.luossfi.gpdgen.common.api.GradlePlugin
    
    @GradlePlugin( 'org.luossfi.test' )
    class TestPlugin implements Plugin<Project> {
      void apply( final Project project ) {}
    }
    
    @GradlePlugin( 'org.luossfi.other' )
    class OtherPlugin implements Plugin<Project> {
      void apply( final Project project ) {}
    }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    def pluginDescriptorDir = new File( outDir, 'META-INF/gradle-plugins' )
    pluginDescriptorDir.isDirectory()

    and:
    pluginDescriptorDir.list().length == 2

    and:
    def testPluginDescriptor = new File( pluginDescriptorDir, 'org.luossfi.test.properties' )
    testPluginDescriptor.isFile()

    and:
    def testContent = getDescriptorContentWithoutComments( testPluginDescriptor )
    testContent == [ 'implementation-class = org.luossfi.gradle.test.TestPlugin' ]

    and:
    def otherPluginDescriptor = new File( pluginDescriptorDir, 'org.luossfi.other.properties' )
    otherPluginDescriptor.isFile()

    and:
    def otherContent = getDescriptorContentWithoutComments( otherPluginDescriptor )
    otherContent == [ 'implementation-class = org.luossfi.gradle.test.OtherPlugin' ]
  }

  def 'removes leading/trailing whitespace from plugin id'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( ' \\torg.luossfi.test \\r\\n' )
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    def pluginDescriptor = new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' )
    pluginDescriptor.exists()
  }


  def 'fails for invalid plugin id'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( 'org..luossfi.test' )
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for protected non-abstract Gradle plugin implementation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( 'org.luossfi.test' )
      protected class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for package scoped non-abstract Gradle plugin implementation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import groovy.transform.PackageScope
      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @PackageScope
      @GradlePlugin( 'org.luossfi.test' )
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 9

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for public abstract Gradle plugin implementation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( 'org.luossfi.test' )
      abstract class TestPlugin implements Plugin<Project> {}'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for interface extending Gradle plugin'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( 'org.luossfi.test' )
      interface TestPlugin extends Plugin<Project> {}'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for enum Gradle plugin implementation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( 'org.luossfi.test' )
      enum TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for public non-abstract class not implementing Gradle plugin'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( 'org.luossfi.test' )
      class TestPlugin {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 6

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for missing value in @GradlePlugin annotation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for empty value in @GradlePlugin annotation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( '' )
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'fails for whitespace only value in @GradlePlugin annotation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      @GradlePlugin( ' \\t\\r\\n' )
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    final def exception = thrown( MultipleCompilationErrorsException )
    final def syntaxErrors = exception.errorCollector.errors.findAll { it instanceof SyntaxErrorMessage }
    syntaxErrors.size() == 1

    and:
    final def syntaxError = syntaxErrors.first() as SyntaxErrorMessage
    syntaxError.cause.startLine == 7

    and:
    syntaxError.cause.startColumn == 1

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'is a no-op for non-annotated non-abstract public Gradle plugin implementation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      
      class TestPlugin implements Plugin<Project> {
        void apply( final Project project ) {}
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    noExceptionThrown()

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  def 'is a no-op for annotated non-abstract public inner Gradle plugin implementation'()
  {
    given:
    def outDir = new File( tmpDir, UUID.randomUUID().toString() )
    outDir.mkdir()

    and:
    def classloader = createClassLoader( outDir )

    and:
    def pluginImpl = '''\
      package org.luossfi.gradle.test

      import org.gradle.api.Plugin
      import org.gradle.api.Project
      import org.luossfi.gpdgen.common.api.GradlePlugin
      
      class Foo {
        @GradlePlugin( 'org.luossfi.test' )
        static class TestPlugin implements Plugin<Project> {
          void apply( final Project project ) {}
        }
      }'''.stripIndent()

    when:
    classloader.parseClass( pluginImpl )

    then:
    noExceptionThrown()

    and:
    !new File( outDir, 'META-INF/gradle-plugins/org.luossfi.test.properties' ).exists()
  }

  @SuppressWarnings( "GrMethodMayBeStatic" )
  private GroovyClassLoader createClassLoader( final File outDir )
  {
    final def compilerConfiguration = new CompilerConfiguration()
    compilerConfiguration.targetBytecode = CompilerConfiguration.JDK8
    compilerConfiguration.targetDirectory = outDir

    new GroovyClassLoader( Thread.currentThread().contextClassLoader, compilerConfiguration )
  }

  private static List<String> getDescriptorContentWithoutComments( final File pluginDescriptor )
  {
    pluginDescriptor.readLines( 'UTF-8' ).findAll { !it.startsWith( '#' ) }
  }
}
