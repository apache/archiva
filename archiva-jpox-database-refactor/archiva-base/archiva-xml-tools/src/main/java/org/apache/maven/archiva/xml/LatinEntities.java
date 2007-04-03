package org.apache.maven.archiva.xml;

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

import java.util.HashMap;
import java.util.Map;

/**
 * LatinEntities - simple utility class to handle latin entity conversion. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LatinEntities
{
    private static final Map defaultEntityMap;

    static
    {
        defaultEntityMap = new HashMap();

        defaultEntityMap.put( "nbsp", "\u00a0" );
        defaultEntityMap.put( "iexcl", "\u00a1" );
        defaultEntityMap.put( "cent", "\u00a2" );
        defaultEntityMap.put( "pound", "\u00a3" );
        defaultEntityMap.put( "curren", "\u00a4" );
        defaultEntityMap.put( "yen", "\u00a5" );
        defaultEntityMap.put( "brvbar", "\u00a6" );
        defaultEntityMap.put( "sect", "\u00a7" );
        defaultEntityMap.put( "uml", "\u00a8" );
        defaultEntityMap.put( "copy", "\u00a9" );
        defaultEntityMap.put( "ordf", "\u00aa" );
        defaultEntityMap.put( "laquo", "\u00ab" );
        defaultEntityMap.put( "not", "\u00ac" );
        defaultEntityMap.put( "shy", "\u00ad" );
        defaultEntityMap.put( "reg", "\u00ae" );
        defaultEntityMap.put( "macr", "\u00af" );
        defaultEntityMap.put( "deg", "\u00b0" );
        defaultEntityMap.put( "plusmn", "\u00b1" );
        defaultEntityMap.put( "sup2", "\u00b2" );
        defaultEntityMap.put( "sup3", "\u00b3" );
        defaultEntityMap.put( "acute", "\u00b4" );
        defaultEntityMap.put( "micro", "\u00b5" );
        defaultEntityMap.put( "para", "\u00b6" );
        defaultEntityMap.put( "middot", "\u00b7" );
        defaultEntityMap.put( "cedil", "\u00b8" );
        defaultEntityMap.put( "sup1", "\u00b9" );
        defaultEntityMap.put( "ordm", "\u00ba" );
        defaultEntityMap.put( "raquo", "\u00bb" );
        defaultEntityMap.put( "frac14", "\u00bc" );
        defaultEntityMap.put( "frac12", "\u00bd" );
        defaultEntityMap.put( "frac34", "\u00be" );
        defaultEntityMap.put( "iquest", "\u00bf" );
        defaultEntityMap.put( "Agrave", "\u00c0" );
        defaultEntityMap.put( "Aacute", "\u00c1" );
        defaultEntityMap.put( "Acirc", "\u00c2" );
        defaultEntityMap.put( "Atilde", "\u00c3" );
        defaultEntityMap.put( "Auml", "\u00c4" );
        defaultEntityMap.put( "Aring", "\u00c5" );
        defaultEntityMap.put( "AElig", "\u00c6" );
        defaultEntityMap.put( "Ccedil", "\u00c7" );
        defaultEntityMap.put( "Egrave", "\u00c8" );
        defaultEntityMap.put( "Eacute", "\u00c9" );
        defaultEntityMap.put( "Ecirc", "\u00ca" );
        defaultEntityMap.put( "Euml", "\u00cb" );
        defaultEntityMap.put( "Igrave", "\u00cc" );
        defaultEntityMap.put( "Iacute", "\u00cd" );
        defaultEntityMap.put( "Icirc", "\u00ce" );
        defaultEntityMap.put( "Iuml", "\u00cf" );
        defaultEntityMap.put( "ETH", "\u00d0" );
        defaultEntityMap.put( "Ntilde", "\u00d1" );
        defaultEntityMap.put( "Ograve", "\u00d2" );
        defaultEntityMap.put( "Oacute", "\u00d3" );
        defaultEntityMap.put( "Ocirc", "\u00d4" );
        defaultEntityMap.put( "Otilde", "\u00d5" );
        defaultEntityMap.put( "Ouml", "\u00d6" );
        defaultEntityMap.put( "times", "\u00d7" );
        defaultEntityMap.put( "Oslash", "\u00d8" );
        defaultEntityMap.put( "Ugrave", "\u00d9" );
        defaultEntityMap.put( "Uacute", "\u00da" );
        defaultEntityMap.put( "Ucirc", "\u00db" );
        defaultEntityMap.put( "Uuml", "\u00dc" );
        defaultEntityMap.put( "Yacute", "\u00dd" );
        defaultEntityMap.put( "THORN", "\u00de" );
        defaultEntityMap.put( "szlig", "\u00df" );
        defaultEntityMap.put( "agrave", "\u00e0" );
        defaultEntityMap.put( "aacute", "\u00e1" );
        defaultEntityMap.put( "acirc", "\u00e2" );
        defaultEntityMap.put( "atilde", "\u00e3" );
        defaultEntityMap.put( "auml", "\u00e4" );
        defaultEntityMap.put( "aring", "\u00e5" );
        defaultEntityMap.put( "aelig", "\u00e6" );
        defaultEntityMap.put( "ccedil", "\u00e7" );
        defaultEntityMap.put( "egrave", "\u00e8" );
        defaultEntityMap.put( "eacute", "\u00e9" );
        defaultEntityMap.put( "ecirc", "\u00ea" );
        defaultEntityMap.put( "euml", "\u00eb" );
        defaultEntityMap.put( "igrave", "\u00ec" );
        defaultEntityMap.put( "iacute", "\u00ed" );
        defaultEntityMap.put( "icirc", "\u00ee" );
        defaultEntityMap.put( "iuml", "\u00ef" );
        defaultEntityMap.put( "eth", "\u00f0" );
        defaultEntityMap.put( "ntilde", "\u00f1" );
        defaultEntityMap.put( "ograve", "\u00f2" );
        defaultEntityMap.put( "oacute", "\u00f3" );
        defaultEntityMap.put( "ocirc", "\u00f4" );
        defaultEntityMap.put( "otilde", "\u00f5" );
        defaultEntityMap.put( "ouml", "\u00f6" );
        defaultEntityMap.put( "divide", "\u00f7" );
        defaultEntityMap.put( "oslash", "\u00f8" );
        defaultEntityMap.put( "ugrave", "\u00f9" );
        defaultEntityMap.put( "uacute", "\u00fa" );
        defaultEntityMap.put( "ucirc", "\u00fb" );
        defaultEntityMap.put( "uuml", "\u00fc" );
        defaultEntityMap.put( "yacute", "\u00fd" );
        defaultEntityMap.put( "thorn", "\u00fe" );
        defaultEntityMap.put( "yuml", "\u00ff" );

        // ----------------------------------------------------------------------
        // Special entities
        // ----------------------------------------------------------------------

        defaultEntityMap.put( "OElig", "\u0152" );
        defaultEntityMap.put( "oelig", "\u0153" );
        defaultEntityMap.put( "Scaron", "\u0160" );
        defaultEntityMap.put( "scaron", "\u0161" );
        defaultEntityMap.put( "Yuml", "\u0178" );
        defaultEntityMap.put( "circ", "\u02c6" );
        defaultEntityMap.put( "tilde", "\u02dc" );
        defaultEntityMap.put( "ensp", "\u2002" );
        defaultEntityMap.put( "emsp", "\u2003" );
        defaultEntityMap.put( "thinsp", "\u2009" );
        defaultEntityMap.put( "zwnj", "\u200c" );
        defaultEntityMap.put( "zwj", "\u200d" );
        defaultEntityMap.put( "lrm", "\u200e" );
        defaultEntityMap.put( "rlm", "\u200f" );
        defaultEntityMap.put( "ndash", "\u2013" );
        defaultEntityMap.put( "mdash", "\u2014" );
        defaultEntityMap.put( "lsquo", "\u2018" );
        defaultEntityMap.put( "rsquo", "\u2019" );
        defaultEntityMap.put( "sbquo", "\u201a" );
        defaultEntityMap.put( "ldquo", "\u201c" );
        defaultEntityMap.put( "rdquo", "\u201d" );
        defaultEntityMap.put( "bdquo", "\u201e" );
        defaultEntityMap.put( "dagger", "\u2020" );
        defaultEntityMap.put( "Dagger", "\u2021" );
        defaultEntityMap.put( "permil", "\u2030" );
        defaultEntityMap.put( "lsaquo", "\u2039" );
        defaultEntityMap.put( "rsaquo", "\u203a" );
        defaultEntityMap.put( "euro", "\u20ac" );

        // ----------------------------------------------------------------------
        // Symbol entities
        // ----------------------------------------------------------------------

        defaultEntityMap.put( "fnof", "\u0192" );
        defaultEntityMap.put( "Alpha", "\u0391" );
        defaultEntityMap.put( "Beta", "\u0392" );
        defaultEntityMap.put( "Gamma", "\u0393" );
        defaultEntityMap.put( "Delta", "\u0394" );
        defaultEntityMap.put( "Epsilon", "\u0395" );
        defaultEntityMap.put( "Zeta", "\u0396" );
        defaultEntityMap.put( "Eta", "\u0397" );
        defaultEntityMap.put( "Theta", "\u0398" );
        defaultEntityMap.put( "Iota", "\u0399" );
        defaultEntityMap.put( "Kappa", "\u039a" );
        defaultEntityMap.put( "Lambda", "\u039b" );
        defaultEntityMap.put( "Mu", "\u039c" );
        defaultEntityMap.put( "Nu", "\u039d" );
        defaultEntityMap.put( "Xi", "\u039e" );
        defaultEntityMap.put( "Omicron", "\u039f" );
        defaultEntityMap.put( "Pi", "\u03a0" );
        defaultEntityMap.put( "Rho", "\u03a1" );
        defaultEntityMap.put( "Sigma", "\u03a3" );
        defaultEntityMap.put( "Tau", "\u03a4" );
        defaultEntityMap.put( "Upsilon", "\u03a5" );
        defaultEntityMap.put( "Phi", "\u03a6" );
        defaultEntityMap.put( "Chi", "\u03a7" );
        defaultEntityMap.put( "Psi", "\u03a8" );
        defaultEntityMap.put( "Omega", "\u03a9" );
        defaultEntityMap.put( "alpha", "\u03b1" );
        defaultEntityMap.put( "beta", "\u03b2" );
        defaultEntityMap.put( "gamma", "\u03b3" );
        defaultEntityMap.put( "delta", "\u03b4" );
        defaultEntityMap.put( "epsilon", "\u03b5" );
        defaultEntityMap.put( "zeta", "\u03b6" );
        defaultEntityMap.put( "eta", "\u03b7" );
        defaultEntityMap.put( "theta", "\u03b8" );
        defaultEntityMap.put( "iota", "\u03b9" );
        defaultEntityMap.put( "kappa", "\u03ba" );
        defaultEntityMap.put( "lambda", "\u03bb" );
        defaultEntityMap.put( "mu", "\u03bc" );
        defaultEntityMap.put( "nu", "\u03bd" );
        defaultEntityMap.put( "xi", "\u03be" );
        defaultEntityMap.put( "omicron", "\u03bf" );
        defaultEntityMap.put( "pi", "\u03c0" );
        defaultEntityMap.put( "rho", "\u03c1" );
        defaultEntityMap.put( "sigmaf", "\u03c2" );
        defaultEntityMap.put( "sigma", "\u03c3" );
        defaultEntityMap.put( "tau", "\u03c4" );
        defaultEntityMap.put( "upsilon", "\u03c5" );
        defaultEntityMap.put( "phi", "\u03c6" );
        defaultEntityMap.put( "chi", "\u03c7" );
        defaultEntityMap.put( "psi", "\u03c8" );
        defaultEntityMap.put( "omega", "\u03c9" );
        defaultEntityMap.put( "thetasym", "\u03d1" );
        defaultEntityMap.put( "upsih", "\u03d2" );
        defaultEntityMap.put( "piv", "\u03d6" );
        defaultEntityMap.put( "bull", "\u2022" );
        defaultEntityMap.put( "hellip", "\u2026" );
        defaultEntityMap.put( "prime", "\u2032" );
        defaultEntityMap.put( "Prime", "\u2033" );
        defaultEntityMap.put( "oline", "\u203e" );
        defaultEntityMap.put( "frasl", "\u2044" );
        defaultEntityMap.put( "weierp", "\u2118" );
        defaultEntityMap.put( "image", "\u2111" );
        defaultEntityMap.put( "real", "\u211c" );
        defaultEntityMap.put( "trade", "\u2122" );
        defaultEntityMap.put( "alefsym", "\u2135" );
        defaultEntityMap.put( "larr", "\u2190" );
        defaultEntityMap.put( "uarr", "\u2191" );
        defaultEntityMap.put( "rarr", "\u2192" );
        defaultEntityMap.put( "darr", "\u2193" );
        defaultEntityMap.put( "harr", "\u2194" );
        defaultEntityMap.put( "crarr", "\u21b5" );
        defaultEntityMap.put( "lArr", "\u21d0" );
        defaultEntityMap.put( "uArr", "\u21d1" );
        defaultEntityMap.put( "rArr", "\u21d2" );
        defaultEntityMap.put( "dArr", "\u21d3" );
        defaultEntityMap.put( "hArr", "\u21d4" );
        defaultEntityMap.put( "forall", "\u2200" );
        defaultEntityMap.put( "part", "\u2202" );
        defaultEntityMap.put( "exist", "\u2203" );
        defaultEntityMap.put( "empty", "\u2205" );
        defaultEntityMap.put( "nabla", "\u2207" );
        defaultEntityMap.put( "isin", "\u2208" );
        defaultEntityMap.put( "notin", "\u2209" );
        defaultEntityMap.put( "ni", "\u220b" );
        defaultEntityMap.put( "prod", "\u220f" );
        defaultEntityMap.put( "sum", "\u2211" );
        defaultEntityMap.put( "minus", "\u2212" );
        defaultEntityMap.put( "lowast", "\u2217" );
        defaultEntityMap.put( "radic", "\u221a" );
        defaultEntityMap.put( "prop", "\u221d" );
        defaultEntityMap.put( "infin", "\u221e" );
        defaultEntityMap.put( "ang", "\u2220" );
        defaultEntityMap.put( "and", "\u2227" );
        defaultEntityMap.put( "or", "\u2228" );
        defaultEntityMap.put( "cap", "\u2229" );
        defaultEntityMap.put( "cup", "\u222a" );
        defaultEntityMap.put( "int", "\u222b" );
        defaultEntityMap.put( "there4", "\u2234" );
        defaultEntityMap.put( "sim", "\u223c" );
        defaultEntityMap.put( "cong", "\u2245" );
        defaultEntityMap.put( "asymp", "\u2248" );
        defaultEntityMap.put( "ne", "\u2260" );
        defaultEntityMap.put( "equiv", "\u2261" );
        defaultEntityMap.put( "le", "\u2264" );
        defaultEntityMap.put( "ge", "\u2265" );
        defaultEntityMap.put( "sub", "\u2282" );
        defaultEntityMap.put( "sup", "\u2283" );
        defaultEntityMap.put( "nsub", "\u2284" );
        defaultEntityMap.put( "sube", "\u2286" );
        defaultEntityMap.put( "supe", "\u2287" );
        defaultEntityMap.put( "oplus", "\u2295" );
        defaultEntityMap.put( "otimes", "\u2297" );
        defaultEntityMap.put( "perp", "\u22a5" );
        defaultEntityMap.put( "sdot", "\u22c5" );
        defaultEntityMap.put( "lceil", "\u2308" );
        defaultEntityMap.put( "rceil", "\u2309" );
        defaultEntityMap.put( "lfloor", "\u230a" );
        defaultEntityMap.put( "rfloor", "\u230b" );
        defaultEntityMap.put( "lang", "\u2329" );
        defaultEntityMap.put( "rang", "\u232a" );
        defaultEntityMap.put( "loz", "\u25ca" );
        defaultEntityMap.put( "spades", "\u2660" );
        defaultEntityMap.put( "clubs", "\u2663" );
        defaultEntityMap.put( "hearts", "\u2665" );
        defaultEntityMap.put( "diams", "\u2666" );
    }

    public static String resolveEntity( String entity )
    {
        if ( entity == null )
        {
            // Invalid. null.
            return entity;
        }

        if ( entity.trim().length() <= 0 )
        {
            // Invalid. empty.
            return entity;
        }

        if ( !( entity.charAt( 0 ) == '&' ) && ( entity.charAt( entity.length() ) == ';' ) )
        {
            // Invalid, not an entity.
            return entity;
        }

        String result = (String) defaultEntityMap.get( entity.substring( 1, entity.length() - 1 ) );
        if ( result == null )
        {
            return entity;
        }

        return result;
    }
}
