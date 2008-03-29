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
package it.could.webdav.methods;

import it.could.webdav.DAVInputStream;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;


/**
 * <p><a href="http://www.rfc-editor.org/rfc/rfc2616.txt">HTTP</a>
 * <code>GET</code> metohd implementation.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class GET extends HEAD {
    
    /** <p>The encoding charset to repsesent collections.</p> */
    public static final String ENCODING = "UTF-8";

    /** <p>The mime type that {@link GET} will use serving collections.</p> */ 
    public static final String COLLECTION_MIME_TYPE = "text/html ;charset=\""
                                                      + ENCODING + "\"";

    /**
     * <p>Create a new {@link GET} instance.</p>
     */
    public GET() {
        super();
    }

    /**
     * <p>Process the <code>GET</code> method.</p>
     */
    public void process(DAVTransaction transaction, DAVResource resource)
    throws IOException {
        super.process(transaction, resource);

        final String originalPath = transaction.getOriginalPath();
        final String normalizedPath = transaction.getNormalizedPath();
        final String current;
        final String parent;
        if (originalPath.equals(normalizedPath)) {
            final String relativePath = resource.getRelativePath();
            if (relativePath.equals("")) {
                current = transaction.lookup(resource).toASCIIString();
            } else {
                current = relativePath;
            }
            parent = "./";
        } else {
            current = "./";
            parent = "../";
        }

        if (resource.isCollection()) {
            transaction.setHeader( "Content-Disposition", "inline; filename=\"index.html\"");
            PrintWriter out = transaction.write(ENCODING);
            String path = resource.getRelativePath();
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Collection: /" + path + "</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h2>Collection: /" + path + "</h2>");
            out.println("<ul>");

            /* Process the parent */
            final DAVResource parentResource = resource.getParent(); 
            if (parentResource != null) {
                out.print("<li><a href=\"");
                out.print(parent);
                out.print("\">");
                out.print(parentResource.getDisplayName());
                out.println("</a> <i><small>(Parent)</small></i></li>");
                out.println("</ul>");
                out.println("<ul>");
            }

            /* Process the children (in two sorted sets, for nice ordering) */
            Set resources = new TreeSet();
            Set collections = new TreeSet();
            Iterator iterator = resource.getChildren();
            while (iterator.hasNext()) {
                final DAVResource child = (DAVResource) iterator.next();
                final StringBuffer buffer = new StringBuffer();
                final String childPath = child.getDisplayName();
                buffer.append("<li><a href=\"");
                buffer.append(current);
                buffer.append(childPath);
                buffer.append("\">");
                buffer.append(childPath);
                buffer.append("</li>");
                if (child.isCollection()) {
                    collections.add(buffer.toString());
                } else {
                    resources.add(buffer.toString());
                }
            }

            /* Spit out the collections first and the resources then */
            for (Iterator i = collections.iterator(); i.hasNext(); )
                out.println(i.next());
            for (Iterator i = resources.iterator(); i.hasNext(); )
                out.println(i.next());

            out.println("</ul>");
            out.println("</body>");
            out.println("</html>");
            out.flush();
            return;
        }
        
        /* Processing a normal resource request */
        OutputStream out = transaction.write();
        DAVInputStream in = resource.read();
        byte buffer[] = new byte[4096];
        int k = -1;
        while ((k = in.read(buffer)) != -1) out.write(buffer, 0, k);
        in.close();
        out.flush();
    }
}
