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
import it.could.webdav.DAVProcessor;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;

import java.io.IOException;


/**
 * <p><a href="http://www.rfc-editor.org/rfc/rfc2616.txt">HTTP</a>
 * <code>OPTIONS</code> metohd implementation.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class OPTIONS implements DAVMethod {

    /**
     * <p>Create a new {@link OPTIONS} instance.</p>
     */
    public OPTIONS() {
        super();
    }

    /**
     * <p>Process the <code>OPTIONS</code> method.</p>
     */
    public void process(DAVTransaction transaction, DAVResource resource)
    throws IOException {
        transaction.setHeader("Content-Type", resource.getContentType());
        transaction.setHeader("Allow", DAVProcessor.METHODS);
        transaction.setStatus(200);
    }

}
