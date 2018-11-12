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

package org.luossfi.gpdgen.groovy.transform;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.luossfi.gpdgen.common.api.GradlePlugin;
import org.luossfi.gpdgen.groovy.internal.GradlePluginAnnotatedClassService;

import static org.codehaus.groovy.control.CompilePhase.OUTPUT;

/**
 * This AST transformation takes the value of the {@link org.luossfi.gpdgen.common.api.GradlePlugin @GradlePlugin} annotation and generates a plugin
 * descriptor file if both the value and the annotated class are valid.
 *
 * @author Steff Lukas
 */
@GroovyASTTransformation( phase = OUTPUT )
public class GradlePluginAstTransformation extends AbstractASTTransformation
{
  private static final ClassNode GRADLE_PLUGIN_ANNOTATION = ClassHelper.make( GradlePlugin.class );

  /**
   * Instantiates a new GradlePluginAstTransformation.
   */
  public GradlePluginAstTransformation()
  {
    super();
  }

  @Override
  public void visit( final ASTNode[] nodes, final SourceUnit source )
  {

    final ModuleNode ast = source.getAST();
    ast.getClasses()
       .stream()
       .filter( GradlePluginAstTransformation::isNotInnerClass )
       .filter( this::isGradlePluginAnnotated )
       .map( classNode -> instantiateGradlePluginAnnotatedClassService( classNode, source ) )
       .forEach( GradlePluginAnnotatedClassService::generateGradlePluginDescriptors );


  }

  private boolean isGradlePluginAnnotated( final ClassNode classNode )
  {
    return hasAnnotation( classNode, GRADLE_PLUGIN_ANNOTATION );
  }

  private static boolean isNotInnerClass( final ClassNode classNode )
  {
    return classNode.getOuterClass() == null;
  }

  private GradlePluginAnnotatedClassService instantiateGradlePluginAnnotatedClassService( final ClassNode classNode, final SourceUnit source )
  {
    return new GradlePluginAnnotatedClassService( classNode, source, getClass().getName() );
  }
}
