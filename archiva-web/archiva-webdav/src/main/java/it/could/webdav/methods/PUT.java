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
import it.could.webdav.DAVOutputStream;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;

import java.io.IOException;
import java.io.InputStream;


/**
 * <p><a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * <code>PUT</code> metohd implementation.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class PUT implements DAVMethod {

    /**
     * <p>Create a new {@link PUT} instance.</p>
     */
    public PUT() {
        super();
    }

    /**
     * <p>Process the <code>PUT</code> method.</p>
     */
    public void process(DAVTransaction transaction, DAVResource resource)
    throws IOException {
    	/* 
    	* The HTTP status code will depend on the existance of the resource:
    	* if not found: HTTP/1.1 201 Created
    	* if existing:  HTTP/1.1 204 No Content
    	*/
    	transaction.setStatus(resource.isNull()? 201: 204);

        /* Open the streams for reading and writing */
        InputStream in = transaction.read();
        if (in == null) throw new DAVException(411, "Content-Length required");
        DAVOutputStream out = resource.write();

        /* Write the content from the PUT to the specified resource */
        try {
            byte buffer[] = new byte[4096];
            int k = -1;
            while ((k = in.read(buffer)) != -1) out.write(buffer, 0, k);
            in.close();            
            out.close();
        } finally {
            out.abort();
        }
    }
}
