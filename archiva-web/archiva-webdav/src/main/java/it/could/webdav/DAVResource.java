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
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * <p>A simple representation of a WebDAV resource based on {@link File}s.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVResource implements Comparable {
    
    /** <p>The mime type when {@link #isCollection()} is <b>true</b>.</p> */ 
    public static final String COLLECTION_MIME_TYPE = "httpd/unix-directory"; 

    /** <p>The prefix for all temporary resources.</p> */
    protected static final String PREFIX = ".dav_";
    /** <p>The suffix for all temporary resources.</p> */
    protected static final String SUFFIX = ".temp";
    /** <p>The {@link DAVRepository} instance containing this resource.</p> */
    private DAVRepository repository = null;
    /** <p>The {@link File} associated with this resource.</p> */
    private File file = null;

    /* ====================================================================== */
    /* Constructors                                                           */
    /* ====================================================================== */

    /**
     * <p>Create a new {@link DAVResource} instance.</p>
     */
    protected DAVResource(DAVRepository repo, File file) {
        if (repo == null) throw new NullPointerException("Null repository");
        if (file == null) throw new NullPointerException("Null resource");
        init(repo, file);
    }

    protected void init(DAVRepository repo, File file)
    {
        this.repository = repo;
        this.file = file;

        if (this.getRelativeURI().isAbsolute())
            throw new DAVException(412, "Error relativizing resource");
    }

    /* ====================================================================== */
    /* Generic object methods                                                 */
    /* ====================================================================== */
    
    /**
     * <p>Return an integer number for the hash value of this instance.</p>
     */
    public int hashCode() {
        return this.file.hashCode();
    }

    /**
     * <p>Compare this instance to another object for equality.</p>
     */
    public boolean equals(Object object) {
        if (object == null) return (false);
        if (object instanceof DAVResource) {
            DAVResource resource = (DAVResource) object;
            boolean u = this.file.equals(resource.file);
            boolean r = this.repository == resource.repository;
            return (u && r);
        } else {
            return (false);
        }
    }

    /**
     * <p>Compare this instance to another object for sorting.</p>
     */
    public int compareTo(Object object) {
        DAVResource resource = (DAVResource) object;
        return (this.file.compareTo(resource.file));
    }

    /* ====================================================================== */
    /* Resource checkers                                                      */
    /* ====================================================================== */
    
    /**
     * <p>Checks if this {@link DAVResource} is a null (non existant) one.</p>
     * 
     * @return <b>true</b> if this resource does not esist (is a null resource).
     */
    public boolean isNull() {
        return (! this.file.exists());
    }

    /**
     * <p>Checks if this {@link DAVResource} is a collection.</p>
     * 
     * @return <b>true</b> if this resource is a collection.
     */
    public boolean isCollection() {
        if (this.isNull()) return false;
        return (this.file.isDirectory());
    }

    /**
     * <p>Checks if this {@link DAVResource} is an existing resource.</p>
     * 
     * @return <b>true</b> if this resource is a collection.
     */
    public boolean isResource() {
        if (this.isNull()) return false;
        return (! this.isCollection());
    }
    
    /* ====================================================================== */
    /* Resource methods                                                       */
    /* ====================================================================== */

    /**
     * <p>Return the {@link File} associated with this resource.</p>
     */
    protected File getFile() {
        return this.file;
    }

    /**
     * <p>Return the {@link DAVRepository} associated with this resource.</p>
     */
    public DAVRepository getRepository() {
        return this.repository;
    }

    /**
     * <p>Return the bare name of this resource (without any &quot;/&quot;
     * slashes at the end if it is a collection).</p>
     * 
     * @return a <b>non null</b> {@link String}.
     */
    public String getName() {
        return this.file.getName();
    }

    /**
     * <p>Return the display name of this resource (with an added &quot;/&quot;
     * slash at the end if it is a collection).</p>
     * 
     * @return a <b>non null</b> {@link String}.
     */
    public String getDisplayName() {
        String name = this.getName();
        if (this.isCollection()) return (name + "/");
        return name;
    }

    /**
     * <p>Return the path of this {@link DAVResource} relative to the root
     * of the associated {@link DAVRepository}.</p>
     * 
     * @return a <b>non null</b> {@link String}.
     */
    public String getRelativePath() {
        return this.getRelativeURI().toASCIIString();
    }

    /**
     * <p>Return the {@link URI} of this {@link DAVResource} relative to the
     * root of the associated {@link DAVRepository}.</p>
     * 
     * @return a <b>non-null</b> {@link URI} instance.
     */
    public URI getRelativeURI() {
        URI uri = this.file.toURI();
        return this.repository.getRepositoryURI().relativize(uri).normalize();
    }

    /**
     * <p>Return the parent {@link DAVResource} of this instance.</p>
     * 
     * @return a <b>non-null</b> {@link DAVResource} instance or <b>null</b>
     *         if this {@link DAVResource} is the repository root.
     */
    public DAVResource getParent() {
        try {
            return new DAVResource(this.repository, this.file.getParentFile());
        } catch (Throwable throwable) {
            return null;
        }
    }

    /**
     * <p>Return an {@link Iterator} over all children of this instance.</p> 
     * 
     * @return a <b>non-null</b> {@link Iterator} instance or <b>null</b> if
     *         this {@link DAVResource} is not a collection.
     * @throws IOException If the resource could not be resolved.
     */
    public Iterator getChildren() {
        if (! this.isCollection()) return null;

        File children[] = this.file.listFiles();
        if (children == null) children = new File[0];
        List resources = new ArrayList(children.length);

        for (int x = 0; x < children.length; x++) {
            String c = children[x].getName();
            if (c.startsWith(PREFIX) && c.endsWith(SUFFIX)) continue;
            resources.add(new DAVResource(this.repository, children[x]));
        }

        return resources.iterator();
    }

    /* ====================================================================== */
    /* DAV Properties                                                         */
    /* ====================================================================== */
    
    /**
     * <p>Return the MIME Content-Type of this {@link DAVResource}.</p>
     * 
     * <p>If the {@link #isCollection()} method returns <b>true</b> this
     * method always returns <code>text/html</code>.</p>
     * 
     * @return a {@link String} instance or <b>null</b> if this resource does
     *         not exist.
     */
    public String getContentType() {
        if (this.isNull()) return null;
        if (this.isCollection()) return COLLECTION_MIME_TYPE;
        return DAVUtilities.getMimeType(this.getDisplayName());
   }

    /**
     * <p>Return the MIME Content-Length of this {@link DAVResource}.</p>
     * 
     * @return a {@link Long} instance or <b>null</b> if this resource does
     *         not exist or is a collection.
     */
    public Long getContentLength() {
        if (this.isNull() || this.isCollection()) return null;
        return new Long(this.file.length());
    }

    /**
     * <p>Return the creation date of this {@link DAVResource}.</p>
     * 
     * <p>As this implementation relies on a {@link File} backend, this method
     * will always return the same as {@link #getLastModified()}.</p>
     *
     * @return a {@link String} instance or <b>null</b> if this resource does
     *         not exist.
     */
    public Date getCreationDate() {
        if (this.isNull()) return null;
        return new Date(this.file.lastModified());
    }

    /**
     * <p>Return the last modification date of this {@link DAVResource}.</p>
     * 
     * @return a {@link String} instance or <b>null</b> if this resource does
     *         not exist.
     */
    public Date getLastModified() {
        if (this.isNull()) return null;
        return new Date(this.file.lastModified());
    }

    /**
     * <p>Return a {@link String} representing the Entity Tag of this
     * {@link DAVResource} as described by the
     * <a href="http://www.rfc-editor.org/rfc/rfc2616.txt">HTTP RFC</a>.</p>
     * 
     * @return a {@link String} instance or <b>null</b> if this resource does
     *         not exist.
     */
    public String getEntityTag() {
        if (this.isNull()) return null;

        String path = this.getRelativePath();
        StringBuffer etag = new StringBuffer();
        etag.append('"');
        
        /* Append the MD5 hash of this resource name */
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.reset();
            digester.update(path.getBytes("UTF8"));
            etag.append(DAVUtilities.toHexString(digester.digest()));
            etag.append('-');
        } catch (Exception e) {
            // If we can't get the MD5 HASH, let's ignore and hope...
        }
        
        /* Append the hashCode of this resource name */
        etag.append(DAVUtilities.toHexString(path.hashCode()));

        /* Append the last modification date if possible */
        Date date = this.getLastModified();
        if (date != null) {
            etag.append('-');
            etag.append(DAVUtilities.toHexString(date.getTime()));
        }

        /* Close the ETag */
        etag.append('"');
        return(etag.toString());
    }

    /* ====================================================================== */
    /* DAV Operations                                                         */
    /* ====================================================================== */

    /**
     * <p>Delete this resource.</p>
     *
     * @throws DAVException If for any reason this resource cannot be deleted.
     */
    public void delete()
    throws DAVMultiStatus {
        if (this.isNull()) throw new DAVException(404, "Not found", this);

        if (this.isResource()) {
            if (!windowsSafeDelete(this.file)) {
                throw new DAVException(403, "Can't delete resource", this);
            } else {
                this.repository.notify(this, DAVListener.RESOURCE_REMOVED);
            }
        } else if (this.isCollection()) {
            DAVMultiStatus multistatus = new DAVMultiStatus();

            Iterator children = this.getChildren();
            while (children.hasNext()) try {
                ((DAVResource)children.next()).delete();
            } catch (DAVException exception) {
                multistatus.merge(exception);
            }
            
            if (multistatus.size() > 0) throw multistatus;
            if (!this.file.delete()) {
                throw new DAVException(403, "Can't delete collection", this);
            } else {
                this.repository.notify(this, DAVListener.COLLECTION_REMOVED);
            }
        }
    }

    /**
     * <p>Copy this resource to the specified destination.</p>
     *
     * @throws DAVException If for any reason this resource cannot be deleted.
     */
    public void copy(DAVResource dest, boolean overwrite, boolean recursive)
    throws DAVMultiStatus {

        /*
         * NOTE: Since the COPY operation relies on other operation defined in
         * this class (and in DAVOutputStream for resources) rather than on
         * files temselves, notifications are sent elsewhere, not here.
         */

        if (this.isNull()) throw new DAVException(404, "Not found", this);

        /* Check if the destination exists and delete if possible */
        if (!dest.isNull()) {
            if (! overwrite) {
                String msg = "Not overwriting existing destination";
                throw new DAVException(412, msg, dest);
            }
            dest.delete();
        }

        /* Copy a single resource (destination is null as we deleted it) */
        if (this.isResource()) {
            DAVInputStream in = this.read();
            DAVOutputStream out = dest.write();
            byte buffer[] = new byte[4096];
            int k = -1;
            while ((k = in.read(buffer)) != -1) out.write(buffer, 0, k);
            in.close();
            out.close();
        }

        /* Copy the collection and all nested members */
        if (this.isCollection()) {
            dest.makeCollection();
            if (! recursive) return;

            DAVMultiStatus multistatus = new DAVMultiStatus();
            Iterator children = this.getChildren();
            while (children.hasNext()) try {
                DAVResource childResource = (DAVResource) children.next();
                File child = new File(dest.file, childResource.file.getName());
                DAVResource target = new DAVResource(this.repository, child);
                childResource.copy(target, overwrite, recursive);
            } catch (DAVException exception) {
                multistatus.merge(exception);
            }
            if (multistatus.size() > 0) throw multistatus;
        }
    }

    /**
     * <p>Moves this resource to the specified destination.</p>
     *
     * @throws DAVException If for any reason this resource cannot be deleted.
     */
    public void move(DAVResource dest, boolean overwrite, boolean recursive)
    throws DAVMultiStatus {
    	// the base class implementation is just copy-then-delete
    	copy(dest, overwrite, recursive);
    	this.delete();
    }
    
    /**
     * <p>Create a collection identified by this {@link DAVResource}.</p>
     *
     * <p>This resource must be {@link #isNull() non-null} and its
     * {@link #getParent() parent} must be accessible and be a
     * {@link #isCollection() collection}.</p>
     * 
     * @throws DAVException If for any reason a collection identified by this
     *                      resource cannot be created.
     */
    public void makeCollection() {
        DAVResource parent = this.getParent();
        if (!this.isNull())
            throw new DAVException(405, "Resource exists", this);
        if (parent.isNull())
            throw new DAVException(409, "Parent does not not exist", this);
        if (!parent.isCollection())
            throw new DAVException(403, "Parent not a collection", this);
        if (!this.file.mkdir())
            throw new DAVException(507, "Can't create collection", this);
        this.repository.notify(this, DAVListener.COLLECTION_CREATED);
    }

    /**
     * <p>Return an {@link InputStream} reading the resource.</p>
     *
     * @return a <b>non-null</b> {@link InputStream} instance.
     */
    public DAVInputStream read() {
        if (this.isNull()) throw new DAVException(404, "Not found", this);
        if (this.isCollection())
            throw new DAVException (403, "Resource is collection", this);
        return new DAVInputStream(this);
    }
    
    /**
     * <p>Return a {@link DAVOutputStream} writing to this {@link DAVResource}
     * instance.</p>
     *
     * @return a <b>non-null</b> {@link DAVOutputStream} instance.
     */
    public DAVOutputStream write() {
        DAVResource parent = this.getParent();
        if (this.isCollection())
            throw new DAVException(409, "Can't write a collection", this);
        if (parent.isNull())
            throw new DAVException(409, "Parent doesn't exist", this);
        if (! parent.isCollection())
            throw new DAVException(403, "Parent not a collection", this);
        return new DAVOutputStream(this);
    }
    
    /** File.delete(file) sometimes fails transiently on Windows.
     * This occurs even in low-I/O conditions, with file Explorer closed.
     * Delete can still fail (correctly) due to the separate Windows problem 
     * of file sharing violations.
     * @return the status of the last attempt of File.delete()
     */
    private static boolean windowsSafeDelete(File f)
    {
    	// www.mail-archive.com/java-user@lucene.apache.org/msg08994.html
    	boolean success = f.delete();
    	int attempts = 1;
    	while(!success && f.exists() && attempts < 3) {
    		if(attempts > 2) {
    			System.gc();
    		}
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignore) {
            }
            success = f.delete();
            attempts++;
    	}
    	return success;    	 
    }
    
}
