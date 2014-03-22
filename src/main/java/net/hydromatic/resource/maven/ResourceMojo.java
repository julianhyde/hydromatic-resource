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

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

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
    pd.mkdirs();

    FileWriter out = new FileWriter(new File(pd, "Resources.java"));
    try {
      final URL resource =
          ResourceMojo.class.getResource("/Resources.java.template");
      final InputStream in = resource.openStream();
      final InputStreamReader reader = new InputStreamReader(in);
      final StringBuilder sb = new StringBuilder();
      char[] chars = new char[1024];
      for (;;) {
        final int n = reader.read(chars);
        if (n < 0) {
          break;
        }
        sb.append(chars, 0, n);
      }
      in.close();
      reader.close();
      replaceAll(sb, "<%package%>", packageName);
      out.append(sb.toString());
    } finally {
      out.flush();
      out.close();
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug("Resources.java");
    }
  }

  private void replaceAll(StringBuilder sb, String find, String replace) {
    if (find.length() == 0) {
      return;
    }

    for (int i = sb.length(); i >= 0;) {
      i = sb.lastIndexOf(find, i);
      if (i < 0) {
        return;
      }
      sb.replace(i, i + find.length(), replace);
    }
  }
}

// End ResourceMojo.java
