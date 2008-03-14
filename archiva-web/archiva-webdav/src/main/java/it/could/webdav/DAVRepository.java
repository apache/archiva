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
package it.could.webdav;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>A simple class representing a {@link File} based WebDAV repository.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVRepository {

    /** <p>A {@link String} of all acceptable characters in a URI.</p> */
    private static final String ACCEPTABLE =
                                "ABCDEFGHIJLKMNOPQRSTUVWXYZ" + // ALPHA (UPPER)
                                "abcdefghijklmnopqrstuvwxyz" + // ALPHA (LOWER)
                                "0123456789" +                 // DIGIT
                                "_-!.~'()*" +                  // UNRESERVED
                                ",;:$&+=" +                    // PUNCT
                                "?/[]@";                       // RESERVED


    /** <p>The {@link File} identifying the root of this repository.</p> */
    protected File root = null;  
    /** <p>The {@link URI} associated with the root of this repository.</p> */
    protected URI base = null;
    /** <p>The {@link Set} of all configured {@link DAVListener}s.</p> */
    private Set listeners = new HashSet();

    /**
     * <p>Create a new {@link DAVRepository} instance.</p>
     *
     * @param root The {@link File} identifying the root of the repository.
     * @throws IOException If the specified root is not a directory.
     * @throws NullPointerExceptoin If the specified root was <b>null</b>.
     */
    public DAVRepository(File root)
    throws IOException {
    	init(root);
    }

    protected void init(File root)
    throws IOException {
        if (root == null) throw new NullPointerException("Null root");
        if (root.isDirectory()) {
            this.root = root.getCanonicalFile();
            this.base = this.root.toURI().normalize();
        } else {
            throw new IOException("Root \"" + root + "\" is not a directory");
        }
    }

    /**
     * <p>Return the {@link URI} representing the root directory of this
     * {@link DAVRepository}.</p>
     * 
     * @return a <b>non-null</b> {@link URI} instance.
     */
    protected URI getRepositoryURI() {
        return (this.base);
    }

    /**
     * <p>Return the {@link DAVResource} associated with the given name.</p>
     * 
     * @param name a {@link String} identifying the resource name.
     * @return a <b>non-null</b> {@link DAVResource} instance.
     * @throws IOException If the resource could not be resolved.
     */
    public DAVResource getResource(String name)
    throws IOException {
        if (name == null) return this.getResource((URI) null);

        try {
            /* Encode the string into a URI */
            StringBuffer buffer = new StringBuffer();
            byte encoded[] = name.getBytes("UTF-8");
            for (int x = 0; x < encoded.length; x ++) {
                if (ACCEPTABLE.indexOf((int)encoded[x]) < 0) {
                    buffer.append('%');
                    buffer.append(DAVUtilities.toHexString(encoded[x]));
                    continue;
                }
                buffer.append((char) encoded[x]);
            }

            return this.getResource(new URI(buffer.toString()));
        } catch (URISyntaxException exception) {
            String message = "Invalid resource name \"" + name + "\"";
            throw (IOException) new IOException(message).initCause(exception);
        }
    }

    /**
     * <p>Return the {@link DAVResource} associated with a {@link URI}.</p>
     * 
     * <p>If the specified {@link URI} is relative it will be resolved against
     * the root of this {@link DAVRepository}.</p>
     * 
     * @param uri an absolute or relative {@link URI} identifying the resource.
     * @return a <b>non-null</b> {@link DAVResource} instance.
     * @throws IOException If the resource could not be resolved.
     */
    public DAVResource getResource(URI uri)
    throws IOException {
        if (uri == null) return new DAVResource(this, this.root);

        if (! uri.isAbsolute()) uri = this.base.resolve(uri).normalize();
        return new DAVResource(this, new File(uri).getAbsoluteFile());
    }
    
    /**
     * <p>Add a new {@link DAVListener} to the list of instances notified by
     * this {@link DAVRepository}.</p>
     */
    public void addListener(DAVListener listener) {
        if (listener != null) this.listeners.add(listener);
    }

    /**
     * <p>Remove a {@link DAVListener} from the list of instances notified by
     * this {@link DAVRepository}.</p>
     */
    public void removeListener(DAVListener listener) {
        if (listener != null) this.listeners.remove(listener);
    }

    /**
     * <p>Notify all configured {@link DAVListener}s of an event.</p>
     */
    protected void notify(DAVResource resource, int event) {
        if (resource == null) throw new NullPointerException("Null resource");
        if (resource.getRepository() != this)
            throw new IllegalArgumentException("Invalid resource");

        Iterator iterator = this.listeners.iterator();
        while (iterator.hasNext()) try {
            ((DAVListener)iterator.next()).notify(resource, event);
        } catch (RuntimeException exception) {
            // Swallow any RuntimeException thrown by listeners.
        }
    }
}
