/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.resource.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import java.io.File;

public abstract class AbstractCodeGeneratorMojo extends AbstractMojo {
  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   * @since 1.0
   */
  MavenProject project;

  /**
   * @parameter default-value="target/generated-sources/resgen"
   * @required
   */
  File outputDirectory;

  public void execute() {
    try {
      generate();

      project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

    } catch (Exception e) {
      getLog().error("General error", e);
    }
  }

  protected abstract void generate() throws Exception;
}

// End AbstractCodeGeneratorMojo.java
