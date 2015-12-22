/*
 * Licensed to Julian Hyde under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. Julian Hyde
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hydromatic.resource.maven;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;

/**
 * Generates a Resources class.
 *
 * @goal generate-sources
 * @phase generate-sources
 */
public class ResourceMojo extends AbstractCodeGeneratorMojo {
  /**
   * @parameter default-value="com.acme.resource"
   * @required
   */
  String packageName;

  @Override protected void generate() throws Exception {
    File pd = new File(outputDirectory, packageName.replaceAll("\\.", "/"));
    if (getLog().isDebugEnabled()) {
      getLog().info("Creating directory " + pd);
    }
    pd.mkdirs();

    InputStream in = null;
    try {
      in = ResourceMojo.class.getResourceAsStream(
              "/net/hydromatic/resource/Resources.java");
      String template = IOUtil.toString(in, "UTF-8");
      template = template.replace("package net.hydromatic.resource;",
          "package " + packageName + ";");
      saveResult(new File(pd, "Resources.java"), template);
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  private void saveResult(File file, String contents) throws IOException {
    if (file.exists()) {
      String prevContents = FileUtils.fileRead(file, "UTF-8");
      if (contents.equals(prevContents)) {
        getLog().info(file + " is up to date");
        return;
      }
    }
    getLog().info("Creating " + file);
    FileUtils.fileWrite(file, "UTF-8", contents);
  }
}

// End ResourceMojo.java
