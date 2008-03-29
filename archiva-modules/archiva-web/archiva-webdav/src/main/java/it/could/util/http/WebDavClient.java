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
package it.could.util.http;

import it.could.util.StreamTools;
import it.could.util.StringTools;
import it.could.util.location.Location;
import it.could.util.location.Path;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * <p>A class implementing an extremely simple WebDAV Level 1 client based on
 * the {@link HttpClient}.</p>
 * 
 * <p>Once opened this class will represent a WebDAV collection. Users of this
 * class can then from an instance of this, deal with relative parent and
 * children resources.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class WebDavClient {

    /** <p>The WebDAV resource asociated with this instance.</p> */
    private Resource resource;
    /** <p>A map of children resources of this instance.</p> */
    private Map children;

    /**
     * <p>Create a new {@link WebDavClient} instance opening the collection
     * identified by the specified {@link Location}.</p>
     * 
     * @param location the {@link Location} of the WebDAV collection to open.
     * @throws IOException if an I/O or network error occurred, or if the
     *                     {@link Location} specified does not point to a
     *                     WebDAV collection.
     * @throws NullPointerException if the {@link Location} was <b>null</b>.
     */
    public WebDavClient(Location location)
    throws NullPointerException, IOException {
        if (location == null) throw new NullPointerException("Null location");
        this.reload(location);
    }

    /* ====================================================================== */
    /* ACTIONS                                                                */
    /* ====================================================================== */

    /**
     * <p>Refresh this {@link WebDavClient} instance re-connecting to the remote
     * collection and re-reading its properties.</p>
     * 
     * @return this {@link WebDavClient} instance.
     */
    public WebDavClient refresh()
    throws IOException {
        this.reload(this.resource.location);
        return this;
    }

    /**
     * <p>Fetch the contents of the specified child resource of the collection
     * represented by this {@link WebDavClient} instance.</p>
     * 
     * @see #isCollection(String)
     * @return a <b>non-null</b> {@link InputStream}.
     * @throws IOException if an I/O or network error occurred, or if the
     *                     child specified represents a collection.
     * @throws NullPointerException if the child was <b>null</b>.
     */
    public InputStream get(String child)
    throws NullPointerException, IOException {
        if (child == null) throw new NullPointerException("Null child");
        if (! this.isCollection(child)) {
            final Location location = this.getLocation(child);
            final HttpClient client = new HttpClient(location);
            client.setAcceptableStatus(200).connect("GET");
            return client.getResponseStream();
        }
        throw new IOException("Child \"" + child + "\" is a collection");
    }

    /**
     * <p>Delete the child resource (or collection) of the collection
     * represented by this {@link WebDavClient} instance.</p>
     * 
     * @return this {@link WebDavClient} instance.
     * @throws IOException if an I/O or network error occurred, or if the
     *                     child specified represents a collection.
     * @throws NullPointerException if the child was <b>null</b>.
     */
    public WebDavClient delete(String child)
    throws NullPointerException, IOException {
        if (child == null) throw new NullPointerException("Null child");
        final HttpClient client = new HttpClient(this.getLocation(child));
        client.setAcceptableStatus(204).connect("DELETE").disconnect();
        return this.refresh();
    }

    /**
     * <p>Create a new collection as a child of the collection represented
     * by this {@link WebDavClient} instance.</p>
     * 
     * <p>In comparison to {@link #put(String)} and {@link #put(String, long)}
     * this method will fail if the specified child already exist.</p>
     * 
     * @see #hasChild(String)
     * @return this {@link WebDavClient} instance.
     * @throws IOException if an I/O or network error occurred, or if the
     *                     child specified already exist.
     * @throws NullPointerException if the child was <b>null</b>.
     */
    public WebDavClient mkcol(String child)
    throws NullPointerException, IOException {
        if (child == null) throw new NullPointerException("Null child");
        if (this.hasChild(child))
            throw new IOException("Child \"" + child + "\" already exists");
        final Location location = this.resource.location.resolve(child);
        final HttpClient client = new HttpClient(location);
        client.setAcceptableStatus(201).connect("MKCOL").disconnect();
        return this.refresh();
    }

    /**
     * <p>Create a new (or update the contents of a) child of of the collection
     * represented by this {@link WebDavClient} instance.</p>
     * 
     * <p>This method will behave exactly like the {@link #put(String, long)}
     * method, but the data written to the returned {@link OutputStream} will
     * be <i>buffered in memory</i> and will be transmitted to the remote
     * server only when the {@link OutputStream#close()} method is called.</p>
     * 
     * <p>If the returned {@link OutputStream} is garbage collected before the
     * {@link OutputStream#close() close()} method is called, the entire
     * transaction will be aborted and no connection to the remote server will
     * be established.</p>
     * 
     * <p>Use this method in extreme cases. In normal circumstances always rely
     * on the {@link #put(String, long)} method.</p>
     * 
     * @see #put(String, long)
     * @return a <b>non-null</b> {@link OutputStream} instance.
     * @throws NullPointerException if the child was <b>null</b>.
     */
    public OutputStream put(final String child)
    throws NullPointerException {
        if (child == null) throw new NullPointerException("Null child");
        final WebDavClient client = this;
        return new ByteArrayOutputStream() {
            private boolean closed = false;
            public void close()
            throws IOException {
                if (this.closed) return;
                this.flush();
                OutputStream output = client.put(child, this.buf.length);
                output.write(this.buf);
                output.flush();
                output.close();
            }

            protected void finalize()
            throws Throwable {
                this.closed = true;
                super.finalize();
            }
        };
    }

    /**
     * <p>Create a new (or update the contents of a) child of of the collection
     * represented by this {@link WebDavClient} instance.</p>
     * 
     * <p>If the specified child {@link #hasChild(String) already exists} on
     * the remote server, it will be {@link #delete(String) deleted} before
     * writing.</p>
     * 
     * @return a <b>non-null</b> {@link OutputStream} instance.
     * @throws NullPointerException if the child was <b>null</b>.
     * @throws IOException if an I/O or network error occurred, or if the
     *                     child specified already exist.
     */
    public OutputStream put(String child, long length)
    throws NullPointerException, IOException {
        if (child == null) throw new NullPointerException("Null child");
        if (this.hasChild(child)) this.delete(child);
        final Location location = this.resource.location.resolve(child);
        final HttpClient client = new HttpClient(location);
        client.setAcceptableStatuses(new int[] { 201, 204 });
        client.connect("PUT", length);
        
        final WebDavClient webdav = this;
        return new BufferedOutputStream(client.getRequestStream()) {
            boolean closed = false;
            public void close()
            throws IOException {
                if (this.closed) return;
                try {
                    super.close();
                } finally {
                    this.closed = true;
                    webdav.refresh();
                }
            }
            protected void finalize()
            throws Throwable {
                try {
                    this.close();
                } finally {
                    super.finalize();
                }
            }
        };
    }

    /**
     * <p>Open the specified child collection of the collection represented by
     * this {@link WebDavClient} as a new {@link WebDavClient} instance.</p>
     * 
     * <p>If the specified child is &quot;<code>.</code>&quot; this method
     * will behave exactly like {@link #refresh()} and <i>this instance</i>
     * will be returned.</p>
     * 
     * <p>If the specified child is &quot;<code>..</code>&quot; this method
     * will behave exactly like {@link #parent()}.</p>
     *
     * @return a <b>non-null</b> {@link WebDavClient} instance.
     * @throws NullPointerException if the child was <b>null</b>.
     * @throws IOException if an I/O or network error occurred, or if the
     *                     child specified did not exist.
     */
    public WebDavClient open(String child)
    throws NullPointerException, IOException {
        if (child == null) throw new NullPointerException("Null child");
        if (".".equals(child)) return this.refresh();
        if ("..".equals(child)) return this.parent();
        if (resource.collection) {
            Location loc = this.getLocation().resolve(this.getLocation(child));
            return new WebDavClient(loc);
        }
        throw new IOException("Child \"" + child + "\" is not a collection");
    }

    /**
     * <p>Open the parent collection of the collection represented by this
     * {@link WebDavClient} as a new {@link WebDavClient} instance.</p>
     *
     * @return a <b>non-null</b> {@link WebDavClient} instance.
     * @throws IOException if an I/O or network error occurred, or if the
     *                     child specified did not exist.
     */
    public WebDavClient parent()
    throws IOException {
        final Location location = this.resource.location.resolve("..");
        return new WebDavClient(location);
    }

    /* ====================================================================== */
    /* ACCESSOR METHODS                                                       */
    /* ====================================================================== */

    /**
     * <p>Return an {@link Iterator} over {@link String}s for all the children
     * of the collection represented by this {@link WebDavClient} instance.</p>
     */
    public Iterator iterator() {
        return this.children.keySet().iterator();
    }

    /**
     * <p>Checks if the collection represented by this {@link WebDavClient}
     * contains the specified child.</p>
     */
    public boolean hasChild(String child) {
        return this.children.containsKey(child);
    }

    /**
     * <p>Return the {@link Location} associated with the collection
     * represented by this {@link WebDavClient}.</p>
     *
     * <p>The returned {@link Location} can be different from the one specified
     * at construction, in case the server redirected us upon connection.</p>
     */
    public Location getLocation() {
        return this.resource.location;
    }

    /**
     * <p>Return the content length (in bytes) of the collection represented
     * by this {@link WebDavClient} as passed to us by the WebDAV server.</p>
     */
    public long getContentLength() {
        return this.resource.contentLength;
    }

    /**
     * <p>Return the content type (mime-type) of the collection represented
     * by this {@link WebDavClient} as passed to us by the WebDAV server.</p>
     */
    public String getContentType() {
        return this.resource.contentType;
    }

    /**
     * <p>Return the last modified {@link Date} of the collection represented
     * by this {@link WebDavClient} as passed to us by the WebDAV server.</p>
     */
    public Date getLastModified() {
        return this.resource.lastModified;
    }

    /**
     * <p>Return the creation {@link Date} of the collection represented
     * by this {@link WebDavClient} as passed to us by the WebDAV server.</p>
     */
    public Date getCreationDate() {
        return this.resource.creationDate;
    }

    /**
     * <p>Return the {@link Location} associated with the specified child of
     * the collection represented by this {@link WebDavClient}.</p>
     *
     * @throws IOException if the specified child does not exist.
     * @throws NullPointerException if the specified child was <b>null</b>.
     */
    public Location getLocation(String child)
    throws IOException {
        Location location = this.getResource(child).location;
        return this.resource.location.resolve(location);
    }

    /**
     * <p>Checks if the specified child of the collection represented by this
     * {@link WebDavClient} instance is a collection.</p>
     */
    public boolean isCollection(String child)
    throws IOException {
        return this.getResource(child).collection;
    }

    /**
     * <p>Return the content length (in bytes) associated with the specified
     * child of the collection represented by this {@link WebDavClient}.</p>
     *
     * @throws IOException if the specified child does not exist.
     * @throws NullPointerException if the specified child was <b>null</b>.
     */
    public long getContentLength(String child)
    throws IOException {
        return this.getResource(child).contentLength;
    }

    /**
     * <p>Return the content type (mime-type) associated with the specified
     * child of the collection represented by this {@link WebDavClient}.</p>
     *
     * @throws IOException if the specified child does not exist.
     * @throws NullPointerException if the specified child was <b>null</b>.
     */
    public String getContentType(String child)
    throws IOException {
        return this.getResource(child).contentType;
    }

    /**
     * <p>Return the last modified {@link Date} associated with the specified
     * child of the collection represented by this {@link WebDavClient}.</p>
     *
     * @throws IOException if the specified child does not exist.
     * @throws NullPointerException if the specified child was <b>null</b>.
     */
    public Date getLastModified(String child)
    throws IOException {
        return this.getResource(child).lastModified;
    }

    /**
     * <p>Return the creation {@link Date} associated with the specified
     * child of the collection represented by this {@link WebDavClient}.</p>
     *
     * @throws IOException if the specified child does not exist.
     * @throws NullPointerException if the specified child was <b>null</b>.
     */
    public Date getCreationDate(String child)
    throws IOException {
        return this.getResource(child).creationDate;
    }

    /* ====================================================================== */
    /* INTERNAL METHODS                                                       */
    /* ====================================================================== */

    /**
     * <p>Return the resource associated with the specified child.</p>
     *
     * @throws IOException if the specified child does not exist.
     * @throws NullPointerException if the specified child was <b>null</b>.
     */
    private Resource getResource(String child)
    throws IOException {
        if (child == null) throw new NullPointerException();
        final Resource resource = (Resource) this.children.get(child);
        if (resource == null) throw new IOException("Not found: " + child);
        return resource;
    }

    /**
     * <p>Contact the remote WebDAV server and fetch all properties.</p>
     */
    private void reload(Location location)
    throws IOException {

        /* Do an OPTIONS over onto the location */
        location = this.options(location);

        /* Do a PROPFIND to figure out the properties and the children */
        final Iterator iterator = this.propfind(location).iterator();
        final Map children = new HashMap();
        while (iterator.hasNext()) {
            final Resource resource = (Resource) iterator.next();
            final Path path = resource.location.getPath();
            if (path.size() == 0) {
                resource.location = location.resolve(resource.location);
                this.resource = resource;
            } else if (path.size() == 1) {
                final Path.Element element = (Path.Element) path.get(0);
                if ("..".equals(element.getName())) continue;
                children.put(element.toString(), resource);
            }
        }
        
        /* Check if the current resource was discovered */
        if (this.resource == null)
            throw new IOException("Current resource not returned in PROOPFIND");

        /* Don't actually allow resources to be modified */ 
        this.children = Collections.unmodifiableMap(children);
    }

    /**
     * <p>Contact the remote WebDAV server and do an OPTIONS lookup.</p>
     */
    private Location options(Location location)
    throws IOException {
        /* Create the new HttpClient instance associated with the location */
        final HttpClient client = new HttpClient(location);
        client.setAcceptableStatus(200).connect("OPTIONS", true).disconnect();

        /* Check that the remote server returned the "Dav" header */
        final List davHeader = client.getResponseHeaderValues("dav");
        if (davHeader == null) {
            throw new IOException("Server did not respond with a DAV header");
        }

        /* Check if the OPTIONS request contained the DAV header */
        final Iterator iterator = davHeader.iterator();
        boolean foundLevel1 = false;
        while (iterator.hasNext() && (! foundLevel1)) {
            String value = (String) iterator.next();
            StringTokenizer tokenizer = new StringTokenizer(value, ",");
            while (tokenizer.hasMoreTokens()) {
                if (! "1".equals(tokenizer.nextToken().trim())) continue;
                foundLevel1 = true;
                break;
            }
        }
        
        /* Return the (possibly redirected) location or fail miserably */
        if (foundLevel1) return client.getLocation();
        throw new IOException("Server doesn't support DAV Level 1");
    }
    
    /**
     * <p>Contact the remote WebDAV server and do a PROPFIND lookup, returning
     * a {@link List} of all scavenged resources.</p>
     */
    private List propfind(Location location)
    throws IOException {
        /* Create the new HttpClient instance associated with the location */
        final HttpClient client = new HttpClient(location);
        client.addRequestHeader("Depth", "1");
        client.setAcceptableStatus(207).connect("PROPFIND", true);

        /* Get the XML SAX Parser and parse the output of the PROPFIND */
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            final SAXParser parser = factory.newSAXParser();
            final String systemId = location.toString();
            final InputSource source = new InputSource(systemId);
            final Handler handler = new Handler(location);
            source.setByteStream(client.getResponseStream());
            parser.parse(source, handler);
            return handler.list;

        } catch (ParserConfigurationException exception) {
            Exception throwable = new IOException("Error creating XML parser");
            throw (IOException) throwable.initCause(exception);
        } catch (SAXException exception) {
            Exception throwable = new IOException("Error creating XML parser");
            throw (IOException) throwable.initCause(exception);
        } finally {
            client.disconnect();
        }
    }

    /* ====================================================================== */
    /* INTERNAL CLASSES                                                       */
    /* ====================================================================== */

    /**
     * <p>An internal XML {@link DefaultHandler} used to parse out the various
     * details of a PROPFIND response.</p> 
     */
    private static final class Handler extends DefaultHandler {
        
        /* ================================================================== */
        /* PSEUDO-XPATH LOCATIONS FOR QUICK-AND-DIRTY LOCATION LOOKUP         */
        /* ================================================================== */
        private static final String RESPONSE_PATH = "/multistatus/response";
        private static final String HREF_PATH = "/multistatus/response/href";
        private static final String COLLECTION_PATH = 
            "/multistatus/response/propstat/prop/resourcetype/collection";
        private static final String GETCONTENTTYPE_PATH = 
            "/multistatus/response/propstat/prop/getcontenttype";
        private static final String GETLASTMODIFIED_PATH = 
            "/multistatus/response/propstat/prop/getlastmodified";
        private static final String GETCONTENTLENGTH_PATH = 
            "/multistatus/response/propstat/prop/getcontentlength";
        private static final String CREATIONDATE_PATH = 
            "/multistatus/response/propstat/prop/creationdate";

        /** <p>The {@link Location} for resolving all other links.</p> */
        private final Location base;
        /** <p>The {@link List} of all scavenged resources.</p> */
        private final List list = new ArrayList();
        /** <p>The resource currently being processed.</p> */
        private Resource rsrc = null;
        /** <p>A {@link StringBuffer} holding character data.</p> */
        private StringBuffer buff = null;
        /** <p>A {@link Stack} for quick-and-dirty pseudo XPath lookups.</p> */
        private Stack stack = new Stack();

        /**
         * <p>Create a new instance specifying the base {@link Location}.</p>
         */
        private Handler(Location location) {
            this.base = location;
        }

        /**
         * <p>Push an element name in the stack for pseudo-XPath lookups.</p>
         * 
         * @return a {@link String} like <code>/element/element/element</code>.
         */
        private String pushPath(String path) {
            this.stack.push(path.toLowerCase());
            final StringBuffer buffer = new StringBuffer();
            for (int x = 0; x < this.stack.size(); x ++)
                buffer.append('/').append(this.stack.get(x));
            return buffer.toString();
        }

        /**
         * <p>Pop the last element name from the pseudo-XPath lookup stack.</p>
         * 
         * @return a {@link String} like <code>/element/element/element</code>.
         */
        private String popPath(String path)
        throws SAXException {
            final StringBuffer buffer = new StringBuffer();
            final String last = (String) this.stack.pop();
            if (path.toLowerCase().equals(last)) {
                for (int x = 0; x < this.stack.size(); x ++)
                    buffer.append('/').append(this.stack.get(x));
                return buffer.append('/').append(last).toString();
            }
            throw new SAXException("Tag <" + path + "/> unbalanced at path \""
                                   + pushPath(last) + "\"");
        }

        /**
         * <p>Handle the start-of-element SAX event.</p>
         */
        public void startElement(String uri, String l, String q, Attributes a)
        throws SAXException {
            if (! "DAV:".equals(uri.toUpperCase())) return;
            final String path = this.pushPath(l);

            if (RESPONSE_PATH.equals(path)) {
                this.rsrc = new Resource();
    
            } else if (COLLECTION_PATH.equals(path)) {
                if (this.rsrc != null) this.rsrc.collection = true;

            } else if (GETCONTENTTYPE_PATH.equals(path) ||
                       GETLASTMODIFIED_PATH.equals(path) ||
                       GETCONTENTLENGTH_PATH.equals(path) ||
                       CREATIONDATE_PATH.equals(path) ||
                       HREF_PATH.equals(path)) {  
                this.buff = new StringBuffer();
            }
        }

        /**
         * <p>Handle the end-of-element SAX event.</p>
         */
        public void endElement(String uri, String l, String q)
        throws SAXException {
            if (! "DAV:".equals(uri.toUpperCase())) return;
            final String path = this.popPath(l);
            final String data = this.resetBuffer();

            if (RESPONSE_PATH.equals(path)) {
                if (this.rsrc != null) {
                    if (this.rsrc.location != null) {
                        if (this.rsrc.location.isAbsolute()) {
                            final String z = this.rsrc.location.toString();
                            throw new SAXException("Unresolved location " + z);
                        } else {
                            this.list.add(this.rsrc);
                        }
                    } else {
                        throw new SAXException("Null location for resource");
                    }
                }
    
            } else if (HREF_PATH.equals(path)) {
                if (this.rsrc != null) try {
                    final Location resolved = this.base.resolve(data);
                    this.rsrc.location = this.base.relativize(resolved);
                    if (! this.rsrc.location.isRelative())
                        throw new SAXException("Unable to relativize location "
                                               + this.rsrc.location);
                } catch (MalformedURLException exception) {
                    final String msg = "Unable to resolve URL \"" + data + "\"";
                    SAXException throwable = new SAXException(msg, exception);
                    throw (SAXException) throwable.initCause(exception);
                }
    
            } else if (CREATIONDATE_PATH.equals(path)) {
                if (this.rsrc != null)
                    this.rsrc.creationDate = StringTools.parseIsoDate(data);

            } else if (GETCONTENTTYPE_PATH.equals(path)) {
                if (this.rsrc != null) this.rsrc.contentType = data;
    
            } else if (GETLASTMODIFIED_PATH.equals(path)) {
                if (this.rsrc != null)
                    this.rsrc.lastModified = StringTools.parseHttpDate(data);
    
            } else if (GETCONTENTLENGTH_PATH.equals(path)) {
                if (this.rsrc != null) {
                    Long length = StringTools.parseNumber(data);
                    if (length != null) {
                        this.rsrc.contentLength = length.longValue();
                    }
                }
            }
        }
    
        /**
         * <p>Handle SAX characters notification.</p>
         */
        public void characters(char buffer[], int offset, int length) {
            if (this.buff != null) this.buff.append(buffer, offset, length);
        }
        
        /**
         * <p>Reset the current characters buffer and return it as a
         * {@link String}.</p>
         */
        private String resetBuffer() {
            if (this.buff == null) return null;
            if (this.buff.length() == 0) {
                this.buff = null;
                return null;
            }
            final String value = this.buff.toString();
            this.buff = null;
            return value;
        }
    }

    /**
     * <p>A simple class holding the core resource properties.</p>
     */
    private static class Resource {
        private Location location = null;
        private boolean collection = false;
        private long contentLength = -1;
        private String contentType = null;
        private Date lastModified = null;
        private Date creationDate = null;
    }

    /* ====================================================================== */
    /* COMMAND LINE CLIENT                                                    */
    /* ====================================================================== */

    /**
     * <p>A command-line interface to a WebDAV repository.</p>
     * 
     * <p>When invoked from the command line, this class requires one only
     * argument, the URL location of the WebDAV repository to connect to.</p>
     *
     * <p>After connection this method will interact with the user using an
     * extremely simple console-based interface.</p>
     */
    public static void main(String args[])
    throws IOException {
        final InputStreamReader r = new InputStreamReader(System.in);
        final BufferedReader in = new BufferedReader(r);
        WebDavClient client = new WebDavClient(Location.parse(args[0]));

        while (true) try {
            System.out.print("[" + client.getLocation() + "] -> ");
            args = parse(in.readLine());
            if (args == null) break;
            if (args[0].equals("list")) {
                if (args[1] == null) list(client, System.out);
                else list(client.open(args[1]), System.out);

            } else if (args[0].equals("refresh")) {
                client = client.refresh();

            } else if (args[0].equals("get")) {
                if (args[1] != null) {
                    final InputStream input = client.get(args[1]);
                    final File file = new File(args[2]).getCanonicalFile();
                    final OutputStream output = new FileOutputStream(file);
                    final long bytes = StreamTools.copy(input, output);
                    System.out.println("Fetched child \"" + args[1] +
                                       "\" to file \""  + file + "\" (" +
                                       bytes + " bytes)");
                }
                else System.out.print("Can't \"get\" null");

            } else if (args[0].equals("put")) {
                if (args[1] != null) {
                    final File file = new File(args[1]).getCanonicalFile();
                    final InputStream input = new FileInputStream(file);
                    final OutputStream output = client.put(args[2], file.length());
                    final long bytes = StreamTools.copy(input, output);
                    System.out.println("Uploaded file \"" + file +
                                       "\" to child \"" + args[2] + "\" (" +
                                       bytes + " bytes)");
                }
                else System.out.print("Can't \"put\" null");

            } else if (args[0].equals("mkcol")) {
                if (args[1] != null) {
                    client.mkcol(args[1]);
                    System.out.println("Created \"" + args[1] + "\"");
                }
                else System.out.print("Can't \"mkcol\" null");

            } else if (args[0].equals("delete")) {
                if (args[1] != null) {
                    client.delete(args[1]);
                    System.out.println("Deleted \"" + args[1] + "\"");
                }
                else System.out.print("Can't \"delete\" null");

            } else if (args[0].equals("cd")) {
                if (args[1] != null) client = client.open(args[1]);
                else System.out.print("Can't \"cd\" to null");

            } else if (args[0].equals("quit")) {
                break;

            } else {
                System.out.print("Invalid command \"" + args[0] + "\". ");
                System.out.println("Valid commands are:");
                System.out.println(" - \"list\"    list the children child");
                System.out.println(" - \"get\"     fetch the specified child");
                System.out.println(" - \"put\"     put the specified child");
                System.out.println(" - \"mkcol\"   create a collection");
                System.out.println(" - \"delete\"  delete a child");
                System.out.println(" - \"put\"     put the specified resource");
                System.out.println(" - \"cd\"      change the location");
                System.out.println(" - \"refresh\" refresh this location");
                System.out.println(" - \"quit\"    quit this application");
            }
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }
        System.err.println();
    }
    
    /**
     * <p>Parse a line entered by the user returning a three-tokens argument
     * list (command, argument 1, argument 2)</p>
     */
    private static String[] parse(String line) {
        if (line == null) return null;
        final String array[] = new String[3];
        final StringTokenizer tokenizer = new StringTokenizer(line);
        int offset = 0;
        while (tokenizer.hasMoreTokens() && (offset < 3))
            array[offset ++] = tokenizer.nextToken();
        if (array[0] == null) return null;
        if (array[2] == null) array[2] = array[1];
        return array;
    }
    
    /**
     * <p>Pseudo-nicely display a list of the children of a collection</p>
     */
    private static void list(WebDavClient client, PrintStream out)
    throws IOException {
        out.print("C | ");
        out.print("CONTENT TYPE    | ");
        out.print("CREATED             | ");
        out.print("MODIFIED            | ");
        out.print("SIZE       | ");
        out.println("NAME ");
        for (Iterator iterator = client.iterator(); iterator.hasNext() ; ) {
            final StringBuffer buffer = new StringBuffer();
            String child = (String) iterator.next();
            if (client.isCollection(child)) buffer.append("* | ");
            else buffer.append("  | ");
            format(buffer, client.getContentType(child), 15).append(" | ");
            format(buffer, client.getCreationDate(child), 19).append(" | ");
            format(buffer, client.getLastModified(child), 19).append(" | ");
            format(buffer, client.getContentLength(child), 10).append(" | ");
            out.println(buffer.append(child));
        }
    }

    /** <p>Format a number aligning it to the right of a string.</p> */
    private static StringBuffer format(StringBuffer buf, long num, int len) {
        final String data;
        if (num < 0) data = "";
        else data = Long.toString(num);
        final int spaces = len - data.length();
        for (int x = 0; x < spaces; x++) buf.append(' ');
        buf.append(data);
        return buf;
    }

    /** <p>Format a string into an exact number of characters.</p> */
    private static StringBuffer format(StringBuffer buf, Object obj, int len) {
        final String string;
        if (obj == null) {
            string = ("[null]");
        } else if (obj instanceof Date) {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            string = f.format((Date) obj);
        } else {
            string = obj.toString();
        }
        final StringBuffer buffer = new StringBuffer(string);
        for (int x = string.length(); x < len; x ++) buffer.append(' ');
        return buf.append(buffer.substring(0, len));
    }
}
