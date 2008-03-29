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
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * <p>A {@link DAVException} representing a
 * <a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * <code>207</code> (Multi-Status) response.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVMultiStatus extends DAVException {
    
    private Set responses = new HashSet();

    /**
     * <p>Create a new {@link DAVMultiStatus} instance.</p>
     */
    public DAVMultiStatus() {
        super(207, "Multi-Status response");
    }

    /**
     * <p>Write the body of the multi-status response to the specified
     * {@link DAVTransaction}'s output.</p>
     */
    public void write(DAVTransaction transaction)
    throws IOException {
        /* What to do on a collection resource */
        transaction.setStatus(207);
        transaction.setContentType("text/xml; charset=\"UTF-8\"");
        PrintWriter out = transaction.write("UTF-8");

        /* Output the XML declaration and the root document tag */
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<D:multistatus xmlns:D=\"DAV:\">");
        
        Iterator responses = this.responses.iterator();
        while (responses.hasNext()) {
            Response response = (Response) responses.next();
            out.println(" <D:response>");
            out.print("  <D:href>");
            out.print(transaction.lookup(response.resource));
            out.println("</D:href>");

            if (response.status != 0) {
                out.print("  <D:status>HTTP/1.1 ");
                out.print(DAVUtilities.getStatusMessage(response.status));
                out.println("</D:status>");
            }

            if (response.message != null) {
                out.print("  <D:responsedescription>");
                out.print(response.message);
                out.println("</D:responsedescription>");
            }

            out.println(" </D:response>");
        }
        
        out.println("</D:multistatus>");
        out.flush();
    }

    /**
     * <p>Return the number of responses held in this instance.</p>
     */
    public int size() {
        return this.responses.size();
    }

    /**
     * <p>Merge the responses held into the specified {@link DAVMultiStatus}
     * into this instance.</p>
     */
    public void merge(DAVMultiStatus multistatus) {
        if (multistatus == null) return;
        Iterator iterator = multistatus.responses.iterator();
        while (iterator.hasNext()) this.responses.add(iterator.next());
    }

    /**
     * <p>Merge the details held into the specified {@link DAVException}
     * into this instance.</p>
     */
    public void merge(DAVException exception) {
        DAVResource resource = exception.getResource();
        if (resource == null) throw exception;

        int status = exception.getStatus();
        String message = exception.getMessage();
        this.responses.add(new Response(resource, status, message));
    }

    private static class Response implements Comparable {
        private DAVResource resource = null;
        private int status = 0;
        private String message = null;

        public Response(Response response) {
            this(response.resource, response.status, response.message);
        }

        public Response(DAVResource resource, int status, String message) {
            if (resource == null) throw new NullPointerException();
            this.resource = resource;
            this.status = status;
            this.message = message;
        }

        public int hashCode() {
            return this.resource.hashCode();
        }

        public int compareTo(Object object) {
            Response response = (Response) object;
            return (this.resource.compareTo(response.resource));
        }

        public boolean equals(Object object) {
            if (object instanceof Response) {
                Response response = (Response) object;
                return (this.resource.equals(response.resource));
            }
            return false;
        }
    }
}
