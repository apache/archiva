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

import java.io.IOException;


/**
 * <p><a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * <code>PROPPATCH</code> metohd implementation.</p>
 * 
 * <p>As this servlet does not handle the creation of custom properties, this
 * method will always fail with a <code>403</code> (Forbidden).</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class PROPPATCH implements DAVMethod {

    /**
     * <p>Create a new {@link PROPPATCH} instance.</p>
     */
    public PROPPATCH() {
        super();
    }

    /**
     * <p>Process the <code>PROPPATCH</code> method.</p>
     * 
     * <p>As this servlet does not handle the creation of custom properties,
     * this method will always fail with a <code>403</code> (Forbidden).</p>
     */
    public void process(DAVTransaction transaction, DAVResource resource)
    throws IOException {
        throw new DAVException(403, "All properties are immutable");
    }
}
