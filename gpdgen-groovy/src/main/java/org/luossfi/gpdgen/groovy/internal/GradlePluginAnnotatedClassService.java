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

package org.luossfi.gpdgen.groovy.internal;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.luossfi.gpdgen.common.api.GradlePlugin;
import org.luossfi.gpdgen.common.api.validation.PluginIdValidator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

/**
 * This is the service which does the work for the {@link org.luossfi.gpdgen.groovy.transform.GradlePluginAstTransformation}.
 *
 * @author Steff Lukas
 */
public final class GradlePluginAnnotatedClassService
{
  private static final ClassNode GRADLE_PLUGIN_ANNOTATION = ClassHelper.make( GradlePlugin.class );
  private static final ClassNode PLUGIN_INTERFACE = ClassHelper.make( "org.gradle.api.Plugin" );

  private final SourceUnit source;
  private final ClassNode pluginImplementation;
  private final String generatorName;

  /**
   * Instantiates a new {@link GradlePluginAnnotatedClassService}.
   *
   * @param pluginImplementation the Gradle plugin implementation this services shall handle, must not be null
   * @param source the source of the handled plugin implementation, must not be null
   * @param generatorName the name of the generator, this will be added as a comment in the resulting descriptor file, must not be null
   */
  public GradlePluginAnnotatedClassService( final ClassNode pluginImplementation, final SourceUnit source, final String generatorName )
  {
    this.pluginImplementation = pluginImplementation;
    this.source = source;
    this.generatorName = generatorName;
  }

  /**
   * Checks if the handled Gradle plugin implementation is valid and if so, generates the plugin descriptor for it.
   *
   * <p>All problems are recorded using {@link SourceUnit#getErrorCollector()}.
   */
  public void generateGradlePluginDescriptors()
  {
    if ( isValidPluginImplementation() )
    {
      pluginImplementation.getAnnotations( GRADLE_PLUGIN_ANNOTATION ).forEach( this::handleGradlePluginAnnotation );
    }
  }

  private boolean isValidPluginImplementation()
  {
    boolean isValid = true;

    if ( isNotPublic( pluginImplementation ) )
    {
      final String message = format( "Illegal annotation location: Only public implementations may be annotated with @%s",
                                     GRADLE_PLUGIN_ANNOTATION.getName() );
      addSyntaxErrorMessage( message, findFirstGradlePluginAnnotation() );

      isValid = false;
    }

    if ( isAbstract( pluginImplementation ) || pluginImplementation.isInterface() || pluginImplementation.isEnum() )
    {
      final String message = format( "Illegal annotation location: Only non-abstract classes may be annotated with @%s!",
                                     GRADLE_PLUGIN_ANNOTATION.getName() );
      addSyntaxErrorMessage( message, findFirstGradlePluginAnnotation() );

      isValid = false;
    }

    if ( !pluginImplementation.implementsInterface( PLUGIN_INTERFACE ) )
    {
      final String message = format( "Illegal annotation location: Only subtypes of %s may be annotated with @%s!", PLUGIN_INTERFACE.getName(),
                                     GRADLE_PLUGIN_ANNOTATION.getName() );
      addSyntaxErrorMessage( message, findFirstGradlePluginAnnotation() );

      isValid = false;
    }

    return isValid;
  }

  private AnnotationNode findFirstGradlePluginAnnotation()
  {
    return pluginImplementation.getAnnotations( GRADLE_PLUGIN_ANNOTATION ).get( 0 );
  }

  private static boolean isAbstract( final ClassNode classNode )
  {
    return Modifier.isAbstract( classNode.getModifiers() );
  }

  private static boolean isNotPublic( final ClassNode classNode )
  {
    return !Modifier.isPublic( classNode.getModifiers() );
  }

  private void addSyntaxErrorMessage( final String message, final ASTNode node )
  {
    final SyntaxException error = new SyntaxException( message, node.getLineNumber(), node.getColumnNumber() );

    source.getErrorCollector().addErrorAndContinue( new SyntaxErrorMessage( error, source ) );
  }

  private void handleGradlePluginAnnotation( final AnnotationNode gradlePluginAnnotation )
  {
    findPluginId( gradlePluginAnnotation ).filter( pluginId -> isValidPluginId( pluginId, gradlePluginAnnotation ) )
                                          .ifPresent( this::writePluginDescriptor );
  }

  private Optional<String> findPluginId( final AnnotationNode gradlePluginAnnotation )
  {
    final Optional<String> pluginId;
    final Expression valueMember = gradlePluginAnnotation.getMember( "value" );
    if ( valueMember != null )
    {
      pluginId = Optional.of( valueMember.getText().trim() );
    }
    else
    {
      pluginId = Optional.empty();

      final String message = format( "annotation @%s is missing a default value for the element 'value'",
                                     GRADLE_PLUGIN_ANNOTATION.getNameWithoutPackage() );
      addSyntaxErrorMessage( message, gradlePluginAnnotation );
    }

    return pluginId;
  }

  private boolean isValidPluginId( final String pluginId, final AnnotationNode sourceAnnotation )
  {
    final boolean isValid = PluginIdValidator.isValid( pluginId );
    if ( !isValid )
    {
      final String message = format( "the plugin id '%s' is not a valid Gradle plugin id", pluginId );
      addSyntaxErrorMessage( message, sourceAnnotation );
    }

    return isValid;
  }

  private void writePluginDescriptor( final String pluginId )
  {
    final File targetDir = source.getConfiguration().getTargetDirectory();

    final File pluginDescriptorsDir = new File( targetDir, "META-INF/gradle-plugins" );
    //noinspection ResultOfMethodCallIgnored
    pluginDescriptorsDir.mkdirs();

    final String pluginDescriptorFileName = pluginId + ".properties";
    final File pluginDescriptor = new File( pluginDescriptorsDir, pluginDescriptorFileName );

    try ( PrintWriter writer = createPrintWriter( pluginDescriptor ) )
    {
      writer.print( "# Generated by " );
      writer.print( getPrettyGeneratorName() );
      writer.println();

      writer.print( "# Generated on " );
      writer.print( getIsoDateTime() );
      writer.println();

      writer.print( "implementation-class = " );
      writer.print( pluginImplementation.getName() );
      writer.println();
    }
    catch ( final IOException exception )
    {
      reportException( exception );
    }
  }

  private PrintWriter createPrintWriter( final File pluginDescriptor ) throws IOException
  {
    final String charset = source.getConfiguration().getSourceEncoding();

    return new PrintWriter( new BufferedWriter( new OutputStreamWriter( new FileOutputStream( pluginDescriptor ), charset ) ) );
  }

  private static String getIsoDateTime()
  {
    return ZonedDateTime.now().format( ISO_ZONED_DATE_TIME );
  }

  private String getPrettyGeneratorName()
  {
    final String version = getClass().getPackage().getImplementationVersion();
    final String prettyVersion = version != null ? " (version: " + version : "";
    return generatorName + prettyVersion;
  }

  private void reportException( final Exception cause )
  {
    final Message message = new ExceptionMessage( cause, true, source );
    source.getErrorCollector().addFatalError( message );
  }
}
