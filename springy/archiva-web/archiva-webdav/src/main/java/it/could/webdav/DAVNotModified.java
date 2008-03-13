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

/**
 * <p>A simple {@link DAVException} encapsulating an
 * <a href="http://www.rfc-editor.org/rfc/rfc2616.txt">HTTP</a> not modified
 * response.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVNotModified extends DAVException {

    private DAVResource resource = null;

    /**
     * <p>Create a new {@link DAVNotModified} instance.</p>
     */
    public DAVNotModified(DAVResource resource) {
        super(304, "Resource Not Modified");
        this.resource = resource;
    }

    /**
     * <p>Write the body of this {@link DAVNotModified} to the specified
     * {@link DAVTransaction}'s output.</p>
     */
    public void write(DAVTransaction transaction)
    throws IOException {
        transaction.setStatus(this.getStatus());

        /* Figure out what we're dealing with here */
        String etag = resource.getEntityTag();
        String lmod = DAVUtilities.formatHttpDate(resource.getLastModified());

        /* Set the normal headers that are required for a GET */
        if (etag != null) transaction.setHeader("ETag", etag);
        if (lmod != null) transaction.setHeader("Last-Modified", lmod);
    }
}
