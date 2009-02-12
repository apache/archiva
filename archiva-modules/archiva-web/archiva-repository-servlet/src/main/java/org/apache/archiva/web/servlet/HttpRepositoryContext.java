package org.apache.archiva.web.servlet;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.api.PathUtils;
import org.apache.archiva.repository.api.RepositoryContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.archiva.repository.api.MimeTypes;
import org.apache.archiva.repository.api.RequestType;
import org.apache.commons.codec.binary.Base64;

public class HttpRepositoryContext implements RepositoryContext
{
    private HttpServletRequest request;
    private HttpServletResponse response;

    private final String logicalPath;
    private final String principal;
    private final String repositoryId;
    private final RequestType requestType;
    private int contentLength;

    public HttpRepositoryContext(HttpServletRequest request, HttpServletResponse response)
    {
        this.request = request;
        this.response = response;

        this.repositoryId = PathUtils.getRepositoryId(request.getPathInfo());
        this.logicalPath = PathUtils.getLogicalPath(request.getPathInfo());

        if (request.getAuthType() != null && !HttpServletRequest.BASIC_AUTH.equals(request.getAuthType()))
        {
            throw new RuntimeException("Authentication type " + request.getAuthType() + " is not supported");
        }

        this.principal = getPrincipal(request);

        if ("PUT".equals(request.getMethod()))
        {
            this.requestType = RequestType.Write;
        }
        else
        {
            this.requestType = RequestType.Read;
        }
    }

    private static String getPrincipal(HttpServletRequest request)
    {
        String header = request.getHeader( "Authorization" );

        // in tomcat this is : authorization=Basic YWRtaW46TWFuYWdlMDc=
        if ( header == null )
        {
            header = request.getHeader( "authorization" );
        }

        if ( ( header != null ) && header.startsWith( "Basic " ) )
        {
            String base64Token = header.substring( 6 );
            String token = new String( Base64.decodeBase64( base64Token.getBytes() ) );

            String username = "";
            int delim = token.indexOf( ':' );

            if ( delim != ( -1 ) )
            {
                username = token.substring( 0, delim );
            }
            return username;
        }
        return null;
    }

    public String getLogicalPath()
    {
        return logicalPath;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public RequestType getRequestType()
    {
        return requestType;
    }

    public InputStream getInputStream()
    {
        try
        {
            return request.getInputStream();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public OutputStream getOutputStream()
    {
        try
        {
            //Head should not return a body
            if ("HEAD".equals(request.getMethod()))
            {
                return null;
            }
            return response.getOutputStream();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(int contentLength)
    {
        this.contentLength = contentLength;
    }

    public String getContentType()
    {
        return MimeTypes.getMimeType(logicalPath);
    }
}
