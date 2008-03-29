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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <p>The <a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * transactions processor.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVProcessor {

    /** <p>All the implemented methods, comma separated.</p> */
    public static final String METHODS = "COPY,DELETE,GET,HEAD,MKCOL,MOVE," + 
                                         "OPTIONS,PROPFIND,PROPPATCH,PUT";

    /** <p>A static map of all known webdav methods.</p> */
    private static Map INSTANCES = new HashMap();
    static {
        /* Load and verify all the known methods */
        final String thisName = DAVProcessor.class.getName();
        final int packageDelimiter = thisName.lastIndexOf('.');
        final String packageName = packageDelimiter < 1 ? "methods." :
                        thisName.substring(0, packageDelimiter) + ".methods.";
        final StringTokenizer tokenizer = new StringTokenizer(METHODS, ",");
        final ClassLoader classLoader = DAVProcessor.class.getClassLoader();
        while (tokenizer.hasMoreTokens()) try {
            final String method = tokenizer.nextToken();
            final String className = packageName + method;
            final Class clazz = classLoader.loadClass(className);
            INSTANCES.put(method, (DAVMethod) clazz.newInstance());
        } catch (Throwable throwable) {
            InternalError error = new InternalError("Error loading method");
            throw (InternalError) error.initCause(throwable);
        }
    }

    /** <p>The {@link DAVRepository} associated with this instance.</p> */
    private DAVRepository repository = null;

    /**
     * <p>Create a new {@link DAVProcessor} instance.</p>
     */
    public DAVProcessor(DAVRepository repository) {
        if (repository == null) throw new NullPointerException();
        this.repository = repository;
    }

    /**
     * <p>Process the specified {@link DAVTransaction} fully.</p>
     */
    public void process(DAVTransaction transaction)
    throws IOException {
        try {
            String method = transaction.getMethod();
            if (INSTANCES.containsKey(method)) {
                String path = transaction.getNormalizedPath();
                DAVResource resource = this.repository.getResource(path);
                DAVMethod instance = ((DAVMethod) INSTANCES.get(method));
                instance.process(transaction, resource);
            } else {
                String message = "Method \"" + method + "\" not implemented";
                throw new DAVException(501, message);
            }
        } catch (DAVException exception) {
            exception.write(transaction);
        }
    }
    
    public void setMethod( String methodKey, DAVMethod method ) {
        INSTANCES.put( methodKey, method );
    }
}
