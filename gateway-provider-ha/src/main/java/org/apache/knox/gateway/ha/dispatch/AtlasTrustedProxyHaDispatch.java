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

package org.apache.knox.gateway.ha.dispatch;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AtlasTrustedProxyHaDispatch extends DefaultHaDispatch {
    private static Set<String> REQUEST_EXCLUDE_HEADERS = new HashSet<>();

    static {
        REQUEST_EXCLUDE_HEADERS.add("Content-Length");
    }

    public AtlasTrustedProxyHaDispatch() {
        setServiceRole("ATLAS");
    }

    @Override
    public void init() {
        super.init();
    }


    @Override
    protected void executeRequest(HttpUriRequest outboundRequest,
                                  HttpServletRequest inboundRequest,
                                  HttpServletResponse outboundResponse) throws IOException {
        HttpResponse inboundResponse = null;
        try {
            inboundResponse = executeOutboundRequest(outboundRequest);

            int sc = inboundResponse.getStatusLine().getStatusCode();
            if (sc == HttpServletResponse.SC_MOVED_TEMPORARILY || sc == HttpServletResponse.SC_TEMPORARY_REDIRECT) {
                if (!isLoginRedirect(inboundResponse.getFirstHeader("Location"))) {
                    inboundResponse.removeHeaders("Location");
                    failoverRequest(outboundRequest,
                            inboundRequest,
                            outboundResponse,
                            inboundResponse,
                            new Exception("Atlas HA redirection"));
                }
            }

            writeOutboundResponse(outboundRequest, inboundRequest, outboundResponse, inboundResponse);

        } catch (IOException e) {
            LOG.errorConnectingToServer(outboundRequest.getURI().toString(), e);
            failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, e);
        }
    }

    private boolean isLoginRedirect(Header locationHeader) {
        boolean result = false;
        if (locationHeader != null) {
            String value = locationHeader.getValue();
            result = (value.endsWith("login.jsp") || value.contains("originalUrl"));
        }
        return result;
    }

}
