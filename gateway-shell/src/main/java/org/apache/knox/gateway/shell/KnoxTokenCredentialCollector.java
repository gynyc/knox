/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.shell;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.knox.gateway.util.JsonUtils;

public class KnoxTokenCredentialCollector extends AbstractCredentialCollector {

  public static final String COLLECTOR_TYPE = "KnoxToken";

  private static final String KNOXTOKENCACHE = ".knoxtokencache";

  private String targetUrl;

  private String tokenType;

  @Override
  public void collect() throws CredentialCollectionException {
    String userDir = System.getProperty("user.home");
    File knoxtoken = new File(userDir, KNOXTOKENCACHE);
    if (knoxtoken.exists()) {
      Path path = Paths.get(knoxtoken.toURI());
      List<String> lines;
      try {
        lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (!lines.isEmpty()) {
          Map<String, String> attrs = JsonUtils.getMapFromJsonString(lines.get(0));
          value = attrs.get("access_token");
          targetUrl = attrs.get("target_url");
          tokenType = attrs.get("token_type");
          Date expires = new Date(Long.parseLong(attrs.get("expires_in")));
          if (expires.before(new Date())) {
            throw new CredentialCollectionException("Cached knox token has expired. Please relogin through knoxinit.");
          }
        }
      } catch (IOException e) {
        throw new CredentialCollectionException("Cached knox token cannot be read. Please login through knoxinit.", e);
      }
    } else {
      throw new CredentialCollectionException("Cached knox token cannot be found. Please login through knoxinit.");
    }
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public String getTokenType() {
    return tokenType;
  }

  @Override
  public String type() {
    return COLLECTOR_TYPE;
  }
}
