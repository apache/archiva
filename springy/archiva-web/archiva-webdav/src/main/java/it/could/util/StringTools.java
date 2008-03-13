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
package it.could.util;

import it.could.util.encoding.Encodable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>An utility class providing various static methods operating on
 * {@link String}s.</p>
 * 
 * <p>This class implement the {@link Encodable} interface from which it
 * inherits its {@link Encodable#DEFAULT_ENCODING default encoding}.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public final class StringTools {

    /** <p>The {@link SimpleDateFormat} RFC-822 date format.</p> */
    private static final String FORMAT_822 = "EEE, dd MMM yyyy HH:mm:ss 'GMT'";
    /** <p>The {@link SimpleDateFormat} RFC-822 date format.</p> */
    private static final String FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    /** <p>The {@link TimeZone} to use for dates.</p> */
    private static final TimeZone TIMEZONE = TimeZone.getTimeZone("GMT");
    /** <p>The {@link Locale} to use for dates.</p> */
    private static final Locale LOCALE = Locale.US;

    /** <p>Deny construction.</p> */
    private StringTools() { }

    /* ====================================================================== */
    /* NUMBER AND DATE PARSING AND FORMATTING                                 */
    /* ====================================================================== */

    /**
     * <p>Format a {@link Number} into a {@link String} making sure that
     * {@link NullPointerException}s are not thrown.</p>
     * 
     * @param number the {@link Number} to format.
     * @return a {@link String} instance or <b>null</b> if the object was null.
     */
    public static String formatNumber(Number number) {
        if (number == null) return null;
        return (number.toString());
    }

    /**
     * <p>Parse a {@link String} into a {@link Long}.</p>
     * 
     * @param string the {@link String} to parse.
     * @return a {@link Long} instance or <b>null</b> if the date was null or
     *         if there was an error parsing the specified {@link String}.
     */
    public static Long parseNumber(String string) {
        if (string == null) return null;
        try {
            return new Long(string);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * <p>Format a {@link Date} according to the HTTP/1.1 RFC.</p>
     * 
     * @param date the {@link Date} to format.
     * @return a {@link String} instance or <b>null</b> if the date was null.
     */
    public static String formatHttpDate(Date date) {
        if (date == null) return null;
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_822, LOCALE);
        formatter.setTimeZone(TIMEZONE);
        return formatter.format(date);
    }

    /**
     * <p>Format a {@link Date} according to the ISO 8601 specification.</p>
     * 
     * @param date the {@link Date} to format.
     * @return a {@link String} instance or <b>null</b> if the date was null.
     */
    public static String formatIsoDate(Date date) {
        if (date == null) return null;
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_ISO, LOCALE);
        formatter.setTimeZone(TIMEZONE);
        return formatter.format(date);
    }

    /**
     * <p>Parse a {@link String} into a {@link Date} according to the
     * HTTP/1.1 RFC (<code>Mon, 31 Jan 2000 11:59:00 GMT</code>).</p>
     * 
     * @param string the {@link String} to parse.
     * @return a {@link Date} instance or <b>null</b> if the date was null or
     *         if there was an error parsing the specified {@link String}.
     */
    public static Date parseHttpDate(String string) {
        if (string == null) return null;
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_822, LOCALE);
        formatter.setTimeZone(TIMEZONE);
        try {
            return formatter.parse(string);
        } catch (ParseException exception) {
            return null;
        }
    }

    /**
     * <p>Parse a {@link String} into a {@link Date} according to the ISO 8601
     * specification (<code>2000-12-31T11:59:00Z</code>).</p>
     * 
     * @param string the {@link String} to parse.
     * @return a {@link Date} instance or <b>null</b> if the date was null or
     *         if there was an error parsing the specified {@link String}.
     */
    public static Date parseIsoDate(String string) {
        if (string == null) return null;
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_ISO, LOCALE);
        formatter.setTimeZone(TIMEZONE);
        try {
            return formatter.parse(string);
        } catch (ParseException exception) {
            return null;
        }
    }

    /* ====================================================================== */
    /* STRING SPLITTING                                                       */
    /* ====================================================================== */

    /**
     * <p>Split the specified string in two parts according to the specified
     * delimiter, and any resulting path of zero length will be converted to
     * <b>null</b>.</p>
     */
    public static String[] splitOnce(String source, char delimiter,
                                      boolean noDelimReturnSecond) {
        if (source == null) return new String[] { null, null };
        final int position = source.indexOf(delimiter);
        if (position < 0) { // --> first
            if (noDelimReturnSecond) return new String[] { null, source };
            else return new String[] { source, null };
        } else if (position == 0) {
            if (source.length() == 1) { // --> |
                return new String[] { null, null };
            } else { // --> |second
                return new String[] { null, source.substring(1) };
            }
        } else {
            final String first = source.substring(0, position);
            if (source.length() -1 == position) { // --> first|
                return new String[] { first, null };
            } else { // --> first|second
                return new String[] { first, source.substring(position + 1) };
            }
        }
    }

    /**
     * <p>Split the specified string according to the specified delimiter, and
     * any resulting path of zero length will be converted to <b>null</b>.</p>
     */
    public static String[] splitAll(String source, char delimiter) {
        final List strings = new ArrayList();
        String current = source;
        while (current != null) {
            String split[] = splitOnce(current, delimiter, false);
            strings.add(split[0]);
            current = split[1];
        }
        if (current != null) strings.add(current);
        final int length = source.length();
        if ((length > 0) && (source.charAt(length - 1) == delimiter)) {
            strings.add(null);
        }
        return (String []) strings.toArray(new String[strings.size()]);
    }

    /**
     * <p>Find the first occurrence of one of the specified delimiter characters
     * in the specified source string.</p>
     */
    public static int findFirst(String source, String delimiters) {
        final char array[] = source.toCharArray();
        final char delim[] = delimiters.toCharArray();
        for (int x = 0; x < array.length; x ++) {
            for (int y = 0; y < delim.length; y ++) {
                if (array[x] == delim[y]) return x;
            }
        }
        return -1;
    }
}
