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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * <p>The {@link Parameters Parameters} class represents a never empty and
 * immutable {@link List} of {@link Parameters.Parameter Parameter} instances,
 * normally created parsing a query string.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class Parameters extends AbstractList implements Encodable {

    /** <p>The default delimiter for a {@link Parameters} instance.</p> */
    public static final char DEFAULT_DELIMITER = '&';

    /** <p>All the {@link Parameter}s in order.</p> */
    private final Parameter parameters[];
    /** <p>The {@link Map} view over all parameters (names are keys).</p> */
    private final Map map;
    /** <p>The {@link Set} of all parameter names.</p> */
    final Set names;
    /** <p>The character delimiting different parameters.</p> */
    private final char delimiter;
    /** <p>The encoded {@link String} representation of this.</p> */
    private final String string;

    /**
     * <p>Create a new {@link Parameters Parameters} instance from
     * a {@link List} of {@link Parameters.Parameter Parameter} instances
     * using the {@link #DEFAULT_DELIMITER default parameter delimiter}.</p>
     * 
     * @throws NullPointerExceptoin if the {@link List} was <b>null</b>.
     * @throws IllegalArgumentException if the {@link List} was empty.
     * @throws ClassCastException if any of the elements in the {@link List} was
     *                            not a {@link Parameters.Parameter Parameter}.
     */
    public Parameters(List parameters) {
        this(parameters, DEFAULT_DELIMITER);
    }

    /**
     * <p>Create a new {@link Parameters Parameters} instance from
     * a {@link List} of {@link Parameters.Parameter Parameter} instances
     * using the specified character as the parameters delimiter.</p>
     * 
     * @throws NullPointerExceptoin if the {@link List} was <b>null</b>.
     * @throws IllegalArgumentException if the {@link List} was empty.
     * @throws ClassCastException if any of the elements in the {@link List} was
     *                            not a {@link Parameters.Parameter Parameter}.
     */
    public Parameters(List parameters, char delimiter) {
        if (parameters.size() == 0) throw new IllegalArgumentException();
        final Parameter array[] = new Parameter[parameters.size()];
        final Map map = new HashMap();
        for (int x = 0; x < array.length; x ++) {
            final Parameter parameter = (Parameter) parameters.get(x);
            final String key = parameter.getName();
            List values = (List) map.get(key);
            if (values == null) {
                values = new ArrayList();
                map.put(key, values);
            }
            values.add(parameter.getValue());
            array[x] = parameter;
        }

        /* Make all parameter value lists unmodifiable */
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
            final Map.Entry entry = (Map.Entry) iter.next();
            final List list = (List) entry.getValue();
            entry.setValue(Collections.unmodifiableList(list));
        }

        /* Store the current values */
        this.delimiter = delimiter;
        this.map = Collections.unmodifiableMap(map);
        this.names = Collections.unmodifiableSet(map.keySet());
        this.parameters = array;
        this.string = EncodingTools.toString(this);
    }

    /* ====================================================================== */
    /* STATIC CONSTRUCTION METHODS                                            */
    /* ====================================================================== */

    /**
     * <p>Utility method to create a new {@link Parameters} instance from a
     * {@link List} of {@link Parameters.Parameter Parameter} instances.</p>
     *
     * @return a <b>non-null</b> and not empty {@link Parameters} instance or
     *         <b>null</b> if the specified {@link List} was <b>null</b>, empty
     *         or did not contain any {@link Parameters.Parameter Parameter}.
     * @throws ClassCastException if any of the elements in the {@link List} was
     *                            not a {@link Parameters.Parameter Parameter}.
     */
    public static Parameters create(List parameters) {
        return create(parameters, DEFAULT_DELIMITER);
    }

    /**
     * <p>Utility method to create a new {@link Parameters} instance from a
     * {@link List} of {@link Parameters.Parameter Parameter} instances.</p>
     *
     * @return a <b>non-null</b> and not empty {@link Parameters} instance or
     *         <b>null</b> if the specified {@link List} was <b>null</b>, empty
     *         or did not contain any {@link Parameters.Parameter Parameter}.
     * @throws ClassCastException if any of the elements in the {@link List} was
     *                            not a {@link Parameters.Parameter Parameter}.
     */
    public static Parameters create(List parameters, char delimiter) {
        if (parameters == null) return null;
        final List dedupes = new ArrayList();
        for (Iterator iter = parameters.iterator(); iter.hasNext(); ) {
            Object next = iter.next();
            if (dedupes.contains(next)) continue;
            dedupes.add(next);
        }
        if (dedupes.size() == 0) return null;
        return new Parameters(dedupes, delimiter);
    }

    /**
     * <p>Parse the specified parameters {@link String} into a
     * {@link Parameters} instance using the {@link #DEFAULT_DELIMITER default
     * parameter delimiter}.</p>
     *
     * @return a <b>non-null</b> and not empty {@link Parameters} instance or
     *         <b>null</b> if the specified string was <b>null</b>, empty or
     *         did not contain any {@link Parameters.Parameter Parameter}.
     */
    public static Parameters parse(String parameters) {
        try {
            return parse(parameters, DEFAULT_DELIMITER, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
     * <p>Parse the specified parameters {@link String} into a
     * {@link Parameters} instance using the specified character as the
     * parameters delimiter.</p>
     *
     * @return a <b>non-null</b> and not empty {@link Parameters} instance or
     *         <b>null</b> if the specified string was <b>null</b>, empty or
     *         did not contain any {@link Parameters.Parameter Parameter}.
     */
    public static Parameters parse(String parameters, char delimiter) {
        try {
            return parse(parameters, delimiter, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
     * <p>Parse the specified parameters {@link String} into a
     * {@link Parameters} instance using the {@link #DEFAULT_DELIMITER default
     * parameter delimiter}.</p>
     *
     * @return a <b>non-null</b> and not empty {@link Parameters} instance or
     *         <b>null</b> if the specified string was <b>null</b>, empty or
     *         did not contain any {@link Parameters.Parameter Parameter}.
     */
    public static Parameters parse(String parameters, String encoding)
    throws UnsupportedEncodingException {
        return parse(parameters, DEFAULT_DELIMITER, encoding);
    }

    /**
     * <p>Parse the specified parameters {@link String} into a
     * {@link Parameters} instance using the specified character as the
     * parameters delimiter.</p>
     *
     * @return a <b>non-null</b> and not empty {@link Parameters} instance or
     *         <b>null</b> if the specified string was <b>null</b>, empty or
     *         did not contain any {@link Parameters.Parameter Parameter}.
     */
    public static Parameters parse(String parameters, char delimiter,
                                   String encoding)
    throws UnsupportedEncodingException {
        if (parameters == null) return null;
        if (parameters.length() == 0) return null;
        if (encoding == null) encoding = DEFAULT_ENCODING;
        final String split[] = StringTools.splitAll(parameters, delimiter);
        final List list = new ArrayList();
        for (int x = 0; x < split.length; x ++) {
            if (split[x] == null) continue;
            if (split[x].length() == 0) continue;
            Parameter parameter = Parameter.parse(split[x], encoding);
            if (parameter != null) list.add(parameter); 
        }
        if (list.size() == 0) return null;
        return new Parameters(list, delimiter);
    }

    /* ====================================================================== */
    /* PUBLIC EXPOSED METHODS                                                 */
    /* ====================================================================== */

    /**
     * <p>Return the number of {@link Parameters.Parameter Parameter}s
     * contained by this instance.</p>
     */
    public int size() {
        return this.parameters.length;
    }

    /**
     * <p>Return the {@link Parameters.Parameter Parameter} stored by this\
     * instance at the specified index.</p>
     */
    public Object get(int index) {
        return this.parameters[index];
    }
    
    /**
     * <p>Return an immutable {@link Set} of {@link String}s containing all
     * known {@link Parameters.Parameter Parameter}
     * {@link Parameters.Parameter#getName() names}.</p>
     */
    public Set getNames() {
        return this.names;
    }

    /**
     * <p>Return the first {@link String} value associated with the
     * specified parameter name, or <b>null</b>.</p> 
     */
    public String getValue(String name) {
        final List values = (List) this.map.get(name);
        return values == null ? null : (String) values.get(0);
    }

    /**
     * <p>Return an immutable {@link List} of all {@link String} values
     * associated with the specified parameter name, or <b>null</b>.</p> 
     */
    public List getValues(String name) {
        return (List) this.map.get(name);
    }

    /* ====================================================================== */
    /* OBJECT METHODS                                                         */
    /* ====================================================================== */

    /**
     * <p>Return the URL-encoded {@link String} representation of this
     * {@link Parameters Parameters} instance.</p>
     */
    public String toString() {
        return this.string;
    }

    /**
     * <p>Return the URL-encoded {@link String} representation of this
     * {@link Parameters Parameters} instance using the specified
     * character encoding.</p>
     */
    public String toString(String encoding)
    throws UnsupportedEncodingException {
        StringBuffer buffer = new StringBuffer();
        for (int x = 0; x < this.parameters.length; x ++) {
            buffer.append(this.delimiter);
            buffer.append(this.parameters[x].toString(encoding));
        }
        return buffer.substring(1);
    }

    /**
     * <p>Return the hash code value of this
     * {@link Parameters Parameters} instance.</p>
     */
    public int hashCode() {
        return this.string.hashCode();
    }

    /**
     * <p>Check if the specified {@link Object} is equal to this
     * {@link Parameters Parameters} instance.</p>
     * 
     * <p>The specified {@link Object} is considered equal to this one if
     * it is <b>non-null</b>, it is a {@link Parameters Parameters}
     * instance, and its {@link #toString() string representation} equals
     * this one's.</p>
     */
    public boolean equals(Object object) {
        if ((object != null) && (object instanceof Parameters)) {
            return this.string.equals(((Parameters) object).string);
        } else {
            return false;
        }
    }

    /* ====================================================================== */
    /* PUBLIC INNER CLASSES                                                   */
    /* ====================================================================== */

    /**
     * <p>The {@link Parameters.Parameter Parameter} class represents a single
     * parameter either parsed from a query string or a path element.</p>
     * 
     * @author <a href="http://could.it/">Pier Fumagalli</a>
     */
    public static class Parameter implements Encodable {
        /** <p>The name of the parameter (decoded).</p> */
        private final String name;
        /** <p>The value of the parameter (decoded).</p> */
        private final String value;
        /** <p>The encoded {@link String} representation of this.</p> */
        private final String string;

        /**
         * <p>Create a new {@link Parameters.Parameter Parameter} given an
         * encoded parameter name and value.</p>
         * 
         * @throws NullPointerException if the name was <b>null</b>.
         * @throws IllegalArgumentException if the name was an empty string.
         */
        public Parameter(String name, String value) {
            if (name == null) throw new NullPointerException();
            if (name.length() == 0) throw new IllegalArgumentException();
            this.name = name;
            this.value = value;
            this.string = EncodingTools.toString(this);
        }

        /* ================================================================== */
        /* STATIC CONSTRUCTION METHODS                                        */
        /* ================================================================== */

        /**
         * <p>Parse the specified parameters {@link String} into a
         * {@link Parameters.Parameter} instance.</p>
         *
         * @return a <b>non-null</b> and not empty {@link Parameters.Parameter}
         *         instance or <b>null</b> if the specified string was
         *         <b>null</b> or empty.
         */
        public static Parameter parse(String parameter)
        throws UnsupportedEncodingException {
            try {
                return parse(parameter, DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException exception) {
                final String message = "Unsupported encoding " + DEFAULT_ENCODING;
                final InternalError error = new InternalError(message);
                throw (InternalError) error.initCause(exception);
            }
        }

        /**
         * <p>Parse the specified parameters {@link String} into a
         * {@link Parameters.Parameter} instance.</p>
         *
         * @return a <b>non-null</b> and not empty {@link Parameters.Parameter}
         *         instance or <b>null</b> if the specified string was
         *         <b>null</b> or empty.
         */
        public static Parameter parse(String parameter, String encoding)
        throws UnsupportedEncodingException {
            if (parameter == null) return null;
            if (encoding == null) encoding = DEFAULT_ENCODING;
            String split[] = StringTools.splitOnce(parameter, '=', false);
            if (split[0] == null) return null;
            return new Parameter(split[0], split[1]);
        }

        /* ================================================================== */
        /* PUBLIC EXPOSED METHODS                                             */
        /* ================================================================== */

        /**
         * <p>Return the URL-decoded name of this
         * {@link Parameters.Parameter Parameter} instance.</p>
         */
        public String getName() {
            return this.name;
        }
    
        /**
         * <p>Return the URL-decoded value of this
         * {@link Parameters.Parameter Parameter} instance.</p>
         */
        public String getValue() {
            return this.value;
        }
    
        /* ================================================================== */
        /* OBJECT METHODS                                                     */
        /* ================================================================== */

        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Parameters.Parameter Parameter} instance.</p>
         */
        public String toString() {
            return this.string;
        }
    
        /**
         * <p>Return the URL-encoded {@link String} representation of this
         * {@link Parameters.Parameter Parameter} instance using the specified
         * character encoding.</p>
         */
        public String toString(String encoding)
        throws UnsupportedEncodingException {
            if (this.value != null) {
                return EncodingTools.urlEncode(this.name, encoding) + "=" +
                       EncodingTools.urlEncode(this.value, encoding);
            } else {
                return EncodingTools.urlEncode(this.name, encoding);
            }
        }

        /**
         * <p>Return the hash code value for this
         * {@link Parameters.Parameter Parameter} instance.</p>
         */
        public int hashCode() {
            return this.string.hashCode();
        }
    
        /**
         * <p>Check if the specified {@link Object} is equal to this
         * {@link Parameters.Parameter Parameter} instance.</p>
         * 
         * <p>The specified {@link Object} is considered equal to this one if
         * it is <b>non-null</b>, it is a {@link Parameters.Parameter Parameter}
         * instance, and its {@link #toString() string representation} equals
         * this one's.</p>
         */
        public boolean equals(Object object) {
            if ((object != null) && (object instanceof Parameter)) {
                return this.string.equals(((Parameter) object).string);
            } else {
                return false;
            }
        }
    }
}