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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;


/**
 * <p>The {@link Path Path} class is an ordered collection of
 * {@link Path.Element Element} instances representing a path
 * structure.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class Path extends AbstractList implements Encodable {

    /** <p>The array of {@link Path.Element Element}s.</p> */ 
    private final Element paths[];
    /** <p>The current {@link Parameters} instance or <b>null</b>.</p> */
    private final Parameters parameters;
    /** <p>A flag indicating whether this path is absolute or not.</p> */
    private final boolean absolute;
    /** <p>A flag indicating if this path is a collection or not.</p> */
    private final boolean collection;
    /** <p>The {@link String} representation of this (encoded).</p> */
    private final String string;

    /**
     * <p>Create a new {@link Path Path} instance.</p>
     * 
     * @throws ClassCastException if any of the elements in the {@link List}
     *                            was not a {@link Path.Element Element}.
     */
    public Path(List elements, boolean absolute, boolean collection) {
        this(elements, absolute, collection, null);
    }

    /**
     * <p>Create a new {@link Path Path} instance.</p>
     * 
     * @throws ClassCastException if any of the elements in the {@link List}
     *                            was not a {@link Path.Element Element}.
     */
    public Path(List elements, boolean absolute, boolean collection,
                Parameters parameters) {
        final Stack resolved = resolve(null, absolute, elements);
        final Element array[] = new Element[resolved.size()];
        this.paths = (Element []) resolved.toArray(array);
        this.parameters = parameters;
        this.absolute = absolute;
        this.collection = collection;
        this.string = EncodingTools.toString(this);
    }

    /* ====================================================================== */
    /* STATIC CONSTRUCTION METHODS                                            */
    /* ====================================================================== */

    /**
     * <p>Parse the specified {@link String} into a {@link Path} structure.</p>
     */
    public static Path parse(String path) {
        try {
            return parse(path, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
     * <p>Parse the specified {@link String} into a {@link Path} structure.</p>
     */
    public static Path parse(String path, String encoding)
    throws UnsupportedEncodingException {
        final List params = new ArrayList();
        final List elems = new ArrayList();
        
        /* No path, flog it! */
        if ((path == null) || (path.length() == 0)) {
            return new Path(elems, false, false, null);
        }
        
        /* Check for a proper encoding */
        if (encoding == null) encoding = DEFAULT_ENCODING;

        /* Split up the path structure into its path element components */
        final String split[] = StringTools.splitAll(path, '/');

        /* Check if this path is an absolute path */
        final boolean absolute = path.charAt(0) == '/';

        /* Check every single path element and append it to the current one */
        Element element = null;
        for (int x = 0; x < split.length; x++) {
            if (split[x] == null) continue; /* Collapse double slashes */
            element = parsePath(split[x], params, encoding); 
            if (element != null) elems.add(element);
        }

        /* Check if this is a collection */
        final boolean collection =  ((split[split.length - 1] == null)
                                    || (element == null)
                                    || element.getName().equals(".")
                                    || element.getName().equals(".."));

        /* Setup the last path in our chain and return the first one */
        final Parameters parameters = Parameters.create(params, ';');
        return new Path(elems, absolute, collection, parameters);
    }

    /* ====================================================================== */

    /**
     * <p>Parse a single path element like <code>path!extra;param</code>.</p>
     */
    private static Element parsePath(String path, List parameters,
                                     String encoding)
    throws UnsupportedEncodingException {
        final int pathEnds = StringTools.findFirst(path, "!;");
        final Element element;

        if (pathEnds < 0) {
            element = new Element(EncodingTools.urlDecode(path, encoding), null);
        } else if (path.charAt(pathEnds) == ';') {
            // --> pathname;pathparameter
            final String name = path.substring(0, pathEnds);
            final String param = path.substring(pathEnds + 1);
            final Parameters params = Parameters.parse(param, ';', encoding);
            if (params != null) parameters.addAll(params);
            element = new Element(EncodingTools.urlDecode(name, encoding), null);
        } else {
            // --> pathname!extra...
            final String name = path.substring(0, pathEnds);
            final String more = path.substring(pathEnds + 1);
            final String split[] = StringTools.splitOnce(more, ';', false);
            final Parameters params = Parameters.parse(split[1], ';', encoding);
            if (params != null) parameters.addAll(params);
            element = new Element(EncodingTools.urlDecode(name, encoding),
                                  EncodingTools.urlDecode(split[0], encoding));
        }
        if (element.toString().length() == 0) return null;
        return element;
    }

    /* ====================================================================== */
    /* RESOLUTION METHODS                                                     */
    /* ====================================================================== */

    /**
     * <p>Resolve the specified {@link Path} against this one.</p>
     */
    public Path resolve(Path path) {
        /* Merge the parameters */
        final List params = new ArrayList();
        if (this.parameters != null) params.addAll(this.parameters);
        if (path.parameters != null) params.addAll(path.parameters);
        final Parameters parameters = Parameters.create(params, ';');

        /* No path, return this instance */
        if (path == null) return this;

        /* If the target is absolute, only merge the parameters */ 
        if (path.absolute)
            return new Path(path, true, path.collection, parameters);

        /* Resolve the path */
        final Stack source = new Stack();
        source.addAll(this);
        if (! this.collection && (source.size() > 0)) source.pop();
        final List resolved = resolve(source, this.absolute, path);

        /* Figure out if the resolved path is a collection and return it */
        final boolean c = path.size() == 0 ? this.collection : path.collection;
        return new Path(resolved, this.absolute, c, parameters);
    }

    /**
     * <p>Parse the specified {@link String} into a {@link Path} and resolve it
     * against this one.</p>
     */
    public Path resolve(String path) {
        try {
            return this.resolve(parse(path, DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
     * <p>Parse the specified {@link String} into a {@link Path} and resolve it
     * against this one.</p>
     * 
     * @throws NullPointerException if the path {@link String} was <b>null</b>.
     */
    public Path resolve(String path, String encoding)
    throws UnsupportedEncodingException {
        if (encoding == null) encoding = DEFAULT_ENCODING;
        if (path == null) return this;
        return this.resolve(parse(path, encoding));
    }

    /* ====================================================================== */
    
    private static Stack resolve(Stack stack, boolean absolute, List elements) {
        /* If we have no source stack we create a new empty one */
        if (stack == null) stack = new Stack();
        /* A flag indicating whether we are at the "root" path element. */
        boolean atroot = absolute && stack.empty();
        /* Iterate through the current path elements to see what to do. */
        for (Iterator iter = elements.iterator(); iter.hasNext(); ) {
            final Element element = (Element) iter.next();
            /* If this is the "." (current) path element, skip it. */
            if (".".equals(element.getName())) continue;
            /* If this is the ".." (parent) path element, it gets nasty. */
            if ("..".equals(element.getName())) {
                /* The root path's parent is always itself */
                if (atroot) continue;
                /* We're not at root and have the stack, relative ".." */
                if (stack.size() == 0) {
                    stack.push(element);
                /* We're not at root, but we have stuff in the stack */
                } else {
                    /* Get the last element in the stack */
                    final Element prev = (Element) stack.peek();
                    /* If the last element is "..", add another one */
                    if ("..".equals(prev.getName())) stack.push(element);
                    /* The last element was not "..", pop it out */
                    else stack.pop();
                    /* If absoulte and stack is empty, we're at root */
                    if (absolute) atroot = stack.size() == 0;
                }
            } else {
                /* Normal element processing follows... */
                stack.push(element);
                atroot = false;
            }
        }
        return stack;
    }

    /* ====================================================================== */
    /* RELATIVIZATION METHODS                                                 */
    /* ====================================================================== */
    
    /**
     * <p>Parse the specified {@link String} into a {@link Path} and relativize
     * it against this one.</p>
     */
    public Path relativize(String path) {
        try {
            return this.relativize(parse(path, DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
     * <p>Parse the specified {@link String} into a {@link Path} and relativize
     * it against this one.</p>
     */
    public Path relativize(String path, String encoding)
    throws UnsupportedEncodingException {
        if (encoding == null) encoding = DEFAULT_ENCODING;
        return this.relativize(parse(path, encoding));
    }

    /**
     * <p>Retrieve the relativization path from this {@link Path} to the
     * specified {@link Path}.</p>
     */
    public Path relativize(Path path) {
        /* No matter what, always return the aggregate of all parameters */
        final List parameters = new ArrayList();
        if (this.parameters != null) parameters.addAll(this.parameters);
        if (path.parameters != null) parameters.addAll(path.parameters);
        final Parameters params = Parameters.create(parameters, ';');

        /* We are absolute and the specified path is absolute, we process */
        if ((path.absolute) && (this.absolute)) {
            /* Find the max number of paths we should examine */
            final int num  = this.collection ? this.size() : this.size() - 1;

            /* Process the two absolute paths to check common elements */
            int skip = 0;
            for (int x = 0; (x < num) && (x < path.size()); x ++) {
                if (path.paths[x].equals(this.paths[x])) skip ++;
                else break;
            }

            /* Figure out if the resulting path is a collection */
            final boolean collection;
            if (path.size() > skip) collection = path.collection;
            else if (this.size() > skip) collection = true;
            else collection = this.collection;

            /* Recreate the path to return by adding ".." and the paths */
            final List elems = new ArrayList();
            for (int x = skip; x < num; x ++) elems.add(new Element("..", null));
            elems.addAll(path.subList(skip, path.size()));
            return new Path(elems, false, collection);
        }

        /*
         * Here we are in one of the following cases:
         * - the specified path is already relative, so why bother?
         * - we are relative and the specified path is absolute: in this case
         *   we can't possibly know how far away we are located from the root
         *   so, we only have one option, to return the absolute path.
         * In all cases, though, before returning the specified path, we just
         * merge ours and the path's parameters. 
         */
        if (this.absolute && (! path.absolute)) {
            /*
             * Ok, let's bother, we're absolute and the specified is not. This
             * means that if we resolve the path, we can find another absolute
             * path, and therefore we can do a better job at relativizin it.
             */
            return this.relativize(this.resolve(path));
        }
        /* We'll never going to be able to do better than this */
        return new Path(path, path.absolute, path.collection, params);
    }

    /* ====================================================================== */
    /* PUBLIC EXPOSED METHODS                                                 */
    /* ====================================================================== */

    /**
     * <p>Return the {@link Path.Element Element} instance at
     * the specified index.</p>
     */
    public Object get(int index) {
        return this.paths[index];
    }

    /**
     * <p>Return the number of {@link Path.Element Element}
     * instances contained by this instance.</p>
     */
    public int size() {
        return this.paths.length;
    }

    /**
     * <p>Checks if this {@link Path Path} instance represents
     * an absolute path.</p>
     */
    public boolean isAbsolute() {
        return this.absolute;
    }

    /**
     * <p>Checks if this {@link Path Path} instance represents
     * a collection.</p>
     */
    public boolean isCollection() {
        return this.collection;
    }

    /**
     * <p>Returns the collection of {@link Parameters Parameters}
     * contained by this instance or <b>null</b>.</p>
     */
    public Parameters getParameters() {
        return this.parameters;
    }

    /* ====================================================================== */
    /* OBJECT METHODS                                                         */
    /* ====================================================================== */

    /**
     * <p>Return the URL-encoded {@link String} representation of this
     * {@link Path Path} instance.</p>
     */
    public String toString() {
        return this.string;
    }

    /**
     * <p>Return the URL-encoded {@link String} representation of this
     * {@link Path Path} instance using the specified
     * character encoding.</p>
     */
    public String toString(String encoding)
    throws UnsupportedEncodingException {
        StringBuffer buffer = new StringBuffer();
        if (this.absolute) buffer.append('/');
        final int last = this.paths.length - 1;
        for (int x = 0; x < last; x ++) {
            buffer.append(this.paths[x].toString(encoding)).append('/');
        }
        if (last >= 0) {
            buffer.append(this.paths[last].toString(encoding));
            if (this.collection) buffer.append('/');
        }
        if (this.parameters != null)
            buffer.append(';').append(this.parameters.toString(encoding));
        return buffer.toString();
    }

    /**
     * <p>Return the hash code value of this
     * {@link Path Path} instance.</p>
     */
    public int hashCode() {
        return this.string.hashCode();
    }

    /**
     * <p>Check if the specified {@link Object} is equal to this
     * {@link Path Path} instance.</p>
     * 
     * <p>The specified {@link Object} is considered equal to this one if
     * it is <b>non-null</b>, is a {@link Path Path}
     * instance and its {@link #toString() string representation} equals
     * this one's.</p>
     */
    public boolean equals(Object object) {
        if ((object != null) && (object instanceof Path)) {
            return this.string.equals(((Path) object).string);
        }
        return false;
    }

    /* ====================================================================== */
    /* PUBLIC INNER CLASSES                                                   */
    /* ====================================================================== */

    /**
     * <p>The {@link Path.Element Element} class represents a path
     * element within the {@link Path Path} structure.</p>
     *
     * @author <a href="http://could.it/">Pier Fumagalli</a>
     */
    public static class Element implements Encodable {

        /** <p>The name of this path element (decoded).</p> */
        private final String name;
        /** <p>The extra path information of this path element (decoded).</p> */
        private final String extra;
        /** <p>The {@link String} representation of this (encoded).</p> */
        private final String string;

        /**
         * <p>Create a new {@link Path.Element Element} instance given its
         * url-decoded components name and extra.</p>
         * 
         * @throws NullPointerException if the specified name was <b>null</b>.
         */ 
        public Element(String name, String extra) {
            if (name == null) throw new NullPointerException("Null path name");
            this.name = name;
            this.extra = extra;
            this.string = EncodingTools.toString(this);
        }

        /* ================================================================== */
        /* PUBLIC EXPOSED METHODS                                             */
        /* ================================================================== */

        /**
         * <p>Return the url-decoded {@link String} name of this
         * {@link Path.Element Element}.</p>
         */
        public String getName() {
            return this.name;
        }
    
        /**
         * <p>Return the url-decoded {@link String} extra path of this
         * {@link Path.Element Element}.</p>
         */
        public String getExtra() {
            return this.extra;
        }

        /* ================================================================== */
        /* OBJECT METHODS                                                     */
        /* ================================================================== */

        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Path.Element Element} instance.</p>
         */
        public String toString() {
            return this.string;
        }
    
        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Path.Element Element} instance using the specified
         * character encoding.</p>
         */
        public String toString(String encoding)
        throws UnsupportedEncodingException {
            final StringBuffer buffer = new StringBuffer();
            buffer.append(EncodingTools.urlEncode(this.name, encoding));
            if (this.extra != null) {
                buffer.append('!');
                buffer.append(EncodingTools.urlEncode(this.extra, encoding));
            }
            return buffer.toString();
        }

        /**
         * <p>Return the hash code value of this
         * {@link Path.Element Element} instance.</p>
         */
        public int hashCode() {
            return this.string.hashCode();
        }

        /**
         * <p>Check if the specified {@link Object} is equal to this
         * {@link Path.Element Element} instance.</p>
         * 
         * <p>The specified {@link Object} is considered equal to this one if
         * it is <b>non-null</b>, is a {@link Path.Element Element}
         * instance and its {@link #toString() string representation} equals
         * this one's.</p>
         */
        public boolean equals(Object object) {
            if ((object != null) && (object instanceof Element)) {
                return this.string.equals(((Element) object).string);
            }
            return false;
        }
    }
}