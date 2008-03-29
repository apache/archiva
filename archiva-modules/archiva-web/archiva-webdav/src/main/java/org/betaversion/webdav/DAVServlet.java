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
package org.betaversion.webdav;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * <p>The {@link DAVServlet} class has been moved to a new package and should
 * now be referred as {@link it.could.webdav.DAVServlet}.</p>
 * 
 * <p>This class will be preserved for some time (not so long) to give people
 * time to update their servlet deployment descriptors.</p>
 * 
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 * @deprecated This class has been moved into the <code>it.could.webdav</code>
 *             package. Reconfigure your <code>web.xml</code> deployment
 *             descriptor to use {@link it.could.webdav.DAVServlet}.
 */
public class DAVServlet extends it.could.webdav.DAVServlet {

    /**
     * <p>Create a new {@link DAVServlet} instance.</p>
     */
    public DAVServlet() {
        super();
    }
    
    /**
     * <p>Initialize this {@link DAVServlet} instance reporting to the
     * {@link ServletContext} log that this class is deprecated.</p>
     */
    public void init(ServletConfig config)
    throws ServletException {
        final ServletContext context = config.getServletContext();
        context.log("The class \"" + this.getClass().getName()
                    + "\" is deprecated");
        context.log("Modify the \"web.xml\" deployment descriptor to use \""
                    + it.could.webdav.DAVServlet.class.getName() + "\"");
        super.init(config);
    }
}
