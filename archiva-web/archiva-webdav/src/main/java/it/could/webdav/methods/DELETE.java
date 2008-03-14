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

import it.could.webdav.DAVMethod;
import it.could.webdav.DAVMultiStatus;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;

import java.io.IOException;


/**
 * <p><a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * <code>DELETE</code> metohd implementation.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DELETE implements DAVMethod {

    /**
     * <p>Create a new {@link DELETE} instance.</p>
     */
    public DELETE() {
        super();
    }

    /**
     * <p>Process the <code>DELETE</code> method.</p>
     */
    public void process(DAVTransaction transaction, DAVResource resource)
    throws IOException {
        try {
            resource.delete();
            transaction.setStatus(204);
        } catch (DAVMultiStatus multistatus) {
            multistatus.write(transaction);
        }
    }
}
