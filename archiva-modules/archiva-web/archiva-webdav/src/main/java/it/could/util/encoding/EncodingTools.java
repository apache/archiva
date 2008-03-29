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
package it.could.util.encoding;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * <p>An utility class providing various static methods dealing with
 * encodings and {@link Encodable} objects..</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public final class EncodingTools implements EncodingAware {

    /** <p>The Base-64 alphabet.</p> */
    private static final char ALPHABET[] = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/', '=' };

    /** <p>Deny construction of this class.</p> */
    private EncodingTools() { }

    /* ====================================================================== */
    /* URL ENCODING / DECODING                                                */
    /* ====================================================================== */

    /**
     * <p>Return the {@link String} representation of the specified
     * {@link Encodable} object using the {@link EncodingAware#DEFAULT_ENCODING
     * default encoding}.</p>
     *
     * throws NullPointerException if the {@link Encodable} was <b>null</b>.
     */
    public static String toString(Encodable encodable) {
        try {
            return encodable.toString(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            final String message = "Default encoding \"" + DEFAULT_ENCODING +
                                   "\" not supported by the platform";
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /* ====================================================================== */
    /* URL ENCODING / DECODING                                                */
    /* ====================================================================== */

    /**
     * <p>URL-encode the specified string.</p>
     */
    public static String urlEncode(String source, String encoding)
    throws UnsupportedEncodingException {
        if (source == null) return null;
        if (encoding == null) encoding = DEFAULT_ENCODING;
        return URLEncoder.encode(source, encoding);
    }

    /**
     * <p>URL-encode the specified string.</p>
     */
    public static String urlEncode(String source) {
        if (source == null) return null;
        try {
            return URLEncoder.encode(source, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
     * <p>URL-decode the specified string.</p>
     */
    public static String urlDecode(String source, String encoding)
    throws UnsupportedEncodingException {
        if (source == null) return null;
        if (encoding == null) encoding = DEFAULT_ENCODING;
        return URLDecoder.decode(source, encoding);
    }

    /**
     * <p>URL-decode the specified string.</p>
     */
    public static String urlDecode(String source) {
        if (source == null) return null;
        try {
            return URLDecoder.decode(source, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /* ====================================================================== */
    /* BASE 64 ENCODING / DECODING                                            */
    /* ====================================================================== */
    
    /**
     * <p>Encode the specified string in base 64 using the specified
     * encoding.</p>
     */
    public static final String base64Encode(String string, String encoding)
    throws UnsupportedEncodingException {
        /* Check the source string for null or the empty string. */
        if (string == null) return (null);
        if (string.length() == 0) return "";
    
        /* Check the encoding */
        if (encoding == null) encoding = DEFAULT_ENCODING;
    
        /* Prepare the buffers that we'll use to encode in Base 64 */
        final byte bsrc[] = string.getBytes(encoding);
        final char bdst[] = new char[(bsrc.length + 2) / 3 * 4];
    
        /* Iterate into the source in chunks of three bytes */
        int psrc = -1;
        int pdst = 0;
        int temp = 0;
        while ((psrc = psrc + 3) < bsrc.length) {
            /* For every three bytes processed ... */
            temp = ((bsrc[psrc - 2] << 16) & 0xFF0000) |
                   ((bsrc[psrc - 1] <<  8) & 0x00FF00) |
                   ((bsrc[psrc    ]      ) & 0x0000FF);
            /* ... we append four bytes to the buffer */
            bdst[pdst ++] = ALPHABET[(temp >> 18) & 0x3f];
            bdst[pdst ++] = ALPHABET[(temp >> 12) & 0x3f];
            bdst[pdst ++] = ALPHABET[(temp >>  6) & 0x3f];
            bdst[pdst ++] = ALPHABET[(temp      ) & 0x3f];
        }
    
        /* Let's check whether we still have some bytes to encode */
        switch (psrc - bsrc.length) {
        case 0: /* Two bytes left to encode */
            temp = ((bsrc[psrc - 2] & 0xFF) << 8) | (bsrc[psrc - 1] & 0xFF);
            bdst[pdst ++] = ALPHABET[(temp >> 10) & 0x3f];
            bdst[pdst ++] = ALPHABET[(temp >>  4) & 0x3f];
            bdst[pdst ++] = ALPHABET[(temp <<  2) & 0x3c];
            bdst[pdst ++] = ALPHABET[64];
            break;
        case 1: /* One byte left to encode */
            temp = (bsrc[psrc - 2] & 0xFF);
            bdst[pdst ++] = ALPHABET[(temp >> 2) & 0x3f];
            bdst[pdst ++] = ALPHABET[(temp << 4) & 0x30];
            bdst[pdst ++] = ALPHABET[64];
            bdst[pdst ++] = ALPHABET[64];
        }
    
        /* Convert the character array into a proper string */
        return new String(bdst);
    }

    /**
     * <p>Encode the specified string in base 64 using the default encoding.</p>
     */
    public static final String base64Encode(String string) {
        try {
            return (base64Encode(string, DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /**
         * <p>Decode the specified base 64 string using the specified encoding.</p>
         */
        public static final String base64Decode(String string, String encoding)
        throws UnsupportedEncodingException {
            /* Check the source string for null or the empty string. */
            if (string == null) return (null);
            if (string.length() == 0) return "";
    
            /* Check the encoding */
            if (encoding == null) encoding = DEFAULT_ENCODING;
    
            /* Retrieve the array of characters of the source string. */
            final char characters[] = string.toCharArray();
    
            /* Check the length, which must be dividible by 4. */
            if ((characters.length & 0x03) != 0)
                throw new IllegalArgumentException("Invalid length for the "+
                        "encoded string (" + characters.length + ")");
    
            /* The bytes array length is 3/4th of the characters array length */
            byte bytes[] = new byte[characters.length - (characters.length >> 2)];
    
            /*
             * Since this might take a while check now for the last 4 characters
             * token: it must contain at most two == and those need to be in the
             * last two positions in the array (the only valid sequences are:
             * "????", "???=" and "??==").
             */
            if (((characters[characters.length - 4] == '=') ||
                 (characters[characters.length - 3] == '=')) ||
                ((characters[characters.length - 2] == '=') &&
                 (characters[characters.length - 1] != '='))) {
                throw new IllegalArgumentException("Invalid pattern for last " +
                        "Base64 token in string to decode: " +
                        characters[characters.length - 4] +
                        characters[characters.length - 3] +
                        characters[characters.length - 2] +
                        characters[characters.length - 1]);
            }
    
            /* Translate the Base64-encoded String in chunks of 4 characters. */
            int coff = 0;
            int boff = 0;
            while (coff < characters.length) {
                boolean last = (coff == (characters.length - 4));
                int curr = ((value(characters[coff    ], last) << 0x12) |
                            (value(characters[coff + 1], last) << 0x0c) |
                            (value(characters[coff + 2], last) << 0x06) |
                            (value(characters[coff + 3], last)        ));
                bytes[boff + 2] = (byte)((curr        ) & 0xff);
                bytes[boff + 1] = (byte)((curr >> 0x08) & 0xff);
                bytes[boff    ] = (byte)((curr >> 0x10) & 0xff);
                coff += 4;
                boff += 3;
            }
    
            /* Get the real decoded string length, checking out the trailing '=' */
            if (characters[coff - 1] == '=') boff--;
            if (characters[coff - 2] == '=') boff--;
    
            /* All done */
            return (new String(bytes, 0, boff, encoding));
      }

    /**
     * <p>Decode the specified base 64 string using the default encoding.</p>
     */
    public static final String base64Decode(String string) {
        try {
            return (base64Decode(string, DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException exception) {
            final String message = "Unsupported encoding " + DEFAULT_ENCODING;
            final InternalError error = new InternalError(message);
            throw (InternalError) error.initCause(exception);
        }
    }

    /* ====================================================================== */

    /** <p>Retrieve the offset of a character in the base 64 alphabet.</p> */
    private static final int value(char character, boolean last) {
        for (int x = 0; x < 64; x++) if (ALPHABET[x] == character) return (x);
        if (last && (character == ALPHABET[65])) return(0);
        final String message = "Character \"" + character + "\" invalid";
        throw new IllegalArgumentException(message);
    }
}
