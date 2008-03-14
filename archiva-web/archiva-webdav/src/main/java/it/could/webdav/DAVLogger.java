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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * <p>A simplicisting class defining an esay way to log stuff to the
 * {@link ServletContext}.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVLogger {
    
    private final ServletContext context;
    private final String servletName;
    private final boolean debug;

    /**
     * <p>Create a new {@link DAVLogger} from a {@link ServletConfig}.</p>
     */
    public DAVLogger(ServletConfig config, boolean debug) {
        this.context = config.getServletContext();
        this.servletName = config.getServletName();
        this.debug = debug;
    }

    /**
     * <p>Log a debug message to the context logger.</p>
     */
    public void debug(String message) {
        if (this.debug) this.doLog(message, null);
    }

    /**
     * <p>Log a debug message and related exception to the context logger.</p>
     */
    public void debug(String message, Throwable throwable) {
        if (this.debug) this.doLog(message, throwable);
    }

    /**
     * <p>Log a message to the context logger.</p>
     */
    public void log(String message) {
        this.doLog(message, null);
    }

    /**
     * <p>Log a message and related exception to the context logger.</p>
     */
    public void log(String message, Throwable throwable) {
        this.doLog(message, throwable);
    }

    /**
     * <p>Internal method for formatting messages and logging.</p>
     */
    private void doLog(String message, Throwable throwable) {
        if ((message == null) && (throwable == null)) return;
        if ((message == null) || ("".equals(message))) message = "No message";

        StringBuffer buffer = new StringBuffer();
        buffer.append('[');
        buffer.append(this.servletName);
        buffer.append("] ");
        buffer.append(message);
        if (throwable == null) this.context.log(buffer.toString());
        else this.context.log(buffer.toString(), throwable);
    }
}
