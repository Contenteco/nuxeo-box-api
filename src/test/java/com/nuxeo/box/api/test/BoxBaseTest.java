/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Damien Metzler <dmetzler@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package com.nuxeo.box.api.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @since 5.9.2
 */
public class BoxBaseTest {

    protected static enum RequestType {
        GET, POST, DELETE, PUT, POSTREQUEST
    }

    protected ObjectMapper mapper;

    protected WebResource service;

    @Before
    public void doBefore() throws Exception {
        service = getServiceFor("Administrator", "Administrator");
        mapper = new ObjectMapper();
    }

    protected WebResource getServiceFor(String user, String password) {
        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(MultiPartWriter.class);
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter(user, password));
        return client.resource("http://localhost:18090/nuxeo/site/box/2.0/");
    }

    protected ClientResponse getResponse(RequestType requestType, String path) {
        return getResponse(requestType, path, null, null, null);
    }

    protected ClientResponse getResponse(RequestType requestType, String path,
            String data, MultivaluedMap<String, String> queryParams,
            MultiPart mp) {
        WebResource wr = service.path(path);

        if (queryParams != null && !queryParams.isEmpty()) {
            wr = wr.queryParams(queryParams);
        }

        Builder builder = wr.accept(MediaType.APPLICATION_JSON) //
                .header("X-NXDocumentProperties", "dublincore");
        if (mp != null) {
            builder = wr.type(MediaType.MULTIPART_FORM_DATA_TYPE);
        }

        if (requestType == RequestType.POSTREQUEST) {
            builder.header("Content-type", "application/json+nxrequest");
        } else {
            builder.header("Content-type", "application/json+nxentity");
        }

        switch (requestType) {
        case GET:
            return builder.get(ClientResponse.class);
        case POST:
        case POSTREQUEST:
            if (mp != null) {
                return builder.post(ClientResponse.class, mp);
            } else {
                return builder.post(ClientResponse.class, data);
            }
        case PUT:
            if (mp != null) {
                return builder.put(ClientResponse.class, mp);
            } else {
                return builder.put(ClientResponse.class, data);
            }
        case DELETE:
            return builder.delete(ClientResponse.class, data);
        default:
            throw new RuntimeException();
        }
    }
}
