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
package it.could.util.location;

import it.could.util.StringTools;
import it.could.util.encoding.Encodable;
import it.could.util.encoding.EncodingTools;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>An utility class representing an HTTP-like URL.</p>
 * 
 * <p>This class can be used to represent any URL that roughly uses the HTTP
 * format. Compared to the standard {@link java.net.URL} class, the scheme part
 * of the a {@link Location} is never checked, and it's up to the application
 * to verify its correctness, while compared to the {@link java.net.URI} class,
 * its parsing mechanism is a lot more relaxed (be liberal in what you accept,
 * be strict in what you send).</p>
 * 
 * <p>For a bigger picture on how this class works, this is an easy-to-read
 * representation of what the different parts of a {@link Location} are:</p>
 * 
 * <div align="center">
 *   <a href="url.pdf" target="_new" title="PDF Version">
 *     <img src="url.gif" alt="URL components" border="0">
 *   </a>
 * </div>
 * 
 * <p>One important difference between this implementation and the description
 * of <a href="http://www.ietf.org/rfc/rfc1738.txt">URLs</a> and
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">URIs</a> is that parameter
 * paths are represented <i>only at the end of the entire path structure</i>
 * rather than for each path element. This over-simplification allows easy
 * relativization of {@link Location}s when used with servlet containers, which
 * normally use path parameters to encode the session id.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class Location implements Encodable {

    /** <p>A {@link Map} of schemes and their default port number.</p> */
    private static final Map schemePorts = new HashMap();
    static {
        schemePorts.put("acap",   new Integer( 674));
        schemePorts.put("dav",    new Integer(  80));
        schemePorts.put("ftp",    new Integer(  21)); 
        schemePorts.put("gopher", new Integer(  70));
        schemePorts.put("http",   new Integer(  80));
        schemePorts.put("https",  new Integer( 443));
        schemePorts.put("imap",   new Integer( 143));
        schemePorts.put("ldap",   new Integer( 389));
        schemePorts.put("mailto", new Integer(  25));
        schemePorts.put("news",   new Integer( 119));
        schemePorts.put("nntp",   new Integer( 119));
        schemePorts.put("pop",    new Integer( 110));
        schemePorts.put("rtsp",   new Integer( 554));
        schemePorts.put("sip",    new Integer(5060));
        schemePorts.put("sips",   new Integer(5061));
        schemePorts.put("snmp",   new Integer( 161));
        schemePorts.put("telnet", new Integer(  23));
        schemePorts.put("tftp",   new Integer(  69));
    }

    /** <p>The {@link List} of schemes of this {@link Location}.</p> */
    private final Schemes schemes;
    /** <p>The {@link Authority} of this {@link Location}.</p> */
    private final Authority authority;
    /** <p>The {@link Path} of this {@link Location}.</p> */
    private final Path path;
    /** <p>The {@link Parameters} of this {@link Location}.</p> */
    private final Parameters parameters;
    /** <p>The fragment part of this {@link Location}.</p> */
    private final String fragment;
    /** <p>The string representation of this {@link Location}.</p> */
    private final String string;

    /**
     * <p>Create a new {@link Location} instance.</p> 
     */
    public Location(Schemes schemes, Authority authority, Path path,
                    Parameters parameters, String fragment)
    throws MalformedURLException {
        if ((schemes == null) && (authority != null))
            throw new MalformedURLException("No schemes specified");
        if ((schemes != null) && (authority == null))
            throw new MalformedURLException("No authority specified");
        if (path == null) throw new MalformedURLException("No path specified");

        this.schemes = schemes;
        this.authority = authority;
        this.path = path;
        this.parameters = parameters;
        this.fragment = fragment;
        this.string = EncodingTools.toString(this);
    }

    /* ====================================================================== */
    /* STATIC CONSTRUCTION METHODS                                            */
    /* ====================================================================== */
    
    public static Location parse(String url)
    throws MalformedURLException {
        try {
            return parse(url, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    public static Location parse(String url, String encoding)
    throws MalformedURLException, UnsupportedEncodingException {
        if (url == null) return null;;
        if (encoding == null) encoding = DEFAULT_ENCODING;
        final String components[] = parseComponents(url);
        final Schemes schemes = parseSchemes(components[0], encoding);
        final int port = findPort(schemes, encoding);
        final Authority auth = parseAuthority(components[1], port, encoding);
        final Path path = Path.parse(components[2], encoding);
        final Parameters params = Parameters.parse(components[3], '&', encoding);
        final String fragment = components[4];
        return new Location(schemes, auth, path, params, fragment);
    }

    /* ====================================================================== */
    /* ACCESSOR METHODS                                                       */
    /* ====================================================================== */

    /**
     * <p>Return an unmodifiable {@link Schemes list of all schemes} for this
     * {@link Location} instance or <b>null</b>.</p>
     */
    public Schemes getSchemes() {
        return this.schemes;
    }

    /**
     * <p>Return the {@link Location.Authority Authority} part for this
     * {@link Location} or <b>null</b>.</p>
     */
    public Authority getAuthority() {
        return this.authority;
    }

    /**
     * <p>Return the <b>non-null</b> {@link Path Path} structure
     * associated with this {@link Location} instance.</p> 
     */
    public Path getPath() {
        return this.path;
    }

    /**
     * <p>Return an unmodifiable {@link Parameters list of all parameters}
     * parsed from this {@link Location}'s query string or <b>null</b>.</p>
     */
    public Parameters getParameters() {
        return this.parameters;
    }

    /**
     * <p>Return the fragment of this {@link Location} unencoded.</p>
     */
    public String getFragment() {
        return this.fragment;
    }

    /* ====================================================================== */
    /* OBJECT METHODS                                                         */
    /* ====================================================================== */

    /**
     * <p>Check if the specified {@link Object} is equal to this instance.</p>
     * 
     * <p>The specified {@link Object} must be a <b>non-null</b>
     * {@link Location} instance whose {@link #toString() string value} equals
     * this one's.</p>
     */
    public boolean equals(Object object) {
        if ((object != null) && (object instanceof Location)) {
            return this.string.equals(((Location)object).string);
        } else {
            return false;
        }
    }

    /**
     * <p>Return the hash code value for this {@link Location} instance.</p>
     */
    public int hashCode() {
        return this.string.hashCode();
    }

    /**
     * <p>Return the {@link String} representation of this {@link Location}
     * instance.</p>
     */
    public String toString() {
        return this.string;
    }

    /**
     * <p>Return the {@link String} representation of this {@link Location}
     * instance using the specified character encoding.</p>
     */
    public String toString(String encoding)
    throws UnsupportedEncodingException {
        final StringBuffer buffer = new StringBuffer();
        
        /* Render the schemes */
        if (this.schemes != null)
            buffer.append(this.schemes.toString(encoding)).append("://");

        /* Render the authority part */
        if (this.authority != null)
            buffer.append(this.authority.toString(encoding));

        /* Render the paths */
        buffer.append(this.path.toString(encoding));

        /* Render the query string */
        if (this.parameters != null)
            buffer.append('?').append(this.parameters.toString(encoding));
        
        /* Render the fragment */
        if (this.fragment != null) {
            buffer.append('#');
            buffer.append(EncodingTools.urlEncode(this.fragment, encoding));
        }

        /* Return the string */
        return buffer.toString();
    }

    /* ====================================================================== */
    /* PUBLIC METHODS                                                         */
    /* ====================================================================== */
    
    /**
     * <p>Checks whether this {@link Location} is absolute or not.</p>
     * 
     * <p>This method must not be confused with the similarly named
     * {@link Path#isAbsolute() Path.isAbsolute()} method.
     * This method will check whether the full {@link Location} is absolute (it
     * has a scheme), while the one exposed by the {@link Path Path}
     * class will check if the path is absolute.</p>
     */
    public boolean isAbsolute() {
        return this.schemes != null && this.authority != null;
    }
    
    public boolean isRelative() {
        return ! (this.isAbsolute() || this.path.isAbsolute());
    }

    public boolean isAuthoritative(Location location) {
        if (! this.isAbsolute()) return false;
        if (! location.isAbsolute()) return true;
        return this.schemes.equals(location.schemes) &&
               this.authority.equals(location.authority);
    }

    /* ====================================================================== */
    /* RESOLUTION METHODS                                                     */
    /* ====================================================================== */

    public Location resolve(String url)
    throws MalformedURLException {
        try {
            return this.resolve(parse(url, DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    public Location resolve(String url, String encoding)
    throws MalformedURLException, UnsupportedEncodingException {
        if (encoding == null) encoding = DEFAULT_ENCODING;
        return this.resolve(parse(url, encoding));
    }

    public Location resolve(Location location) {
        if (! this.isAuthoritative(location)) return location;

        /* Schemes are the same */
        final Schemes schemes = this.schemes;

        /* Authority needs to be merged (for username and password) */
        final Authority auth;
        if (location.authority != null) {
            final String username = location.authority.username != null ?
                                    location.authority.username :
                                    this.authority.username;
            final String password = location.authority.password != null ?
                                    location.authority.password :
                                    this.authority.password;
            final String host = location.authority.host;
            final int port = location.authority.port;
            auth = new Authority(username, password, host, port);
        } else {
            auth = this.authority;
        }

        /* Path can be resolved */
        final Path path = this.path.resolve(location.path);

        /* Parametrs and fragment are the ones of the target */
        final Parameters params = location.parameters;
        final String fragment = location.fragment;

        /* Create a new {@link Location} instance */
        try {
            return new Location(schemes, auth, path, params, fragment);
        } catch (MalformedURLException exception) {
            /* Should really never happen */
            Error error = new InternalError("Can't instantiate Location");
            throw (Error) error.initCause(exception);
        }
    }

    /* ====================================================================== */
    /* RELATIVIZATION METHODS                                                 */
    /* ====================================================================== */
    
    public Location relativize(String url)
    throws MalformedURLException {
        try {
            return this.relativize(parse(url, DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    public Location relativize(String url, String encoding)
    throws MalformedURLException, UnsupportedEncodingException {
        if (encoding == null) encoding = DEFAULT_ENCODING;
        return this.relativize(parse(url, encoding));
    }

    public Location relativize(Location location) {
        final Path path;
        if (!location.isAbsolute()) {
            /* Target location is not absolute, its path might */
            path = this.path.relativize(location.path);
        } else {
            if (this.isAuthoritative(location)) {
                /* Target location is not on the same authority, process path */
                path = this.path.relativize(location.path);
            } else {
                /* Not authoritative for a non-relative location, yah! */
                return location;
            }
        }
        try {
            return new Location(null, null, path, location.parameters,
                                location.fragment);
        } catch (MalformedURLException exception) {
            /* Should really never happen */
            Error error = new InternalError("Can't instantiate Location");
            throw (Error) error.initCause(exception);
        }
    }

    /* ====================================================================== */
    /* INTERNAL PARSING ROUTINES                                              */
    /* ====================================================================== */

    /**
     * <p>Return the port number associated with the specified schemes.</p>
     */
    public static int findPort(List schemes, String encoding)
    throws UnsupportedEncodingException {
        if (schemes == null) return -1;
        if (schemes.size() < 1) return -1;
        Integer p = (Integer) schemePorts.get(schemes.get(schemes.size() - 1));
        return p == null ? -1 : p.intValue();
    }

    /**
     * <p>Parse <code>scheme://authority/path?query#fragment</code>.</p>
     *
     * @return an array of five {@link String}s: scheme (0), authority (1),
     *         path (2), query (3) and fragment (4).
     */
    private static String[] parseComponents(String url)
    throws MalformedURLException {
        /* Scheme, easy and simple */
        final String scheme;
        final String afterScheme;
        final int schemeEnd = url.indexOf(":/");
        if (schemeEnd > 0) {
            scheme = url.substring(0, schemeEnd).toLowerCase();
            afterScheme = url.substring(schemeEnd + 2);
        } else if (schemeEnd == 0) {
            throw new MalformedURLException("Missing scheme"); 
        } else {
            scheme = null;
            afterScheme = url;
        }

        /* Authority (can be tricky because it can be emtpy) */
        final String auth;
        final String afterAuth;
        if (scheme == null) {
            // --> /path... or path...
            afterAuth = afterScheme;
            auth = null;
        } else if (afterScheme.length() > 0 && afterScheme.charAt(0) == '/') {
            // --> scheme://...
            final int pathStart = afterScheme.indexOf('/', 1);
            if (pathStart == 1) {
                // --> scheme:///path...
                afterAuth = afterScheme.substring(pathStart);
                auth = null;
            } else if (pathStart > 1) {
                // --> scheme://authority/path...
                afterAuth = afterScheme.substring(pathStart);
                auth = afterScheme.substring(1, pathStart);
            } else {
                // --> scheme://authority (but no slashes for the path)
                final int authEnds = StringTools.findFirst(afterScheme, "?#");
                if (authEnds < 0) {
                    // --> scheme://authority (that's it, return)
                    auth = afterScheme.substring(1);
                    return new String[] { scheme, auth, "/", null, null };
                }
                // --> scheme://authority?... or scheme://authority#...
                auth = afterScheme.substring(1, authEnds);
                afterAuth = "/" + afterScheme.substring(authEnds);
            }
        } else {
            // --> scheme:/path...
            afterAuth = url.substring(schemeEnd + 1);
            auth = null;
        }

        /* Path, can be terminated by '?' or '#' whichever is first */
        final int pathEnds = StringTools.findFirst(afterAuth, "?#");
        if (pathEnds < 0) {
            // --> ...path... (no fragment or query, return now)
            return new String[] { scheme, auth, afterAuth, null, null };
        }

        /* We have either a query, a fragment or both after the path */
        final String path = afterAuth.substring(0, pathEnds);
        final String afterPath = afterAuth.substring(pathEnds + 1);

        /* Query? The query can contain a "#" and has an extra fragment */
        if (afterAuth.charAt(pathEnds) == '?') {
            final int fragmPos = afterPath.indexOf('#');
            if (fragmPos < 0) {
                // --> ...path...?... (no fragment)
                return new String[] { scheme, auth, path, afterPath, null };
            }

            // --> ...path...?...#... (has also a fragment)
            final String query = afterPath.substring(1, fragmPos);
            final String fragm = afterPath.substring(fragmPos + 1);
            return new String[] { scheme, auth, path, query, fragm };
        }

        // --> ...path...#... (a path followed by a fragment but no query)
        return new String[] { scheme, auth, path, null, afterPath };
    }
    
    /**
     * <p>Parse <code>scheme:scheme:scheme...</code>.</p>
     */
    private static Schemes parseSchemes(String scheme, String encoding)
    throws MalformedURLException, UnsupportedEncodingException {
        if (scheme == null) return null;
        final String split[] = StringTools.splitAll(scheme, ':');
        List list = new ArrayList();
        for (int x = 0; x < split.length; x++) {
            if (split[x] == null) continue;
            list.add(EncodingTools.urlDecode(split[x], encoding));
        }
        if (list.size() != 0) return new Schemes(list);
        throw new MalformedURLException("Empty scheme detected");
    }

    /**
     * <p>Parse <code>username:password@hostname:port</code>.</p>
     */
    private static Authority parseAuthority(String auth, int defaultPort,
                                            String encoding)
    throws MalformedURLException, UnsupportedEncodingException {
        if (auth == null) return null;
        final String split[] = StringTools.splitOnce(auth, '@', true);
        final String uinfo[] = StringTools.splitOnce(split[0], ':', false);
        final String hinfo[] = StringTools.splitOnce(split[1], ':', false);
        final int port;

        if ((split[0] != null) && (split[1] == null))
            throw new MalformedURLException("Missing required host info part");
        if ((uinfo[0] == null) && (uinfo[1] != null))
            throw new MalformedURLException("Password specified without user");
        if ((hinfo[0] == null) && (hinfo[1] != null))
            throw new MalformedURLException("Port specified without host");
        try {
            if (hinfo[1] != null) {
                final int parsedPort = Integer.parseInt(hinfo[1]);
                if ((parsedPort < 1) || (parsedPort > 65535)) {
                    final String message = "Invalid port number " + parsedPort;
                    throw new MalformedURLException(message);
                }
                /* If the specified port is the default one, ignore it! */
                if (defaultPort == parsedPort) port = -1;
                else port = parsedPort;
            } else {
                port = -1;
            }
        } catch (NumberFormatException exception) {
            throw new MalformedURLException("Specified port is not a number");
        }
        return new Authority(EncodingTools.urlDecode(uinfo[0], encoding),
                             EncodingTools.urlDecode(uinfo[1], encoding),
                             EncodingTools.urlDecode(hinfo[0], encoding),
                             port);
    }

    /* ====================================================================== */
    /* PUBLIC INNER CLASSES                                                   */
    /* ====================================================================== */
    
    /**
     * <p>The {@link Location.Schemes Schemes} class represents an unmodifiable
     * ordered collection of {@link String} schemes for a {@link Location}.</p>
     * 
     * @author <a href="http://could.it/">Pier Fumagalli</a>
     */
    public static class Schemes extends AbstractList implements Encodable {
        /** <p>All the {@link String} schemes in order.</p> */
        private final String schemes[];
        /** <p>The {@link String} representation of this instance.</p> */
        private final String string;

        /**
         * <p>Create a new {@link Schemes} instance.</p>
         */
        private Schemes(List schemes) {
            final int size = schemes.size();
            this.schemes = (String []) schemes.toArray(new String[size]); 
            this.string = EncodingTools.toString(this);
        }

        /**
         * <p>Return the {@link String} scheme at the specified index.</p> 
         */
        public Object get(int index) {
            return this.schemes[index];
        }

        /**
         * <p>Return the number of {@link String} schemes contained by this
         * {@link Location.Schemes Schemes} instance.</p> 
         */
        public int size() {
            return this.schemes.length;
        }

        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Location.Schemes Schemes} instance.</p>
         */
        public String toString() {
            return this.string;
        }

        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Location.Schemes Schemes} instance using the specified
         * character encoding.</p>
         */
        public String toString(String encoding)
        throws UnsupportedEncodingException {
            final StringBuffer buffer = new StringBuffer();
            for (int x = 0; x < this.schemes.length; x ++) {
                buffer.append(':');
                buffer.append(EncodingTools.urlEncode(this.schemes[x], encoding));
            }
            return buffer.substring(1);
        }

        /**
         * <p>Return the hash code value for this
         * {@link Location.Schemes Schemes} instance.</p>
         */
        public int hashCode() {
            return this.string.hashCode();
        }

        /**
         * <p>Check if the specified {@link Object} is equal to this
         * {@link Location.Schemes Schemes} instance.</p>
         * 
         * <p>The specified {@link Object} is considered equal to this one if
         * it is <b>non-null</b>, it is a {@link Location.Schemes Schemes}
         * instance, and its {@link #toString() string representation} equals
         * this one's.</p>
         */
        public boolean equals(Object object) {
            if ((object != null) && (object instanceof Schemes)) {
                return this.string.equals(((Schemes) object).string);
            } else {
                return false;
            }
        }
    }

    /* ====================================================================== */

    /**
     * <p>The {@link Location.Authority Authority} class represents the autority
     * and user information for a {@link Location}.</p>
     * 
     * @author <a href="http://could.it/">Pier Fumagalli</a>
     */
    public static class Authority implements Encodable {
        /** <p>The username of this instance (decoded).</p> */
        private final String username;
        /** <p>The password of this instance (decoded).</p> */
        private final String password;
        /** <p>The host name of this instance (decoded).</p> */
        private final String host;
        /** <p>The port number of this instance.</p> */
        private final int port;
        /** <p>The encoded host and port representation.</p> */
        private final String hostinfo;
        /** <p>The encoded string representation of this instance.</p> */
        private final String string;

        /**
         * <p>Create a new {@link Location.Authority Authority} instance.</p>
         */
        private Authority(String user, String pass, String host, int port) {
            this.username = user;
            this.password = pass;
            this.host = host;
            this.port = port;
            try {
                this.hostinfo = this.getHostInfo(DEFAULT_ENCODING);
                this.string = this.toString(DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException exception) {
                final String message = "Default encoding \"" + DEFAULT_ENCODING
                                       + "\" not supported by the platform";
                final InternalError error = new InternalError(message);
                throw (InternalError) error.initCause(exception);
            }
        }

        /**
         * <p>Returns the decoded user name.</p> 
         */
        public String getUsername() {
            return this.username;
        }
        
        /**
         * <p>Returns the decoded password.</p> 
         */
        public String getPassword() {
            return this.password;
        }

        /**
         * <p>Returns the &quot;user info&quot; field.</p>
         * 
         * <p>This method will concatenate the username and password using the
         * colon character and return a <b>non-null</b> {@link String} only if
         * both of them are <b>non-null</b>.</p>
         */
        public String getUserInfo() {
            if ((this.username == null) || (this.password == null)) return null; 
            return this.username + ':' + this.password;
        }

        /**
         * <p>Returns the decoded host name.</p> 
         */
        public String getHost() {
            return this.host;
        }

        /**
         * <p>Returns the port number.</p> 
         */
        public int getPort() {
            return this.port;
        }

        /**
         * <p>Returns the host info part of the
         * {@link Location.Authority Authority}.</p>
         * 
         * <p>This is the encoded representation of the
         * {@link #getUsername() user name} optionally follwed by the colon (:)
         * character and the encoded {@link #getPassword() password}.</p>
         */
        public String getHostInfo() {
            return this.hostinfo;
        }

        /**
         * <p>Returns the host info part of the
         * {@link Location.Authority Authority} using the specified character
         * encoding.</p>
         * 
         * <p>This is the encoded representation of the
         * {@link #getUsername() user name} optionally follwed by the colon (:)
         * character and the encoded {@link #getPassword() password}.</p>
         */
        public String getHostInfo(String encoding)
        throws UnsupportedEncodingException {
            final StringBuffer hostinfo = new StringBuffer();
            hostinfo.append(EncodingTools.urlEncode(this.host, encoding));
            if (port >= 0) hostinfo.append(':').append(port);
            return hostinfo.toString();
        }

        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Location.Authority Authority} instance.</p>
         */
        public String toString() {
            return this.string;
        }

        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Location.Authority Authority} instance using the specified
         * character encoding.</p>
         */
        public String toString(String encoding)
        throws UnsupportedEncodingException {
            final StringBuffer buffer;
            if (this.username != null) {
                buffer = new StringBuffer();
                buffer.append(EncodingTools.urlEncode(this.username, encoding));
                if (this.password != null) {
                    buffer.append(':');
                    buffer.append(EncodingTools.urlEncode(this.password, encoding));
                }
            } else {
                buffer = null;
            }

            if (buffer == null) return this.getHostInfo(encoding);
            buffer.append('@').append(this.getHostInfo(encoding));
            return buffer.toString();
        }

        /**
         * <p>Return the hash code value for this
         * {@link Location.Authority Authority} instance.</p>
         */
        public int hashCode() {
            return this.hostinfo.hashCode();
        }

        /**
         * <p>Check if the specified {@link Object} is equal to this
         * {@link Location.Authority Authority} instance.</p>
         * 
         * <p>The specified {@link Object} is considered equal to this one if
         * it is <b>non-null</b>, it is a {@link Location.Authority Authority}
         * instance, and its {@link #getHostInfo() host info} equals
         * this one's.</p>
         */
        public boolean equals(Object object) {
            if ((object != null) && (object instanceof Authority)) {
                return this.hostinfo.equals(((Authority) object).hostinfo);
            } else {
                return false;
            }
        }
    }
}
