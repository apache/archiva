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


/**
 * <p>A {@link RuntimeException} representing a
 * <a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * response for a specified {@link DAVResource}.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVException extends RuntimeException {
    
    private DAVResource resource = null;
    private int status = 0;

    /**
     * <p>Create a new {@link DAVException} instance.</p>
     */
    public DAVException(int status, String message) {
        this(status, message, null, null);
    }

    /**
     * <p>Create a new {@link DAVException} instance.</p>
     */
    public DAVException(int status, String message, Throwable throwable) {
        this(status, message, throwable, null);
    }

    /**
     * <p>Create a new {@link DAVException} instance.</p>
     */
    public DAVException(int status, String message, DAVResource resource) {
        this(status, message, null, resource);
    }

    /**
     * <p>Create a new {@link DAVException} instance.</p>
     */
    public DAVException(int s, String m, Throwable t, DAVResource r) {
        super(m, t);
        this.resource = r;
        this.status = s;
    }

    /**
     * <p>Return the status code associated with this instance.</p>
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * <p>Return the {@link DAVResource} associated with this instance.</p>
     */
    public DAVResource getResource() {
        return this.resource;
    }

    /**
     * <p>Write the body of this {@link DAVException} to the specified
     * {@link DAVTransaction}'s output.</p>
     */
    public void write(DAVTransaction transaction)
    throws IOException {
        transaction.setContentType("text/html; charset=\"UTF-8\"");
        transaction.setStatus(this.getStatus());

        /* Prepare and log the error message */
        String message = DAVUtilities.getStatusMessage(this.getStatus()); 
        if (message == null) {
            transaction.setStatus(500);
            message = Integer.toString(this.getStatus()) + " Unknown";
        }

        /* Write the error message to the client */
        PrintWriter out = transaction.write("UTF-8");
        out.println("<html>");
        out.print("<head><title>Error ");
        out.print(message);
        out.println("</title></head>");
        out.println("<body>");
        out.print("<p><b>Error ");
        out.print(message);
        out.println("</b></p>");
        
        /* Check if we have a resource associated with the extension */
        if (this.getResource() != null) {
            String r = transaction.lookup(this.getResource()).toASCIIString();
            out.print("<p>Resource in error: <a href=\"");
            out.print(r);
            out.println("\">");
            out.print(r);
            out.println("</a></p>");
        }

        /* Process any exception and its cause */
        Throwable throwable = this;
        out.println("<hr /><p>Exception details:</p>");
        while (throwable != null) {
            out.print("<pre>");
            throwable.printStackTrace(out);
            out.println("</pre>");
            throwable = throwable.getCause();
            if (throwable != null) out.println("<hr /><p>Caused by:</p>");
        }

        /* Close up the HTML */
        out.println("</body>");
        out.println("</html>");
        out.flush();
    }
}
