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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;


/**
 * <p>A collection of static utilities.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public final class DAVUtilities {

    /** <p>A {@link HashMap} of configured mime types.</p> */
    private static Map MIME_TYPES = new HashMap(); 
    /** <p>A {@link HashMap} of configured mime types.</p> */
    private static Properties PROPERTIES = new Properties(); 
    /** <p>The {@link SimpleDateFormat} RFC-822 date format.</p> */
    private static final String FORMAT_822 = "EEE, dd MMM yyyy HH:mm:ss 'GMT'";
    /** <p>The {@link SimpleDateFormat} RFC-822 date format.</p> */
    private static final String FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    /** <p>The {@link TimeZone} to use for dates.</p> */
    private static final TimeZone TIMEZONE = TimeZone.getTimeZone("GMT");
    /** <p>The {@link Locale} to use for dates.</p> */
    private static final Locale LOCALE = Locale.US;

    /**
     * <p>Load the mime types map from a resource.</p>
     */
    static {
        Class clazz = DAVUtilities.class;
        ClassLoader loader = clazz.getClassLoader();

        /* Load up the properties file */
        String webdavPropResource = "plexus-webdav/webdav.props";
        InputStream prop = loader.getResourceAsStream(webdavPropResource);
        if (prop != null) try {
            DAVUtilities.PROPERTIES.load(prop);
            prop.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        } else {
            System.err.println("Invalid resource: " + webdavPropResource);
        }

        /* Load up the mime types table */
        String mimeTypeResource = "plexus-webdav/mime.types";
        InputStream mime = loader.getResourceAsStream(mimeTypeResource);
        if (mime != null) try {
            InputStreamReader read = new InputStreamReader(mime);
            BufferedReader buff = new BufferedReader(read);
            String line = null;
            while ((line = buff.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                if (line.charAt(0) == '#') continue;
                StringTokenizer tokenizer = new StringTokenizer(line);
                if (tokenizer.countTokens() > 1) {
                    String type = tokenizer.nextToken();
                    while (tokenizer.hasMoreTokens()) {
                        String extension = '.' + tokenizer.nextToken();
                        DAVUtilities.MIME_TYPES.put(extension, type);
                    }
                }
            }
            buff.close();
            read.close();
            mime.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        } else {
            System.err.println("Invalid resource: " + mimeTypeResource);
        }
    }

    /** <p>The signature of this package usable from a servlet.</p> */
    public static final String SERVLET_SIGNATURE = 
            DAVUtilities.getProperty("servlet.signature") + '/' +
            DAVUtilities.getProperty("version");

    /** <p>The information detail of this package usable from a servlet.</p> */
    public static final String SERVLET_INFORMATION = 
            DAVUtilities.getProperty("servlet.information") + " version " +
            DAVUtilities.getProperty("version");

    /**
     * <p>Deny public construction of {@link DAVUtilities} instances.</p>
     */
    private DAVUtilities() {
        super();
    }

    /**
     * <p>Return the value of a property configured for this package.</p>
     * 
     * @param name the property name
     * @return a {@link String} instance or <b>null</b> if unknown.
     */
    public static String getProperty(String name) {
        if (name == null) return null;
        return DAVUtilities.PROPERTIES.getProperty(name);
    }

    /**
     * <p>Return the MIME Type configured for a given resource.</p>
     * 
     * @param name the resource name whose MIME Type needs to be looked up.
     * @return a {@link String} instance or <b>null</b> if the type is unknown.
     */
    public static String getMimeType(String name) {
        if (name == null) return null;

        Iterator iterator = DAVUtilities.MIME_TYPES.keySet().iterator();
        while (iterator.hasNext()) {
            String extension = (String) iterator.next();
            if (name.endsWith(extension)) {
                return (String) DAVUtilities.MIME_TYPES.get(extension);
            }
        }

        return null;
    }

    /**
     * <p>Return a {@link String} message given an HTTP status code.</p>
     */
    public static String getStatusMessage(int status) {
        switch (status) {
            /* HTTP/1.1 RFC-2616 */
            case 100: return "100 Continue";
            case 101: return "101 Switching Protocols";
            case 200: return "200 OK";
            case 201: return "201 Created";
            case 202: return "202 Accepted";
            case 203: return "203 Non-Authoritative Information";
            case 204: return "204 No Content";
            case 205: return "205 Reset Content";
            case 206: return "206 Partial Content";
            case 300: return "300 Multiple Choices";
            case 301: return "301 Moved Permanently";
            case 302: return "302 Found";
            case 303: return "303 See Other";
            case 304: return "304 Not Modified";
            case 305: return "305 Use Proxy";
            case 306: return "306 (Unused)";
            case 307: return "307 Temporary Redirect";
            case 400: return "400 Bad Request";
            case 401: return "401 Unauthorized";
            case 402: return "402 Payment Required";
            case 403: return "403 Forbidden";
            case 404: return "404 Not Found";
            case 405: return "405 Method Not Allowed";
            case 406: return "406 Not Acceptable";
            case 407: return "407 Proxy Authentication Required";
            case 408: return "408 Request Timeout";
            case 409: return "409 Conflict";
            case 410: return "410 Gone";
            case 411: return "411 Length Required";
            case 412: return "412 Precondition Failed";
            case 413: return "413 Request Entity Too Large";
            case 414: return "414 Request-URI Too Long";
            case 415: return "415 Unsupported Media Type";
            case 416: return "416 Requested Range Not Satisfiable";
            case 417: return "417 Expectation Failed";
            case 500: return "500 Internal Server Error";
            case 501: return "501 Not Implemented";
            case 502: return "502 Bad Gateway";
            case 503: return "503 Service Unavailable";
            case 504: return "504 Gateway Timeout";
            case 505: return "505 HTTP Version Not Supported";

            /* DAV/1.0 RFC-2518 */
            case 102: return "102 Processing";
            case 207: return "207 Multi-Status";
            case 422: return "422 Unprocessable Entity";
            case 423: return "423 Locked";
            case 424: return "424 Failed Dependency";
            case 507: return "507 Insufficient Storage";

            /* Unknown */
            default:  return null;
        }
    }

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

    /**
     * <p>Return the HEX representation of an array of bytes.</p>
     * 
     * @param buffer the array of bytes to convert in a HEX {@link String}.
     * @return a <b>non-null</b> {@link String} instance.
     */
    public static String toHexString(byte buffer[]) {
        char output[] = new char[buffer.length * 2];
        int position = 0;
        for (int x = 0; x < buffer.length; x++) {
            output[position ++] = DAVUtilities.toHexDigit(buffer[x] >> 4);
            output[position ++] = DAVUtilities.toHexDigit(buffer[x]);
        }
        return new String(output);
    }

    /**
     * <p>Return the HEX representation of a long integer.</p>
     * 
     * @param number the long to convert in a HEX {@link String}.
     * @return a <b>non-null</b> 16-characters {@link String} instance.
     */
    public static String toHexString(long number) {
        char output[] = new char[16];
        output[0] = DAVUtilities.toHexDigit((int)(number >> 60));
        output[1] = DAVUtilities.toHexDigit((int)(number >> 56));
        output[2] = DAVUtilities.toHexDigit((int)(number >> 52));
        output[3] = DAVUtilities.toHexDigit((int)(number >> 48));
        output[4] = DAVUtilities.toHexDigit((int)(number >> 44));
        output[5] = DAVUtilities.toHexDigit((int)(number >> 40));
        output[6] = DAVUtilities.toHexDigit((int)(number >> 36));
        output[7] = DAVUtilities.toHexDigit((int)(number >> 32));
        output[8] = DAVUtilities.toHexDigit((int)(number >> 28));
        output[9] = DAVUtilities.toHexDigit((int)(number >> 24));
        output[10] = DAVUtilities.toHexDigit((int)(number >> 20));
        output[11] = DAVUtilities.toHexDigit((int)(number >> 16));
        output[12] = DAVUtilities.toHexDigit((int)(number >> 12));
        output[13] = DAVUtilities.toHexDigit((int)(number >> 8));
        output[14] = DAVUtilities.toHexDigit((int)(number >> 4));
        output[15] = DAVUtilities.toHexDigit((int)(number));
        return new String(output);
    }

    /**
     * <p>Return the HEX representation of an integer.</p>
     * 
     * @param number the int to convert in a HEX {@link String}.
     * @return a <b>non-null</b> 8-characters {@link String} instance.
     */
    public static String toHexString(int number) {
        char output[] = new char[8];
        output[0] = DAVUtilities.toHexDigit((int)(number >> 28));
        output[1] = DAVUtilities.toHexDigit((int)(number >> 24));
        output[2] = DAVUtilities.toHexDigit((int)(number >> 20));
        output[3] = DAVUtilities.toHexDigit((int)(number >> 16));
        output[4] = DAVUtilities.toHexDigit((int)(number >> 12));
        output[5] = DAVUtilities.toHexDigit((int)(number >> 8));
        output[6] = DAVUtilities.toHexDigit((int)(number >> 4));
        output[7] = DAVUtilities.toHexDigit((int)(number));
        return new String(output);
    }

    /**
     * <p>Return the HEX representation of a char.</p>
     * 
     * @param number the char to convert in a HEX {@link String}.
     * @return a <b>non-null</b> 4-characters {@link String} instance.
     */
    public static String toHexString(char number) {
        char output[] = new char[4];
        output[0] = DAVUtilities.toHexDigit((int)(number >> 12));
        output[1] = DAVUtilities.toHexDigit((int)(number >> 8));
        output[2] = DAVUtilities.toHexDigit((int)(number >> 4));
        output[3] = DAVUtilities.toHexDigit((int)(number));
        return new String(output);
    }

    /**
     * <p>Return the HEX representation of a byte.</p>
     * 
     * @param number the byte to convert in a HEX {@link String}.
     * @return a <b>non-null</b> 2-characters {@link String} instance.
     */
    public static String toHexString(byte number) {
        char output[] = new char[2];
        output[0] = DAVUtilities.toHexDigit((int)(number >> 4));
        output[1] = DAVUtilities.toHexDigit((int)(number));
        return new String(output);
    }

    /**
     * <p>Return the single digit character representing the HEX encoding of
     * the lower four bits of a given integer.</p>
     */
    private static char toHexDigit(int number) {
        switch (number & 0x0F) {
            case 0x00: return '0';
            case 0x01: return '1';
            case 0x02: return '2';
            case 0x03: return '3';
            case 0x04: return '4';
            case 0x05: return '5';
            case 0x06: return '6';
            case 0x07: return '7';
            case 0x08: return '8';
            case 0x09: return '9';
            case 0x0A: return 'A';
            case 0x0B: return 'B';
            case 0x0C: return 'C';
            case 0x0D: return 'D';
            case 0x0E: return 'E';
            case 0x0F: return 'F';
        }
        String message = "Invalid HEX digit " + Integer.toHexString(number);
        throw new IllegalArgumentException(message);
    }
}
