package org.apache.maven.archiva.web.startup;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.slf4j.LoggerFactory;

/**
 * Banner 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *              role="org.apache.maven.archiva.web.startup.Banner"
 *              role-hint="default"
 */
public class Banner
{
    public static String encode( String raw )
    {
        StringBuffer encoded = new StringBuffer();
        int rawlen = raw.length();

        for ( int i = 0; i < rawlen; i++ )
        {
            char c = raw.charAt( i );
            if ( c == '\\' )
            {
                encoded.append( "$." );
            }
            else if ( c == '$' )
            {
                encoded.append( "$$" );
            }
            else if ( c == '\n' )
            {
                encoded.append( "$n" );
            }
            else if ( Character.isDigit( c ) )
            {
                encoded.append( c );
            }
            else if ( Character.isLetter( c ) )
            {
                encoded.append( rot13( c ) );
            }
            else if ( i < raw.length() - 1 )
            {
                char nc;
                boolean done = false;
                int count = 0;
                for ( int n = i; !done; n++ )
                {
                    if ( n >= rawlen )
                    {
                        break;
                    }

                    nc = raw.charAt( n );

                    if ( nc != c )
                    {
                        done = true;
                    }
                    else
                    {
                        count++;
                    }
                }
                if ( count < 3 )
                {
                    encoded.append( c );
                }
                else
                {
                    encoded.append( "$" ).append( String.valueOf( count ) ).append( c );
                    i += count - 1;
                }
            }
            else
            {
                encoded.append( c );
            }
        }

        return encoded.toString();
    }

    public static String decode( String encoded )
    {
        StringBuffer decoded = new StringBuffer();
        int enlen = encoded.length();
        for ( int i = 0; i < enlen; i++ )
        {
            char c = encoded.charAt( i );
            if ( c == '$' )
            {
                char nc = encoded.charAt( i + 1 );
                if ( nc == '$' )
                {
                    decoded.append( '$' );
                    i++;
                }
                else if ( nc == '.' )
                {
                    decoded.append( '\\' );
                    i++;
                }
                else if ( nc == 'n' )
                {
                    decoded.append( '\n' );
                    i++;
                }
                else if ( Character.isDigit( nc ) )
                {
                    int count = 0;
                    int nn = i + 1;
                    while ( Character.isDigit( nc ) )
                    {
                        count = ( count * 10 );
                        count += ( nc - '0' );
                        nc = encoded.charAt( ++nn );
                    }
                    for ( int d = 0; d < count; d++ )
                    {
                        decoded.append( nc );
                    }
                    i = nn;
                }
            }
            else if ( Character.isLetter( c ) )
            {
                decoded.append( rot13( c ) );
            }
            else
            {
                decoded.append( c );
            }
        }

        return decoded.toString();
    }

    private static char rot13( char c )
    {
        if ( ( c >= 'a' ) && ( c <= 'z' ) )
        {
            char dc = c += 13;
            if ( dc > 'z' )
            {
                dc -= 26;
            }
            return dc;
        }
        else if ( ( c >= 'A' ) && ( c <= 'Z' ) )
        {
            char dc = c += 13;
            if ( dc > 'Z' )
            {
                dc -= 26;
            }
            return dc;
        }
        else
        {
            return c;
        }
    }

    public static String injectVersion( String text, String version )
    {
        Pattern pat = Pattern.compile( "#{2,}" );
        Matcher mat = pat.matcher( text );
        StringBuffer ret = new StringBuffer();
        int off = 0;

        while ( mat.find( off ) )
        {
            ret.append( text.substring( off, mat.start() ) );
            String repl = mat.group();
            ret.append( StringUtils.center( version, repl.length() ) );
            off = mat.end();
        }

        ret.append( text.substring( off ) );

        return ret.toString();
    }

    public static String getBanner( String version )
    {
        String encodedBanner = "$26 $34_$n$15 /$._$7 /$34 $.$n$14 /`/@),$4 |  Ba" +
                " orunys bs nyy bs gur nycnpn'f  |$n$14 |  (~'  __|  gbvyvat njnl ba " +
                "gur Ncnpur Znira  |$n$6 _,--.$3_/  |$4 $.$5 cebwrpg grnzf, V jbhyq y" +
                "vxr gb$3 |$n$4 ,' ,$5 ($3 |$5 $.$5 jrypbzr lbh gb Znira Nepuvin$4 |$" +
                "n$4 |  ($6 $.  /$6 |  $32#  |$n$5 $.  )$._/  ,_/$7 |$36 |$n$5 / /$3 " +
                "( |/$9 |  uggc://znira.ncnpur.bet/nepuvin/  |$n$4 ( |$4 ( |$10 |  ne" +
                "puvin-hfref@znira.ncnpur.bet$4 |$n$5 $.|$5 $.|$11 $.$34_/$n$n";

        return injectVersion( decode( encodedBanner ), version );
    }

    public static void display( String version )
    {
        String banner = getBanner( version );
        LoggerFactory.getLogger( Banner.class ).info( StringUtils.repeat( "_", 25 ) + "\n" + banner );
    }
    
    public void initialize()
        throws InitializationException
    {
        Banner.display( ArchivaVersion.determineVersion( this.getClass().getClassLoader() ) );
    }
}
