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

import it.could.util.StreamTools;
import it.could.util.http.WebDavClient;
import it.could.util.location.Location;
import it.could.webdav.DAVListener;
import it.could.webdav.DAVLogger;
import it.could.webdav.DAVRepository;
import it.could.webdav.DAVResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p>TODO: Document this class.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVReplica extends Thread implements DAVListener {

    private static final int SYNCHRONIZE = -1;

    private final DAVRepository repository;
    private final DAVLogger logger;
    private final Location location;
    private final List actions = new ArrayList();

    public DAVReplica(DAVRepository repository, Location location,
                      DAVLogger logger)
    throws IOException {
        this.location = new WebDavClient(location).getLocation();
        this.repository = repository;
        this.logger = logger;
        this.start();
    }

    public void synchronize()
    throws IOException {
        this.logger.log("Scheduling full synchronization");
        this.notify(this.repository.getResource((String)null), SYNCHRONIZE);
    }

    public void notify(DAVResource resource, int event) {
        this.logger.debug("Event for \"" + resource.getRelativePath() + "\"");
        if (resource.getRepository() != this.repository) return;
        synchronized (this.actions) {
            this.actions.add(new Action(resource, event));
            this.actions.notify();
        }
    }

    public void run() {
        this.logger.debug("Starting background replica thread on " + location);
        while (true) try {
            final DAVReplica.Action array[];
            synchronized(this.actions) { 
                try {
                    if (this.actions.isEmpty()) this.actions.wait();
                    final int s = this.actions.size();
                    array = (Action []) this.actions.toArray(new Action[s]);
                    this.actions.clear();
                } catch (InterruptedException exception) {
                    this.logger.debug("Exiting background replica thread");
                    return;
                }
            }

            for (int x = 0; x < array.length; x ++) try {
                this.replicate(array[x]);
            } catch (Throwable throwable) {
                final String path = array[x].resource.getRelativePath();
                final String message = "Error synchronizing resource " + path;
                this.logger.log(message, throwable);
            }
        } catch (Throwable throwable) {
            this.logger.log("Replica thread attempted suicide", throwable);
        }
    }

    private void replicate(DAVReplica.Action action) {
        final DAVResource resource = action.resource; 

        if (action.event == SYNCHRONIZE) {
            this.synchronize(resource);

        } else try {
            final String path = resource.getParent().getRelativePath();
            final Location location = this.location.resolve(path); 
            final WebDavClient client = new WebDavClient(location);
            final String child = resource.getName();

            switch(action.event) {
            case RESOURCE_CREATED:
            case RESOURCE_MODIFIED:
                this.logger.debug("Putting resource " + path);
                this.put(resource, client);
                break;
            case RESOURCE_REMOVED:
            case COLLECTION_REMOVED:
                this.logger.debug("Deleting resource " + path);
                client.delete(child);
                break;
            case COLLECTION_CREATED:
                this.logger.debug("Creating collection " + path);
                client.mkcol(child);
                break;
            }
        } catch (IOException exception) {
            String message = "Error replicating " + resource.getRelativePath();
            this.logger.log(message, exception);
        }
    }
    
    private void put(DAVResource resource, WebDavClient client)
    throws IOException {
        final String name = resource.getName();
        final long length = resource.getContentLength().longValue(); 
        final OutputStream output = client.put(name, length);
        final InputStream input = resource.read();
        StreamTools.copy(input, output);
    }

    private void synchronize(DAVResource resource) {
        /* Figure out the path of the resource */
        final String path = resource.getRelativePath();

        /* If it's a file or null, just skip the whole thing */
        if (! resource.isCollection()) {
            this.logger.log("Synchronization on non-collection " + path);
            return;
        }

        /* Open a webdav client to the collection to synchronize */
        this.logger.log("Synchronizing collection " + path);
        final WebDavClient client;
        try {
            final Location location = this.location.resolve(path); 
            client = new WebDavClient(location);
        } catch (IOException exception) {
            this.logger.log("Error creating WebDAV client", exception);
            return;
        }

        /* Create a list of all children from the DAV client */
        final Set children = new HashSet();
        for (Iterator iter = client.iterator(); iter.hasNext(); )
            children.add(iter.next());

        /* Process all resource children one by one and ensure they exist */
        for (Iterator iter = resource.getChildren(); iter.hasNext(); ) {
            final DAVResource child = (DAVResource) iter.next();
            final String name = child.getName();

            /* Remove this from the resources that will be removed later */
            children.remove(name);

            /* If the client doesn't have this child, add it to the replica */
            if (! client.hasChild(name)) try {
                if (child.isCollection()) {
                    this.logger.debug("Client doesn't have collection " + name);
                    client.mkcol(name);
                    this.synchronize(child);

                } else {
                    this.logger.debug("Client doesn't have resource " + name);
                    this.put(child, client);
                }
            } catch (IOException exception) {
                this.logger.log("Error creating new child " + name, exception);

            /* If this child is a collection, it must be a collection on dav */
            } else if (child.isCollection()) try {
                if (!client.isCollection(name)) {
                    this.logger.debug("Recreating collection " + name);
                    client.delete(name).mkcol(name);
                }
                this.synchronize(child);
            } catch (IOException exception) {
                this.logger.log("Error creating collection " + name, exception);

            /* Ok, the resource is a normal one, verify size and timestamp */
            } else try {
                final Date rlast = child.getLastModified();
                final Date dlast = client.getLastModified(name);
                if ((rlast != null) && (rlast.equals(dlast))) {
                    final Long rlen = child.getContentLength();
                    final long dlen = client.getContentLength(name);
                    if ((rlen == null) || (rlen.longValue() != dlen)) {
                        this.logger.debug("Resending resource " + name);
                        this.put(child, client.delete(name));
                    }
                }
            } catch (IOException exception) {
                this.logger.log("Error resending resource " + name, exception);
            }
        }

        /* Any other child that was not removed above, will go away now! */
        for (Iterator iter = children.iterator(); iter.hasNext(); ) { 
            final String name = (String) iter.next();
            try {
                this.logger.debug("Removing leftovers " + name);
                client.delete(name);
            } catch (IOException exception) {
                this.logger.log("Error removing left over " + name, exception);
            }
        }
    }

    private static final class Action {
        final DAVResource resource;
        final int event;

        private Action(DAVResource resource, int event) {
            this.resource = resource;
            this.event = event;
        }
    }
}