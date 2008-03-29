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

import it.could.util.encoding.EncodingTools;
import it.could.util.location.Location;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>A class implementing an extremely simple HTTP 1.0 connector with
 * basic authentication support.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class HttpClient {

    /** <p>The default HTTP method to use.</p> */
    public static final String DEFAULT_METHOD = "GET";

    /* ====================================================================== */

    /** <p>The byte sequence CR LF (the end of the request).</p> */
    private static final byte CRLF[] = { 0x0d, 0x0a };
    /** <p>The byte sequence for " HTTP/1.0\r\n" (the request signature).</p> */
    private static final byte HTTP[] = { 0x20, 0x48, 0x54, 0x54, 0x50, 0x2f,
                                         0x31, 0x2e, 0x30, 0x0d, 0x0a };

    /* ====================================================================== */

    /** <p>The buffer used to parse lines in the response.</p> */
    private final byte buffer[] = new byte[4096]; 
    /** <p>The map of the current request headers.</p> */
    private final Map requestHeaders = new HashMap();
    /** <p>The map of the current response headers.</p> */
    private final Map responseHeaders = new HashMap();

    /* ====================================================================== */

    /** <p>The {@link Location} pointing to the current request.</p> */
    private Location location;
    /** <p>The status of the current request.</p> */
    private Status status = null;
    /** <p>An array of acceptable statuses to verify upon connection.</p> */
    private int acceptable[] = null;

    /* ====================================================================== */

    /** <p>The limited input stream associated with this request.</p> */
    private Input xinput = null;
    /** <p>The limited output stream associated with this request.</p> */
    private Output xoutput = null;
    /** <p>The socket associated with this request.</p> */
    private Socket xsocket = null;

    /* ====================================================================== */

    /**
     * <p>Create a new {@link HttpClient} instance associated with the
     * specified location in string format.</p>
     * 
     * @throws MalformedURLException if the location couldn't be parsed.
     */
    public HttpClient(String location)
    throws MalformedURLException {
        this.location = Location.parse(location);
    }

    /**
     * <p>Create a new {@link HttpClient} instance associated with the
     * specified location in string format.</p>
     * 
     * @throws MalformedURLException if the location couldn't be parsed.
     */
    public HttpClient(String location, String encoding)
    throws MalformedURLException, UnsupportedEncodingException {
        this.location = Location.parse(location, encoding);
    }

    /**
     * <p>Create a new {@link HttpClient} instance associated with the
     * specified {@link Location}.</p>
     */
    public HttpClient(Location location) {
        if (location == null) throw new NullPointerException("Null location");
        if (! location.isAbsolute()) 
            throw new IllegalArgumentException("Relative location supplied");
        if (! "http".equals(location.getSchemes().toString())) {
            throw new IllegalArgumentException("Scheme is not HTTP");
        }
        this.location = location;
    }

    /* ====================================================================== */
    /* CONNECTION VERIFICATION METHODS                                        */
    /* ====================================================================== */

    /**
     * <p>Set an HTTP response status code considered to be acceptable when
     * verifying the connection.</p>
     */
    public HttpClient setAcceptableStatus(int status) {
        return this.setAcceptableStatuses(new int[] { status });
    }

    /**
     * <p>Set an array of HTTP response status codes considered to be acceptable
     * when verifying the connection.</p>
     * 
     * <p>If the array is <b>null</b> status code checking is disabled.</p>
     */
    public HttpClient setAcceptableStatuses(int statuses[]) {
        if (statuses == null) {
            this.acceptable = null;
            return this;
        }
        for (int x = 0; x < statuses.length; x ++) {
            final int status = statuses[x];
            if ((status < 100) || (status > 599))
                throw new IllegalArgumentException("Wrong status " + status);
        }
        this.acceptable = statuses;
        return this;
    }

    /* ====================================================================== */
    /* CONNECTION METHODS                                                     */
    /* ====================================================================== */
    
    /**
     * <p>Connect to the {@link Location} specified at construction using the
     * default method <code>GET</code>.</p>
     * 
     * <p>This is equivalent to {@link #connect(boolean) connect(true)}.</p>
     * 
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient connect()
    throws IOException {
        return this.connect(DEFAULT_METHOD, true, 0);
    }

    /**
     * <p>Connect to the {@link Location} specified at construction using the
     * default method <code>GET</code> allowing for a specified amount of
     * content to be written into the request.</p>
     * 
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient connect(long contentLength)
    throws IOException {
        return this.connect(DEFAULT_METHOD, false, contentLength);
    }

    /**
     * <p>Connect to the {@link Location} specified at construction using the
     * default method <code>GET</code> and optionally following redirects.</p>
     * 
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient connect(boolean followRedirects)
    throws IOException {
        return this.connect(DEFAULT_METHOD, followRedirects, 0);
    }

    /**
     * <p>Connect to the {@link Location} specified at construction with the
     * specified method.</p>
     * 
     * <p>This is equivalent to {@link #connect(String,boolean)
     * connect(method, true)}.</p>
     * 
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient connect(String method)
    throws IOException {
        return this.connect(method, true, 0);
    }

    /**
     * <p>Connect to the {@link Location} specified at construction with the
     * specified method allowing for a specified amount of content to be
     * written into the request.</p>
     * 
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient connect(String method, long contentLength)
    throws IOException {
        return this.connect(method, false, contentLength);
    }

    /**
     * <p>Connect to the {@link Location} specified at construction with the
     * specified method and optionally following redirects.</p>
     * 
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient connect(String method, boolean followRedirects)
    throws IOException {
        return this.connect(method, followRedirects, 0);
    }

    /**
     * <p>Disconnect from the remote endpoint and terminate the request.</p>
     * 
     * <p>Note that request and response headers, the resultin status and
     * acceptable statuses are <b>not</b> cleared by this method.</p>
     * 
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient disconnect()
    throws IOException {
        return this.disconnect(false);
    }

    /**
     * <p>Disconnect from the remote endpoint and terminate the request.</p>
     *
     * @param reset whether to reset all headers, status and acceptable response
     *              status codes or not.
     * @return this {@link HttpClient} instance.
     * @throws IOException if an I/O or a network error occurred.
     */
    public HttpClient disconnect(boolean reset)
    throws IOException {
        final Socket socket = this.xsocket;
        if (socket != null) try {
            /* Make sure that we mark this instance as being closed */ 
            this.xsocket = null;
            
            /* Close the input stream if necessary */
            if (this.xinput != null) {
                if (! this.xinput.closed) this.xinput.close();
                this.xinput = null;
            }

            /* Close the output stream if necessary */
            if (this.xoutput != null) {
                if (! this.xoutput.closed) this.xoutput.close();
                this.xoutput = null;
            }

        } finally {
            /* Ensure that the socket is closed */
            socket.close();
        }

        if (reset) {
            this.requestHeaders.clear();
            this.responseHeaders.clear();
            this.status = null;
            this.acceptable = null;
        }
        return this;
    }

    /* ====================================================================== */
    /* INTERNAL CONNECTION HANDLER                                            */
    /* ====================================================================== */

    /**
     * <p>Internal method actually connecting to the remote HTTP server.</p>
     */
    private HttpClient connect(String method, boolean redirect, long length)
    throws IOException {
        /* Check if (by any chance) we have been connected already */
        if (this.xsocket != null)
            throw new IllegalStateException("Already connected");

        /* Check for both follow redirects and content length */
        if (length < 0) throw new IOException("Negative length");
        if ((length > 0) && redirect)
            throw new InternalError("Can't follow redirects and write request");

        /* Verify any authentication token */
        final String userinfo = this.location.getAuthority().getUserInfo();
        if (userinfo != null) {
            final String encoded = EncodingTools.base64Encode(userinfo);
            this.addRequestHeader("Authorization", "Basic " + encoded);
        }
        
        /* All methods in HTTP are upper case */
        method = method.toUpperCase();

        /* Make sure we close the connection at the end of the request */
        this.addRequestHeader("Connection", "close", false);
        
        /* The content length of the request is forced to be valid */
        this.addRequestHeader("Content-Length", Long.toString(length), false);

        /* Enter in a loop for redirections */
        int redirs = 20;
        while (true) {
            /* If we have been redirected too many times, fail */
            if ((--redirs) < 0) throw new IOException("Too many redirections");
            
            /* Get the authority, once and for all */
            final Location.Authority auth = this.location.getAuthority();

            /* Prepare a normalized host header */
            final String host = auth.getHost();
            final int port = auth.getPort() < 0 ? 80 : auth.getPort();
            this.addRequestHeader("Host", host + ":" + port, false);
    
            /* Connect to the remote endpoint */
            final Socket sock = new Socket(auth.getHost(), port);
            final InputStream in = sock.getInputStream();
            final OutputStream out = sock.getOutputStream();

            /* Write the request line */
            out.write((method + " ").getBytes("US-ASCII"));
            out.write(this.location.getPath().toString().getBytes("US-ASCII"));
            out.write(HTTP); /* SPACE HTTP/1.0 CR LF */

            /* Write all the headers */
            final Iterator headers = this.requestHeaders.values().iterator();
            while (headers.hasNext()) {
                final RequestHeader header = (RequestHeader) headers.next();
                final Iterator values = header.values.iterator();
                while (values.hasNext()) {
                    out.write(header.name);
                    out.write((byte []) values.next());
                }
            }

            /* Write the final CRLF, read the status and the headers */
            out.write(CRLF);
            out.flush();

            /* Return now if we have to write content */
            if (length > 0) {
                this.xsocket = sock; 
                this.xoutput = new Output(this, in, out, length);
                this.xinput = null;
                return this;
            }

            this.readStatusLine(in);
            this.readHeaders(in);

            /* If we have to follow redirects, let's inspect the response */
            final int code = this.status.status;
            if (redirect && ((code == 301) || (code == 302) || (code == 307))) {
                final String location = this.getResponseHeader("Location");
                if (location != null) {
                    in.close();
                    out.close();
                    sock.close();
                    this.location = this.location.resolve(location);
                    continue;
                }
            }

            /* No further redirections, so verify if the status code is ok */
            this.verify();

            /* Evaluate the content length specified by the server */
            final String len = this.getResponseHeader("Content-Length");
            long bytesLength = -1;
            if (len != null) try {
                bytesLength = Long.parseLong(len);
            } catch (NumberFormatException exception) {
                /* Swallow this, be liberal in what we accept */
            }

            /* Return an output stream if the content length was not zero */
            this.xsocket = sock; 
            this.xoutput = null;
            this.xinput = new Input(this, in, bytesLength);
            return this;
        }
    }

    private void verify()
    throws IOException {
        /* No further redirections, sov erify if the status code is ok */
        if (this.acceptable != null) {
            boolean accepted = false;
            for (int x = 0; x < this.acceptable.length; x ++) {
                if (this.status.status != this.acceptable[x]) continue;
                accepted = true;
                break;
            }
            if (! accepted) {
                this.disconnect();
                throw new IOException("Connection to " + this.location +
                                      " returned unacceptable status " +
                                      this.status.status + " (" +
                                      this.status.message + ")");
            }
        }
    }

    /* ====================================================================== */
    /* INPUT / OUTPUT METHODS                                                 */
    /* ====================================================================== */

    /**
     * <p>Return an {@link InputStream} where the content of the HTTP response
     * can be read from.</p>
     * 
     * @throws IllegalStateException if this instance is not connected yet, or
     *                               the request body was not fully written yet.
     */
    public InputStream getResponseStream()
    throws IllegalStateException {
        if (this.xsocket == null)
            throw new IllegalStateException("Connection not available");
        if ((this.xoutput != null) && (this.xoutput.remaining != 0))
            throw new IllegalStateException("Request body not fully written");
        return this.xinput;
    }

    /**
     * <p>Return an {@link OutputStream} where the content of the HTTP request
     * can be written to.</p>
     *
     * @throws IllegalStateException if this instance is not connected yet or if
     *                               upon connection the size of the request was
     *                               not specifed or <b>zero</b>.
     */
    public OutputStream getRequestStream()
    throws IllegalStateException {
        if (this.xsocket == null)
            throw new IllegalStateException("Connection not available");
        if (this.xoutput == null) 
            throw new IllegalStateException("No request body to write to");
        return this.xoutput;
    }

    /* ====================================================================== */
    /* REQUEST AND RESPONSE METHODS                                           */
    /* ====================================================================== */

    /**
     * <p>Return the {@link Location} of this connection.</p>
     * 
     * <p>This might be different from the {@link Location} specified at
     * construction time if upon connecting HTTP redirections were followed.</p>
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * <p>Add a new header that will be sent with the HTTP request.</p>
     * 
     * <p>This method will remove any header value previously associated with
     * the specified name, in other words this method is equivalent to
     * {@link #addRequestHeader(String, String, boolean)
     *  addRequestHeader(name, value, false)}.</p>
     * 
     * @param name the name of the request header to add.
     * @param value the value of the request header to add.
     * @return this {@link HttpClient} instance.
     * @throws NullPointerException the name or value were <b>null</b>.
     */
    public HttpClient addRequestHeader(String name, String value) {
        return this.addRequestHeader(name, value, false);
    }

    /**
     * <p>Add a new header that will be sent with the HTTP request.</p>
     * 
     * @param name the name of the request header to add.
     * @param value the value of the request header to add.
     * @param appendValue if the current value should be appended, or in other
     *                    words, that two headers with the same can coexist. 
     * @return this {@link HttpClient} instance.
     * @throws NullPointerException the name or value were <b>null</b>.
     */
    public HttpClient addRequestHeader(String name, String value,
                                           boolean appendValue) {
        final String key = name.toLowerCase();
        try {
            RequestHeader header;
            if (appendValue) {
                header = (RequestHeader) this.requestHeaders.get(key);
                if (header == null) {
                    header = new RequestHeader(name);
                    this.requestHeaders.put(key, header);
                }
            } else {
                header = new RequestHeader(name);
                this.requestHeaders.put(key, header);
            }
            header.values.add((value + "\r\n").getBytes("ISO-8859-1"));
            return this;
        } catch (UnsupportedEncodingException exception) {
            Error error = new InternalError("Standard encoding not supported");
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
     * <p>Remove the named header from the current HTTP request.</p>
     * 
     * @param name the name of the request header to add.
     * @return this {@link HttpClient} instance.
     * @throws NullPointerException the name was <b>null</b>.
     */
    public HttpClient removeRequestHeader(String name) {
        final String key = name.toLowerCase();
        this.requestHeaders.remove(key);
        return this;
    }

    /**
     * <p>Remove all headers from the current HTTP request.</p>
     * 
     * @return this {@link HttpClient} instance.
     */
    public HttpClient removeRequestHeaders() {
        this.requestHeaders.clear();
        return this;
    }

    /**
     * <p>Return the first value for the specified response header.</p>
     *
     * @param name the name of the header whose value needs to be returned.
     * @return a {@link String} or <b>null</b> if no such header exists.
     */
    public String getResponseHeader(String name) {
        final String key = name.toLowerCase();
        ResponseHeader header = (ResponseHeader) this.responseHeaders.get(key);
        if (header == null) return null;
        return (String) header.values.get(0);
    }

    /**
     * <p>Return all the values for the specified response header.</p>
     *
     * @param name the name of the header whose values needs to be returned.
     * @return a {@link List} or <b>null</b> if no such header exists.
     */
    public List getResponseHeaderValues(String name) {
        final String key = name.toLowerCase();
        ResponseHeader header = (ResponseHeader) this.responseHeaders.get(key);
        if (header == null) return null;
        return Collections.unmodifiableList(header.values);
    }

    /**
     * <p>Return an {@link Iterator} over all response header names.</p>
     *
     * @return a <b>non-null</b> {@link Iterator}.
     */
    public Iterator getResponseHeaderNames() {
        final Iterator iterator = this.responseHeaders.values().iterator();
        return new Iterator() {
            public boolean hasNext() {
                return iterator.hasNext();
            }
            public Object next() {
                return ((ResponseHeader) iterator.next()).name;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * <p>Return the protocol returned by the remote HTTP server.</p>
     *
     * @return a <b>non-null</b> {@link String} like <code>HTTP/1.0</code>.
     * @throws IllegalStateException if the connection was never connected. 
     */
    public String getResponseProtocol() {
        if (this.status == null) throw new IllegalStateException();
        return this.status.protocol;
    }

    /**
     * <p>Return the status returned by the remote HTTP server.</p>
     *
     * @return a number representing the HTTP status of the response.
     * @throws IllegalStateException if the connection was never connected. 
     */
    public int getResponseStatus() {
        if (this.status == null) throw new IllegalStateException();
        return this.status.status;
    }

    /**
     * <p>Return the status message returned by the remote HTTP server.</p>
     *
     * @return a <b>non-null</b> {@link String} like <code>OK</code>.
     * @throws IllegalStateException if the connection was never connected. 
     */
    public String getResponseMessage() {
        if (this.status == null) throw new IllegalStateException();
        return this.status.message;
    }

    /* ====================================================================== */
    /* PRIVATE METHODS TO USE WHEN CONNECTING                                 */
    /* ====================================================================== */

    /**
     * <p>Read a single line of the HTTP response from the specified
     * {@link InputStream} into a byte array (trailing CRLF are removed).</p>
     */
    private byte[] readLine(InputStream input)
    throws IOException {
        int x = 0;
        while (true) {
            int b = input.read();
            if (b == -1) break;
            if (b == 0x0A) break;
            if (x == this.buffer.length) break;
            this.buffer[x ++] = (byte) b;
        }
        if ((x > 0) && (this.buffer[x - 1] == 0x0D)) x--;
        final byte array[] = new byte[x];
        System.arraycopy(this.buffer, 0, array, 0, x);
        return array;
    }

    /**
     * <p>Read the status line from the specified {@link InputStream} and
     * setup the {@link #status} field.</p>
     */
    private void readStatusLine(InputStream input)
    throws IOException {
        /* Prepare the different buffers required for parsing */
        final byte line[] = this.readLine(input);
        final byte buff[] = new byte[line.length];
        final String comp[] = new String[3];
        int lpos = 0;
        int bpos = 0;
        int cpos = 0;
        boolean spc = true;

        /* Iterate every single byte in the line, splitting up components */
        while (lpos < line.length) {
            final byte b = line[lpos ++];
            if (spc) {
                if ((b == 0x09) || (b == 0x20)) continue;
                buff[bpos ++] = b;
                if (cpos == 2) break;
                else spc = false;
            } else {
                if ((b == 0x09) || (b == 0x20)) {
                    comp[cpos ++] = new String(buff, 0, bpos, "US-ASCII");
                    bpos = 0;
                    spc = true;
                    continue;
                }
                buff[bpos ++] = b;
            }
            
        }
        /*
         * Copy remaining bytes out of the line buffer and ensure all
         * components in the status line are not null;
         */
        while (lpos < line.length) buff[bpos ++] = line[lpos++];
        if (bpos > 0) comp[cpos++] = new String(buff, 0, bpos, "US-ASCII");
        for (int x = cpos; x < 3; x++) comp[x] = "";

        /* Create the status object */
        this.status = new Status(comp[0], comp[1], comp[2]);
    }

    /**
     * <p>Read all the response headers from the specified {@link InputStream}
     * and setup the {@link #responseHeaders} field.</p>
     */
    private void readHeaders(InputStream input)
    throws IOException {
        /* Clear out any previous header */
        this.responseHeaders.clear();

        /* Process the input stream until we find an empty line */
        while (true) {
            final byte array[] = this.readLine(input);
            if (array.length == 0) break;

            /* Identify where the colon is in the header */
            int pos = -1;
            while (pos < array.length) if (array[++ pos] == 0x03A) break;
            if (pos == 0) continue;
            if (pos == array.length - 1) continue;

            /* Prepare strings for name and value */
            final int o = pos + 1;
            final int l = array.length - o;
            final String name = new String(array, 0, pos, "US-ASCII").trim();
            final String value = new String(array, o, l, "ISO-8859-1").trim();
            if ((name.length() == 0) || (value.length() == 0)) continue;

            /* Store the header value in a list for now */
            final String key = name.toLowerCase();
            ResponseHeader hdr = (ResponseHeader) this.responseHeaders.get(key);
            if (hdr == null) {
                hdr = new ResponseHeader(name);
                this.responseHeaders.put(key, hdr);
            }
            hdr.values.add(value);
        }
    }

    /* ====================================================================== */
    /* INTERNAL CLASS REPRESENTNG THE STATUS LINE AND AN ENCODED HEADER       */
    /* ====================================================================== */

    /**
     * <p>A simple internal class representing a response status line.</p>
     */
    private static final class Status {

        /** <p>The response protocol, like <code>HTTP/1.0</code> */
        private final String protocol;
        /** <p>The response status code, like <code>302</code> */
        private final int status;
        /** <p>The response message, like <code>Moved permanently</code> */
        private final String message;

        /**
         * <p>Create a new {@link Status} verifying the supplied parameters.</p>
         * 
         * @throws IOException if an error occurred verifying the parameters.
         */
        private Status(String protocol, String status, String message)
        throws IOException {

            /* Verify the protocol */
            if ("HTTP/1.0".equals(protocol) || "HTTP/1.1".equals(protocol)) {
                this.protocol = protocol; 
            } else {
                throw new IOException("Unknown protocol \"" + protocol + "\"");
            }

            /* Verify the status */
            try {
                this.status = Integer.parseInt(status);
                if ((this.status < 100) || (this.status > 599)) {
                    throw new IOException("Invalid status \"" + status + "\"");
                }
            } catch (RuntimeException exception) {
                final String error = "Can't parse status \"" + status + "\"";
                IOException throwable = new IOException(error);
                throw (IOException) throwable.initCause(exception);
            }
            
            /* Decode the message */
            if ("".equals(message)) this.message = "No message";
            else this.message = EncodingTools.urlDecode(message, "ISO-8859-1");
        }
    }

    /**
     * <p>A simple internal class representing a request header.</p>
     */
    private static final class RequestHeader {

        /** <p>The byte array of the header's name.</p> */
        private final byte name[];
        /** <p>A {@link List} of all the header's values.</p> */
        private final List values;

        /** <p>Create a new {@link RequestHeader} instance.</p> */
        private RequestHeader(String name)
        throws UnsupportedEncodingException {
            this.name = (name + ": ").getBytes("US-ASCII");
            this.values = new ArrayList();
        }
    }

    /**
     * <p>A simple internal class representing a response header.</p>
     */
    private static final class ResponseHeader {

        /** <p>The real name of the response header.</p> */
        private final String name;
        /** <p>A {@link List} of all the header's values.</p> */
        private final List values;

        /** <p>Create a new {@link ResponseHeader} instance.</p> */
        private ResponseHeader(String name)
        throws UnsupportedEncodingException {
            this.name = name;
            this.values = new ArrayList();
        }
    }

    /* ====================================================================== */
    /* LIMITED STREAMS                                                        */
    /* ====================================================================== */

    /**
     * <p>A simple {@link OutputStream} writing at most the number of bytes
     * specified at construction.</p>
     */
    private static final class Output extends OutputStream {

        /** <p>The {@link OutputStream} wrapped by this instance.</p> */
        private final OutputStream output;
        /** <p>The {@link InputStream} wrapped by this instance.</p> */
        private final InputStream input;
        /** <p>The {@link HttpClient} wrapped by this instance.</p> */
        private final HttpClient client;
        /** <p>The number of bytes yet to write.</p> */
        private long remaining;
        /** <p>A flag indicating whether this instance was closed.</p> */
        private boolean closed;

        /**
         * <p>Create a new {@link Output} instance with the specified limit
         * of bytes to write.</p>
         * 
         * @param output the {@link OutputStream} to wrap.
         * @param remainig the maximum number of bytes to write.
         */
        private Output(HttpClient client, InputStream input,
                       OutputStream output, long remaining) {
            if (input == null) throw new NullPointerException();
            if (output == null) throw new NullPointerException();
            if (client == null) throw new NullPointerException();
            this.remaining = remaining;
            this.client = client;
            this.output = output;
            this.input = input;
        }

        public void write(byte buf[])
        throws IOException {
            this.write(buf, 0, buf.length);
        }

        public void write(byte buf[], int off, int len)
        throws IOException {
            if (len > this.remaining) {
                throw new IOException("Too much data to write");
            } else try {
                this.output.write(buf, off, len);
            } finally {
                this.remaining -= len;
                if (this.remaining < 1) this.close();
            }
        }

        public void write(int b)
        throws IOException {
            if (this.remaining < 1) {
                throw new IOException("Too much data to write");
            } else try {
                this.output.write(b);
            } finally {
                this.remaining -= 1;
                if (this.remaining < 1) this.close();
            }
        }

        public void flush()
        throws IOException {
            this.output.flush();
        }

        public void close()
        throws IOException {
            if (this.closed) return;
            if (this.remaining > 0)
                throw new IOException(this.remaining + " bytes left to write");
            this.closed = true;
            this.output.flush();

            /* Read the status and headers from the connection and verify */ 
            this.client.readStatusLine(this.input);
            this.client.readHeaders(this.input);
            this.client.verify();

            /* Evaluate the content length specified by the server */
            final String slen = this.client.getResponseHeader("Content-Length");
            long blen = -1;
            if (slen != null) try {
                blen = Long.parseLong(slen);
            } catch (NumberFormatException exception) {
                /* Swallow this, be liberal in what we accept */
            }

            /* Return an output stream if the content length was not zero */
            this.client.xoutput = null;
            this.client.xinput = new Input(this.client, this.input, blen);
        }

        protected void finalize()
        throws Throwable {
            try {
                this.close();
            } finally {
                super.finalize();                
            }
        }
    }

    /**
     * <p>A simple {@link InputStream} reading at most the number of bytes
     * specified at construction.</p>
     */
    private static final class Input extends InputStream {

        /** <p>The {@link InputStream} wrapped by this instance.</p> */
        private final InputStream input;
        /** <p>The {@link HttpClient} wrapped by this instance.</p> */
        private final HttpClient client;
        /** <p>The number of bytes yet to write or -1 if unknown.</p> */
        private long remaining;
        /** <p>A flag indicating whether this instance was closed.</p> */
        private boolean closed;
        
        /**
         * <p>Create a new {@link Input} instance with the specified limit
         * of bytes to read.</p>
         * 
         * @param input the {@link InputStream} to wrap.
         * @param remainig the maximum number of bytes to read or -1 if unknown.
         */
        private Input(HttpClient client, InputStream input, long remaining) {
            if (input == null) throw new NullPointerException();
            if (client == null) throw new NullPointerException();
            this.remaining = remaining < 0 ? Long.MAX_VALUE : remaining;
            this.client = client;
            this.input = input;
        }

        public int read()
        throws IOException {
            if (this.remaining < 1) {
                return -1;
            } else try {
                return this.input.read();
            } finally {
                this.remaining -= 1;
                if (this.remaining < 1) this.close();
            }
        }

        public int read(byte buf[])
        throws IOException {
            return read(buf, 0, buf.length);
        }

        public int read(byte buf[], int off, int len)
        throws IOException {
            if (this.remaining <= 0) return -1;
            if (len > this.remaining) len = (int) this.remaining;
            int count = 0;
            try {
                count = this.input.read(buf, off, len);
            } finally {
                this.remaining -= count;
                if (this.remaining < 1) this.close();
            }
            return count;
        }

        public long skip(long n)
        throws IOException {
            if (this.remaining <= 0) return -1;
            if (n > this.remaining) n = this.remaining;
            long count = 0;
            try {
                count = this.input.skip(n);
            } finally {
                this.remaining -= count;
                if (this.remaining < 1) this.close();
            }
            return count;
        }

        public int available()
        throws IOException {
            int count = this.input.available();
            if (count < this.remaining) return count;
            return (int) this.remaining;
        }

        public void close()
        throws IOException {
            if (this.closed) return;
            this.closed = true;
            try {
                this.input.close();
            } finally {
                this.client.disconnect();
            }
        }

        public void mark(int readlimit) {
            this.input.mark(readlimit);
        }

        public void reset()
        throws IOException {
            this.input.reset();
        }

        public boolean markSupported() {
            return this.input.markSupported();
        }

        protected void finalize()
        throws Throwable {
            try {
                this.close();
            } finally {
                super.finalize();                
            }
        }
    }

    /* ====================================================================== */
    /* UTILITY FETCHER                                                        */
    /* ====================================================================== */
    
    /**
     * <p><b>Utility method:</b> fetch the location specified on the command
     * line following redirects if necessary.</p>
     * 
     * <p>The final location fetched (in case of redirections it might change)
     * will be reported on the {@link System#err system error stream} alongside
     * with any errors encountered while processing.</p>
     */
    public static final void main(String args[]) {
        try {
            final HttpClient c = new HttpClient(args[0]).connect();
            final InputStream i = c.getResponseStream();
            for (int b = i.read(); b >= 0; b = i.read()) System.out.write(b);
            c.disconnect();
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        }
    }
}
