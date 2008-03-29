/* ========================================================================== *
 *         Copyright (C) 2004-2006, Pier Fumagalli <http://could.it/>         *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */
package it.could.webdav.replication;

import it.could.util.location.Location;
import it.could.webdav.DAVListener;
import it.could.webdav.DAVLogger;
import it.could.webdav.DAVRepository;
import it.could.webdav.DAVServlet;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * <p>The {@link DAVReplicator} class is a {@link DAVListener} replicating
 * all content to the WebDAV repository specified at construction.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVReplicator extends HttpServlet {

    /** <p>The {@link DAVReplica} instances managed by this.</p> */
    private final List replicas = new ArrayList();

    /**
     * <p>Create a new {@link DAVServlet} instance.</p>
     */
    public DAVReplicator() {
        super();
    }

    /**
     * <p>Initialize this {@link Servlet} instance.</p>
     * 
     * <p>This servlet requires a couple of initialization parameters: the
     * first one is &quot;<code>repository</code>&quot; indicating the name of
     * the {@link DAVServlet} in the &quot;<code>web.xml</code>&quot; deployment
     * descriptor whose repository should be replicated.</p>
     *
     * <p>The second required parameter &quot;<code>replicas</code>&quot;
     * must contain a (whitespace separated list of) URL(s) where the original
     * repository should be replicated to.</p>
     *
     * <p>Finally, when set to <code>true</code>, the optional parameter
     * <code>debugEnabled</code> will enable logging of method invocation and
     * events in the repository.</p> 
     */
    public void init(ServletConfig config)
    throws ServletException {
        /* Initialize the super, just in case, and remember the context */
        super.init(config);
        
        /* Setup logging */
        boolean debug = "true".equals(config.getInitParameter("debugEnabled"));
        DAVLogger logger = new DAVLogger(config, debug);

        /* Try to retrieve the WebDAV repository from the servlet context */
        final String repositoryName = config.getInitParameter("repository");
        final DAVRepository repository;
        if (repositoryName == null) {
            throw new ServletException("Parameter \"rootPath\" not specified");
        } else try {
            final String key = DAVServlet.getRepositoryKey(repositoryName);
            final ServletContext context = config.getServletContext();
            repository = (DAVRepository) context.getAttribute(key);
            if (repository == null)
                throw new ServletException("Unable to access repository from " +
                                           "servlet \"" + repository + "\"");
        } catch (ClassCastException exception) {
            final String message = "Class cast exception accessing repository";
            throw new ServletException(message, exception);
        }

        /* Access the different WebDAV replicas */
        final String replicas = config.getInitParameter("replicas");
        if (replicas == null) {
            throw new ServletException("Parameter \"replicas\" not specified");
        } 
        
        try {
            final StringTokenizer tokenizer = new StringTokenizer(replicas);
            while (tokenizer.hasMoreTokens()) {
                final Location location = Location.parse(tokenizer.nextToken());
                final DAVReplica replica = new DAVReplica(repository, location,
                                                          logger);
                logger.log("Added repository replica to \"" + location + "\"");
                repository.addListener(replica);
                this.replicas.add(replica);
                replica.synchronize();
            }
        } catch (IOException exception) {
            throw new ServletException("Error creating replica", exception);
        }

        /* Check that we have at least one replica in */
        if (this.replicas.size() != 0) return;
        throw new ServletException("No replicas specified for repository");
    }
    
    /**
     * <p>Destroy {@link DAVServlet} instance interrupting all running
     * {@link DAVReplica} instances.</p>
     */
    public void destroy() {
        for (Iterator iter = this.replicas.iterator(); iter.hasNext() ; ) {
            ((DAVReplica) iter.next()).interrupt();
        }
    }
}
