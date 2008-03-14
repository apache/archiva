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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;


/**
 * <p>A simple wrapper isolating the Java Servlet API from this
 * <a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * implementation.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVTransaction {

    /**
     * <p>The identifyication of the <code>infinity</code> value
     * in the <code>Depth</code> header.</p>
     */
    public static final int INFINITY = Integer.MAX_VALUE;

    /** <p>The nested {@link HttpServletRequest}.</p> */
    private HttpServletRequest request = null;
    /** <p>The nested {@link HttpServletResponse}.</p> */
    private HttpServletResponse response = null;
    /** <p>The {@link URI} associated with the base of the repository.</p> */
    private URI base = null;
    /** <p>The status for the HTTP response.</p> */
    private int status = -1;

    /* ====================================================================== */
    /* Constructors                                                           */
    /* ====================================================================== */
    
    /**
     * <p>Create a new {@link DAVTransaction} instance.</p>
     */
    public DAVTransaction(ServletRequest request, ServletResponse response)
    throws ServletException {
        if (request == null) throw new NullPointerException("Null request");
        if (response == null) throw new NullPointerException("Null response");
        this.request = (HttpServletRequest) request;
        this.response = (HttpServletResponse) response;
        this.response.setHeader("DAV", "1");
        this.response.setHeader("MS-Author-Via", "DAV");

        try {
            String scheme = this.request.getScheme();
            String host = this.request.getServerName();
            String path = this.request.getContextPath() + 
                          this.request.getServletPath();
            int port = this.request.getServerPort();
            if (! path.endsWith("/")) path += "/";
            this.base = new URI(scheme, null, host, port, path, null, null);
            this.base = this.base.normalize();
        } catch (URISyntaxException exception) {
            throw new ServletException("Unable to create base URI", exception);
        }
    }

    /* ====================================================================== */
    /* Request methods                                                        */
    /* ====================================================================== */

    /**
     * <p>Return the path originally requested by the client.</p>
     */
    public String getMethod() {
        return this.request.getMethod();
    }

    /**
     * <p>Return the path originally requested by the client.</p>
     */
    public String getOriginalPath() {
        String path = this.request.getPathInfo();
        if (path == null) return "";
        if ((path.length() > 1) && (path.charAt(0) == '/')) {
            return path.substring(1);
        } else {
            return path;
        }
    }

    /**
     * <p>Return the path originally requested by the client.</p>
     */
    public String getNormalizedPath() {
        final String path = this.getOriginalPath();
        if (! path.endsWith("/")) return path;
        return path.substring(0, path.length() - 1);
    }

    /**
     * <p>Return the depth requested by the client for this transaction.</p>
     */
    public int getDepth() {
        String depth = request.getHeader("Depth");
        if (depth == null) return INFINITY;
        if ("infinity".equalsIgnoreCase(depth)) return INFINITY;
        try {
            return Integer.parseInt(depth);
        } catch (NumberFormatException exception) {
            throw new DAVException(412, "Unable to parse depth", exception);
        }
    }

    /**
     * <p>Return a {@link URI} 
     */
    public URI getDestination() {
        String destination = this.request.getHeader("Destination");
        if (destination != null) try {
            return this.base.relativize(new URI(destination));
        } catch (URISyntaxException exception) {
            throw new DAVException(412, "Can't parse destination", exception);
        }
        return null;
    }

    /**
     * <p>Return the overwrite flag requested by the client for this
     * transaction.</p>
     */
    public boolean getOverwrite() {
        String overwrite = request.getHeader("Overwrite");
        if (overwrite == null) return true;
        if ("T".equals(overwrite)) return true;
        if ("F".equals(overwrite)) return false;
        throw new DAVException(412, "Unable to parse overwrite flag");
    }

    /**
     * <p>Check if the client requested a date-based conditional operation.</p>
     */
    public Date getIfModifiedSince() {
        String name = "If-Modified-Since";
        if (this.request.getHeader(name) == null) return null;
        return new Date(this.request.getDateHeader(name));
    }

    /* ====================================================================== */
    /* Response methods                                                       */
    /* ====================================================================== */
    
    /**
     * <p>Set the HTTP status code of the response.</p>
     */
    public void setStatus(int status) {
        this.response.setStatus(status);
        this.status = status;
    }

    /**
     * <p>Set the HTTP status code of the response.</p>
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * <p>Set the HTTP <code>Content-Type</code> header.</p>
     */
    public void setContentType(String type) {
        this.response.setContentType(type);
    }

    /**
     * <p>Set an HTTP header in the response.</p>
     */
    public void setHeader(String name, String value) {
        this.response.setHeader(name, value);
    }

    /* ====================================================================== */
    /* I/O methods                                                            */
    /* ====================================================================== */
    
    /**
     * <p>Check if there is a body in the request.</p>
     * 
     * <p>This method differs from checking if the return value of the
     * {@link #read()} method is not <b>null</b> as a request body of length
     * zero will return <b>false</b> in this case, while in the {@link #read()}
     * method will return an empty {@link InputStream}.</p>
     */
    public boolean hasRequestBody()
    throws IOException {
        /* We don't support ranges */
        if (request.getHeader("Content-Range") != null)
            throw new DAVException(501, "Content-Range not supported");

        if (this.request.getContentLength() > 0) return true;
        String len = this.request.getHeader("Content-Length");
        if (len != null) try {
            return (Long.parseLong(len) > 0);
        } catch (NumberFormatException exception) {
            throw new DAVException(411, "Invalid Content-Length specified");
        }
        return false;
    }

    /**
     * <p>Read from the body of the original request.</p>
     */
    public InputStream read()
    throws IOException {
        /* We don't support ranges */
        if (request.getHeader("Content-Range") != null)
            throw new DAVException(501, "Content-Range not supported");

        if (this.request.getContentLength() >= 0) {
            return this.request.getInputStream();
        }

        String len = this.request.getHeader("Content-Length");
        if (len != null) try {
            if (Long.parseLong(len) >= 0) return this.request.getInputStream();
        } catch (NumberFormatException exception) {
            throw new DAVException(411, "Invalid Content-Length specified");
        }
        return null;
    }

    /**
     * <p>Write the body of the response.</p>
     */
    public OutputStream write()
    throws IOException {
        return this.response.getOutputStream();
    }

    /**
     * <p>Write the body of the response.</p>
     */
    public PrintWriter write(String encoding)
    throws IOException {
        return new PrintWriter(new OutputStreamWriter(this.write(), encoding));
    }

    /* ====================================================================== */
    /* Lookup methods                                                         */
    /* ====================================================================== */
    
    /**
     * <p>Look up the final URI of a {@link DAVResource} as visible from the
     * HTTP client requesting this transaction.</p>
     */
    public URI lookup(DAVResource resource) {
        URI uri = resource.getRelativeURI();
        return this.base.resolve(uri).normalize();
    }
}
