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

import org.apache.archiva.repository.api.RepositoryContext;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerFactory;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.interceptor.PostRepositoryInterceptor;
import org.apache.archiva.repository.api.interceptor.PreRepositoryInterceptor;
import org.apache.archiva.repository.api.interceptor.RepositoryInterceptorFactory;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.archiva.repository.api.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryServlet extends HttpServlet
{
    private static final Logger log = LoggerFactory.getLogger(RepositoryServlet.class);

    private final RepositoryInterceptorFactory<PreRepositoryInterceptor> preRepositoryInterceptorFactory;

    private final RepositoryInterceptorFactory<PostRepositoryInterceptor> postRepositoryInterceptorFactory;

    private final RepositoryManagerFactory repositoryManagerFactory;

    private static final String MKCOL_METHOD = "MKCOL";

    private static final String LAST_MODIFIED = "last-modified";

    public RepositoryServlet( RepositoryInterceptorFactory<PreRepositoryInterceptor> preRepositoryInterceptorFactory,
            RepositoryInterceptorFactory<PostRepositoryInterceptor> postRepositoryInterceptorFactory,
            RepositoryManagerFactory repositoryManagerFactory)
    {
        this.preRepositoryInterceptorFactory = preRepositoryInterceptorFactory;
        this.postRepositoryInterceptorFactory = postRepositoryInterceptorFactory;
        this.repositoryManagerFactory = repositoryManagerFactory;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setHeader("Server", "Apache Archiva");
        resp.setDateHeader("Date", new Date().getTime());

        //Backwards compatability with the weddav wagon
        if (MKCOL_METHOD.equals(req.getMethod()))
        {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            return;
        }
        super.service(req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp);
    }

    private void handleRequest(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException
    {
        log.debug("Request started");
        HttpRepositoryContext context = new HttpRepositoryContext(req, resp);
        log.debug("Running PreRepositoryInterceptors");
        executePreRepositoryInterceptors(context);
        log.debug("Executing Repository Manager");
        runRepositoryManager(context, req, resp);
        log.debug("Running PostRepositoryInterceptors");
        executePostRepositoryInterceptors(context);
        log.debug("Request Completed");
    }

    private void executePreRepositoryInterceptors(RepositoryContext context)
    {
        for (final PreRepositoryInterceptor interceptor : preRepositoryInterceptorFactory.getRepositoryInterceptors())
        {
            interceptor.intercept(context);
        }
    }

    private void executePostRepositoryInterceptors(RepositoryContext context)
    {
        for (final PostRepositoryInterceptor interceptor : postRepositoryInterceptorFactory.getRepositoryInterceptors())
        {
            interceptor.intercept(context);
        }
    }

    private void runRepositoryManager(final RepositoryContext context, final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException
    {
        for (final RepositoryManager manager : repositoryManagerFactory.getRepositoryManagers())
        {
            final ResourceContext resourceContext = manager.handles(context);
            if (resourceContext != null)
            {
                log.debug("Request handled by " + manager.toString());
                doContent(manager, resourceContext, req, resp);
                break;
            }
        }
    }

    private void doContent(final RepositoryManager repositoryManager, final ResourceContext context, final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException
    {
        if ("PUT".equals(req.getMethod()))
        {
            repositoryManager.write(context, req.getInputStream());
            return;
        }

        final List<Status> results = repositoryManager.stat(context);

        if (!results.isEmpty())
        {
            final boolean withBody = !"HEAD".equals(req.getMethod());
            final Status status = results.get(0);
            if (ResourceType.Collection.equals(status.getResourceType()))
            {
                //If does not end with slash we should redirect
                if (!req.getRequestURI().endsWith("/" ))
                {
                    resp.sendRedirect(req.getRequestURI() + "/");
                    return;
                }

                Status collectionStatus = results.get(0);
                resp.setDateHeader(LAST_MODIFIED, collectionStatus.getLastModified());
                resp.setStatus(HttpServletResponse.SC_OK);

                IndexWriter.write(results, context, resp, withBody);
            }
            else
            {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentLength((int)status.getContentLength());
                resp.setContentType(status.getContentType());
                resp.setDateHeader(LAST_MODIFIED, status.getLastModified());

                if (withBody)
                {
                    repositoryManager.read(context, resp.getOutputStream());
                }
            }
        }
        else
        {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
