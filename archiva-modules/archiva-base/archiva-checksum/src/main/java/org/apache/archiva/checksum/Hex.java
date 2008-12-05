package org.apache.archiva.checksum;

/**
 * Hex - simple hex conversions. 
 *
 * @version $Id$
 */
public class Hex
{
    private static final byte[] DIGITS = "0123456789abcdef".getBytes();

    public static String encode( byte[] data )
    {
        int l = data.length;

        byte[] raw = new byte[l * 2];

        for ( int i = 0, j = 0; i < l; i++ )
        {
            raw[j++] = DIGITS[( 0xF0 & data[i] ) >>> 4];
            raw[j++] = DIGITS[0x0F & data[i]];
        }

        return new String( raw );
    }

    public static String encode( String raw )
    {
        return encode( raw.getBytes() );
    }

}
