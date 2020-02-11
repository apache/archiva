package org.apache.archiva.common.utils;

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

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.stream.StreamSupport;

/**
 *
 * PathUtil - simple utility methods for path manipulation.
 *
 * Some code is from Apache Ant SelectorUtils
 *
 */
public class PathUtil
{

    public static final String DEEP_TREE_MATCH = "**";

    public static String toUrl( String path )
    {
        // Is our work already done for us?
        if ( path.startsWith( "file:/" ) )
        {
            return path;
        }

        return toUrl( Paths.get( path ) );
    }

    public static String toUrl( Path file )
    {
        try
        {
            return file.toUri().toURL().toExternalForm();
        }
        catch ( MalformedURLException e )
        {
            String pathCorrected = StringUtils.replaceChars( file.toAbsolutePath().toString(), '\\', '/' );
            if ( pathCorrected.startsWith( "file:/" ) )
            {
                return pathCorrected;
            }

            return "file://" + pathCorrected;
        }
    }

    /**
     * Given a basedir and a child file, return the relative path to the child.
     *
     * @param basedir the basedir.
     * @param file    the file to get the relative path for.
     * @return the relative path to the child. (NOTE: this path will NOT start with a file separator character)
     */
    public static String getRelative( Path basedir, Path file )
    {
        if (basedir.isAbsolute() && !file.isAbsolute()) {
            return basedir.normalize().relativize(file.toAbsolutePath()).toString();
        } else if (!basedir.isAbsolute() && file.isAbsolute()) {
            return basedir.toAbsolutePath().relativize(file.normalize()).toString();
        } else {
            return basedir.normalize().relativize(file.normalize()).toString();
        }
    }

    public static String getRelative(String basedir, Path file) {
        return getRelative(Paths.get(basedir), file);
    }

    /**
     * Given a basedir and a child file, return the relative path to the child.
     *
     * @param basedir the basedir.
     * @param child   the child path (can be a full path)
     * @return the relative path to the child. (NOTE: this path will NOT start with a file separator character)
     */
    public static String getRelative( String basedir, String child )
    {

        return getRelative(basedir, Paths.get(child));
    }

    /**
     * Returns a path object from the given URI. If the URI has no scheme, the path of the URI is used
     * for creating the filesystem path.
     *
     * @param uri the uri to convert
     * @return a path object with the given path
     * @throws java.nio.file.FileSystemNotFoundException if the uri scheme is not known.
     */
    public static Path getPathFromUri( URI uri) {
        if (uri==null) {
            return Paths.get("");
        } else if (uri.getScheme()==null) {
            return Paths.get(uri.getPath());
        } else {
            return Paths.get(uri);
        }
    }

    public static boolean isAbsolutePath(String path) {
        try
        {
            return Paths.get( path ).isAbsolute( );
        } catch (Exception e) {
            return false;
        }
    }

    public static char getSeparatorChar() {
        return FileSystems.getDefault( ).getSeparator( ).charAt( 0 );
    }

    public static String[] dissect(String pathString) {
        Path path = Paths.get(pathString);
        return StreamSupport.stream(path.spliterator(), false).map(Path::toString)
            .toArray(String[]::new);
    }

    public static String separatorsToUnix(String path)
    {
        return path != null && path.indexOf( 92 ) != -1 ? path.replace( '\\', '/' ) : path;
    }


    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * If you need to call this method multiple times with the same
     * pattern you should rather use TokenizedPath
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    public static boolean matchPath(String pattern, String str) {
        String[] patDirs = tokenizePathAsArray(pattern);
        return matchPath(patDirs, tokenizePathAsArray(str), true);
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * If you need to call this method multiple times with the same
     * pattern you should rather use TokenizedPattern
     *
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    public static boolean matchPath(String pattern, String str,
                                    boolean isCaseSensitive) {
        String[] patDirs = tokenizePathAsArray(pattern);
        return matchPath(patDirs, tokenizePathAsArray(str), isCaseSensitive);
    }

    /**
     * Core implementation of matchPath.  It is isolated so that it
     * can be called from TokenizedPattern.
     */
    static boolean matchPath(String[] tokenizedPattern, String[] strDirs,
                             boolean isCaseSensitive) {
        int patIdxStart = 0;
        int patIdxEnd = tokenizedPattern.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.length - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = tokenizedPattern[patIdxStart];
            if (patDir.equals(DEEP_TREE_MATCH)) {
                break;
            }
            if (!match(patDir, strDirs[strIdxStart], isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!tokenizedPattern[i].equals(DEEP_TREE_MATCH)) {
                    return false;
                }
            }
            return true;
        }
        if (patIdxStart > patIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = tokenizedPattern[patIdxEnd];
            if (patDir.equals(DEEP_TREE_MATCH)) {
                break;
            }
            if (!match(patDir, strDirs[strIdxEnd], isCaseSensitive)) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!tokenizedPattern[i].equals(DEEP_TREE_MATCH)) {
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (tokenizedPattern[i].equals(DEEP_TREE_MATCH)) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = tokenizedPattern[patIdxStart + j + 1];
                    String subStr = strDirs[strIdxStart + i + j];
                    if (!match(subPat, subStr, isCaseSensitive)) {
                        continue strLoop;
                    }
                }
                foundIdx = strIdxStart + i;
                break;
            }
            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!DEEP_TREE_MATCH.equals(tokenizedPattern[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match(String pattern, String str) {
        return match(pattern, str, true);
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @param caseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match(String pattern, String str,
                                boolean caseSensitive) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;

        boolean containsStar = false;
        for (char ch : patArr) {
            if (ch == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                char ch = patArr[i];
                if (ch != '?' && different(caseSensitive, ch, strArr[i])) {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while (true) {
            char ch = patArr[patIdxStart];
            if (ch == '*' || strIdxStart > strIdxEnd) {
                break;
            }
            if (ch != '?'
                && different(caseSensitive, ch, strArr[strIdxStart])) {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            return allStars(patArr, patIdxStart, patIdxEnd);
        }

        // Process characters after last star
        while (true) {
            char ch = patArr[patIdxEnd];
            if (ch == '*' || strIdxStart > strIdxEnd) {
                break;
            }
            if (ch != '?' && different(caseSensitive, ch, strArr[strIdxEnd])) {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            return allStars(patArr, patIdxStart, patIdxEnd);
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    char ch = patArr[patIdxStart + j + 1];
                    if (ch != '?' && different(caseSensitive, ch,
                        strArr[strIdxStart + i + j])) {
                        continue strLoop;
                    }
                }
                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }
            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        return allStars(patArr, patIdxStart, patIdxEnd);
    }

    private static boolean allStars(char[] chars, int start, int end) {
        for (int i = start; i <= end; ++i) {
            if (chars[i] != '*') {
                return false;
            }
        }
        return true;
    }

    private static boolean different(
        boolean caseSensitive, char ch, char other) {
        return caseSensitive
            ? ch != other
            : Character.toUpperCase(ch) != Character.toUpperCase(other);
    }

    /**
     * Breaks a path up into a Vector of path elements, tokenizing on
     * <code>File.separator</code>.
     *
     * @param path Path to tokenize. Must not be <code>null</code>.
     *
     * @return a Vector of path elements from the tokenized path
     */
    public static Vector<String> tokenizePath( String path) {
        return tokenizePath(path, FileSystems.getDefault( ).getSeparator());
    }

    /**
     * Breaks a path up into a Vector of path elements, tokenizing on
     *
     * @param path Path to tokenize. Must not be <code>null</code>.
     * @param separator the separator against which to tokenize.
     *
     * @return a Vector of path elements from the tokenized path
     * @since Ant 1.6
     */
    public static Vector<String> tokenizePath(String path, String separator) {
        Vector<String> ret = new Vector<>();
        if (isAbsolutePath(path)) {
            String[] s = dissect(path);
            ret.add(s[0]);
            path = s[1];
        }
        StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreTokens()) {
            ret.addElement(st.nextToken());
        }
        return ret;
    }

    /**
     * Same as {@link #tokenizePath tokenizePath} but hopefully faster.
     */
    /* package */
    static String[] tokenizePathAsArray(String path) {
        String root = null;
        if (isAbsolutePath(path)) {
            String[] s = dissect(path);
            root = s[0];
            path = s[1];
        }
        char sep = getSeparatorChar();
        int start = 0;
        int len = path.length();
        int count = 0;
        for (int pos = 0; pos < len; pos++) {
            if (path.charAt(pos) == sep) {
                if (pos != start) {
                    count++;
                }
                start = pos + 1;
            }
        }
        if (len != start) {
            count++;
        }
        String[] l = new String[count + ((root == null) ? 0 : 1)];

        if (root != null) {
            l[0] = root;
            count = 1;
        } else {
            count = 0;
        }
        start = 0;
        for (int pos = 0; pos < len; pos++) {
            if (path.charAt(pos) == sep) {
                if (pos != start) {
                    String tok = path.substring(start, pos);
                    l[count++] = tok;
                }
                start = pos + 1;
            }
        }
        if (len != start) {
            String tok = path.substring(start);
            l[count/*++*/] = tok;
        }
        return l;
    }


}
