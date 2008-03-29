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

import it.could.webdav.DAVException;
import it.could.webdav.DAVMethod;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;
import it.could.webdav.DAVUtilities;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;


/**
 * <p><a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * <code>PROPFIND</code> metohd implementation.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class PROPFIND implements DAVMethod {

    /**
     * <p>Create a new {@link PROPFIND} instance.</p>
     */
    public PROPFIND() {
        super();
    }

    /**
     * <p>Process the <code>PROPFIND</code> method.</p>
     */
    public void process(DAVTransaction transaction, DAVResource resource)
    throws IOException {
        /* Check if we have to force a resource not found or a redirection */
        if (resource.isNull())
            throw new DAVException(404, "Not found", resource);

        /* Check depth */
        int depth = transaction.getDepth();
        if (depth > 1) new DAVException(403, "Invalid depth");

        /* What to do on a collection resource */
        transaction.setStatus(207);
        transaction.setContentType("text/xml; charset=\"UTF-8\"");
        PrintWriter out = transaction.write("UTF-8");

        /* Output the XML declaration and the root document tag */
        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<D:multistatus xmlns:D=\"DAV:\">");

        /* Process this resource's property (always) */
        this.process(transaction, out, resource);

        /* Process this resource's children (if required) */
        if (resource.isCollection() && (depth > 0)) {
            Iterator children = resource.getChildren();
            while (children.hasNext()) {
                DAVResource child = (DAVResource) children.next();
                this.process(transaction, out, child);
            }
        }

        /* Close up the XML Multi-Status response */
        out.println("</D:multistatus>");
        out.flush();
    }

    private void process(DAVTransaction txn, PrintWriter out, DAVResource res) {
        /* The href of the resource is only the absolute path */
        out.println(" <D:response>");
        out.println("  <D:href>" + txn.lookup(res).getPath() + "</D:href>");
        out.println("  <D:propstat>");
        out.println("   <D:prop>");

        /* Figure out what we're dealing with here */
        if (res.isCollection()) { 
            this.process(out, "resourcetype", "<D:collection/>");
        }
        this.process(out, "getcontenttype", res.getContentType());

        this.process(out, "getetag", res.getEntityTag());
        String date = DAVUtilities.formatIsoDate(res.getCreationDate());
        this.process(out, "creationdate", date);
        String lmod = DAVUtilities.formatHttpDate(res.getLastModified());
        this.process(out, "getlastmodified", lmod);
        String clen = DAVUtilities.formatNumber(res.getContentLength());
        this.process(out, "getcontentlength", clen);

        out.println("   </D:prop>");
        out.println("   <D:status>HTTP/1.1 200 OK</D:status>");
        out.println("  </D:propstat>");
        out.println(" </D:response>");
    }

    private void process(PrintWriter out, String name, String value) {
        if (value == null) return;
        out.print("    <D:");
        out.print(name);
        out.print(">");
        out.print(value);
        out.print("</D:");
        out.print(name);
        out.println(">");
    }
    
}
