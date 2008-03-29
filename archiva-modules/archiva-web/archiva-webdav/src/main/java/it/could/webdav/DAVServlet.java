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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * <p>A very simple servlet capable of processing very simple
 * <a href="http://www.rfc-editor.org/rfc/rfc2518.txt">WebDAV</a>
 * requests.</p> 
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVServlet implements Servlet, DAVListener {

    /** <p>The {@link DAVRepository} configured for this instance.</p> */
    protected DAVRepository repository = null;
    /** <p>The {@link DAVLogger} configured for this instance.</p> */
    protected DAVLogger logger = null;
    /** <p>The {@link DAVProcessor} configured for this instance.</p> */
    protected DAVProcessor processor = null;
    /** <p>The {@link ServletContext} associated with this instance.</p> */
    private ServletContext context = null;
    /** <p>The {@link ServletConfig} associated with this instance.</p> */
    private ServletConfig config= null;

    /**
     * <p>Create a new {@link DAVServlet} instance.</p>
     */
    public DAVServlet() {
        super();
    }

    /**
     * <p>Initialize this {@link Servlet} instance.</p>
     * 
     * <p>The only initialization parameter required by this servlet is the
     * &quot;<code>rootPath</code>&quot; parameter specifying the path
     * of the repository root (either absolute or relative to the configured
     * {@link ServletContext}.</p>
     * 
     * <p>If the specified root is relative, it will be considered to
     * be relative to the {@link ServletContext} deployment path.</p>
     * 
     * <p>In any case, the specified root must ultimately point to an existing
     * directory on a locally-accessible file system.</p>
     * 
     * <p>When set to <code>true</code>, an optional parameter called
     * <code>xmlOnly</code> will force this {@link DAVServlet} to use an
     * {@link XMLRepository} instead of the default {@link DAVRepository}.</p>
     *
     * <p>Finally, when set to <code>true</code>, the optional parameter
     * <code>debugEnabled</code> will enable logging of method invocation and
     * events in the repository.</p> 
     */
    public void init(ServletConfig config)
    throws ServletException {
        /* Remember the configuration instance */
        this.config = config;
        this.context = config.getServletContext();
        
        /* Setup logging */
        boolean debug = "true".equals(config.getInitParameter("debugEnabled"));
        this.logger = new DAVLogger(config, debug);

        /* Try to retrieve the WebDAV root path from the configuration */
        String rootPath = config.getInitParameter("rootPath");
        if (rootPath == null)
            throw new ServletException("Parameter \"rootPath\" not specified");
        
        /* Create repository and processor */
        try {
            File root = new File(rootPath);
            // The repository may not be the local filesystem. It may be rooted at "/". 
            // But then on Windows new File("/").isAbsolute() is false.
            boolean unixAbsolute = rootPath.startsWith("/");
            boolean localAbsolute = root.isAbsolute();
            if (! unixAbsolute && !localAbsolute) {
                URL url = this.context.getResource("/" + rootPath);
                if (! "file".equals(url.getProtocol())) {
                    throw new ServletException("Invalid root \"" + url + "\"");
                } else {
                    root = new File(url.getPath());
                }
            }

            /* Discover the repository implementation at runtime */
            String repositoryClass = config.getInitParameter("repositoryClass");
            if(repositoryClass != null) {
            	this.repository = DAVServlet.newRepository(repositoryClass, root);
            } else {
            	// legacy configuration format. keep for now 
	            /* Make sure that we use the correct repository type */
	            if ("true".equalsIgnoreCase(config.getInitParameter("xmlOnly"))) {
	                this.repository = new XMLRepository(root);
	            } else {
	                this.repository = new DAVRepository(root);
	            }
            }

            /* Initialize the processor and register ourselves as listeners */
            this.processor = new DAVProcessor(this.repository);
            this.repository.addListener(this);
            this.logger.log("Initialized from " + root.getPath());

        } catch (MalformedURLException e) {
            throw new ServletException("Can't resolve \"" + rootPath + "\"", e);
        } catch (IOException e) {
            String msg = "Can't initialize repository at \"" + rootPath + "\"";
            throw new ServletException(msg, e);
        }
        
        /* Finally, register this repository in the servlet context */
        final String key = getRepositoryKey(config.getServletName());
        this.context.setAttribute(key, this.repository);
    }

    /**
     * <p>Retrieve a {@link DAVRepository} for a given {@link File}.</p>
     */
    public DAVRepository getRepository(File root)
    throws IOException {
        return new XMLRepository(root);
    }

    /**
     * <p>Detroy this {@link Servlet} instance.</p>
     */
    public void destroy() {
        this.repository.removeListener(this);
    }

    /**
     * <p>Return the {@link ServletConfig} associated with this instance.</p>
     */
    public ServletConfig getServletConfig() {
        return (this.config);
    }

    /**
     * <p>Return the {@link ServletContext} associated with this instance.</p>
     */
    public ServletContext getServletContext() {
        return (this.context);
    }

    /**
     * <p>Return a informative {@link String} about this servlet.</p>
     */
    public String getServletInfo() {
        return DAVUtilities.SERVLET_INFORMATION;
    }

    /**
     * <p>Execute the current request.</p>
     */
    public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        
        /* Mark our presence */
        res.setHeader("Server", this.context.getServerInfo() + ' ' + 
                                DAVUtilities.SERVLET_SIGNATURE);

        /* Normal methods are processed by their individual instances */
        DAVTransaction transaction = new DAVTransaction(req, res); 
        try {
            this.processor.process(transaction);
        } catch (RuntimeException exception) {
            final String header = req.getMethod() + ' ' + req.getRequestURI()
                                  + ' ' + req.getProtocol();
            this.context.log("Error processing: " + header);
            this.context.log("Exception processing DAV transaction", exception);
            throw exception;
        }
    }

    /* ====================================================================== */
    /* DAV LISTENER INTERFACE IMPLEMENTATION                                  */
    /* ====================================================================== */

    /**
     * <p>Receive notification of an event occurred in a specific
     * {@link DAVRepository}.</p>
     */
    public void notify(DAVResource resource, int event) {
        String message = "Unknown event";
        switch (event) {
            case DAVListener.COLLECTION_CREATED:
                message = "Collection created";
                break;
            case DAVListener.COLLECTION_REMOVED:
                message = "Collection removed";
                break;
            case DAVListener.RESOURCE_CREATED:
                message = "Resource created";
                break;
            case DAVListener.RESOURCE_REMOVED:
                message = "Resource removed";
                break;
            case DAVListener.RESOURCE_MODIFIED:
                message = "Resource modified";
                break;
        }
        this.logger.debug(message + ": \"" + resource.getRelativePath() + "\"");
    }

    /* ====================================================================== */
    /* CONTEXT METHODS                                                        */
    /* ====================================================================== */
    
    /**
     * <p>Retrieve the key in the {@link ServletContext} where the instance of
     * the {@link DAVRepository} associated with a named {@link DAVServlet}
     * can be found.</p>
     * 
     * @param servletName the name of the {@link DAVServlet} as specified in
     *                    the <code>web.xml</code> deployment descriptor.</p>
     */
    public static String getRepositoryKey(String servletName) {
        if (servletName == null) throw new NullPointerException();
        return DAVRepository.class.getName() + "." + servletName;
    }
    
    /** factory for subclasses configured in web.xml 
     * @param repositoryClass must extend DAVRepository and have a public constructor(File).
     *  */
    static DAVRepository newRepository(String repositoryClass, File root)
    throws ServletException
    {
    	try {
    		Class c = Class.forName(repositoryClass);
    		Constructor ctor = c.getConstructor(new Class[]{File.class});
    		DAVRepository repo = (DAVRepository)ctor.newInstance(new Object[]{root});
    		return repo;
    	} catch(ClassNotFoundException e) {
    		throw new ServletException(e);
    	} catch(LinkageError le) {
    		throw new ServletException(le);        	
    	} catch(NoSuchMethodException ns) {
    		throw new ServletException(ns);
    	} catch(InvocationTargetException it) {
    		throw new ServletException(it);
    	} catch(IllegalAccessException ia) {
    		throw new ServletException(ia);
    	} catch(InstantiationException ie) {
    		throw new ServletException(ie);
    	}
    }
    
}
