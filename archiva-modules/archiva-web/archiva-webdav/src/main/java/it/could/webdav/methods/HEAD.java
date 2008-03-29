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
import it.could.webdav.DAVNotModified;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;
import it.could.webdav.DAVUtilities;

import java.io.IOException;
import java.util.Date;


/**
 * <p><a href="http://www.rfc-editor.org/rfc/rfc2616.txt">HTTP</a>
 * <code>HEAD</code> metohd implementation.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class HEAD implements DAVMethod {

    /**
     * <p>Create a new {@link HEAD} instance.</p>
     */
    public HEAD() {
        super();
    }

    /**
     * <p>Process the <code>HEAD</code> method.</p>
     */
    public void process(DAVTransaction transaction, DAVResource resource)
    throws IOException {
        /* Check if we have to force a resource not found or a redirection */
        if (resource.isNull())
            throw new DAVException(404, "Not found", resource);

        /* Check if this is a conditional (processable only for resources) */
        Date ifmod = transaction.getIfModifiedSince();
        Date lsmod = resource.getLastModified();
        if (resource.isResource() && (ifmod != null) && (lsmod != null)) {
            /* HTTP doesn't send milliseconds, but Java does, so, reset them */
            lsmod = new Date(((long)(lsmod.getTime() / 1000)) * 1000);
            if (!ifmod.before(lsmod)) throw new DAVNotModified(resource);
        }

        /* Get the headers of this method */
        String ctyp = resource.getContentType();
        String etag = resource.getEntityTag();
        String lmod = DAVUtilities.formatHttpDate(resource.getLastModified());
        String clen = DAVUtilities.formatNumber(resource.getContentLength());

        /* Set the normal headers that are required for a GET */
        if (resource.isCollection()) {
            transaction.setContentType(GET.COLLECTION_MIME_TYPE);
        } else if (ctyp != null) {
            transaction.setContentType(ctyp);
        }
        if (etag != null) transaction.setHeader("ETag", etag);
        if (lmod != null) transaction.setHeader("Last-Modified", lmod);
        if (clen != null) transaction.setHeader("Content-Length", clen);
    }
}
