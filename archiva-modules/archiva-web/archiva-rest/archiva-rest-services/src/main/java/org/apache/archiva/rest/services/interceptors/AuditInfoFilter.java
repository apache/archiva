package org.apache.archiva.rest.services.interceptors;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @since
 */
@Service("auditInfoFilter#rest")
@Provider
public class AuditInfoFilter implements ContainerRequestFilter
{

    private static final Logger log = LoggerFactory.getLogger( AuditInfoFilter.class );

    @Context
    private HttpServletRequest servletRequest;

    private static final AuditInfoThreadLocal auditInfoThreadLocal = new AuditInfoThreadLocal();

    public AuditInfoFilter() {

    }

    public static class AuditInfoThreadLocal extends ThreadLocal<AuditInfo> {

        public AuditInfoThreadLocal() {

        }

        @Override
        protected AuditInfo initialValue( )
        {
            return new AuditInfo();
        }
    }

    public static class AuditInfo {

        private String remoteAddress = "0.0.0.0";
        private String localAddress = "0.0.0.0";
        private String remoteHost = "0.0.0.0";
        private String protocol = "";
        private int remotePort = 0;
        private String method = "";

        public AuditInfo() {

        }

        public String getRemoteAddress( )
        {
            return remoteAddress;
        }

        public void setRemoteAddress( String remoteAddress )
        {
            this.remoteAddress = remoteAddress;
        }

        public String getLocalAddress( )
        {
            return localAddress;
        }

        public void setLocalAddress( String localAddress )
        {
            this.localAddress = localAddress;
        }

        public String getRemoteHost( )
        {
            return remoteHost;
        }

        public void setRemoteHost( String remoteHost )
        {
            this.remoteHost = remoteHost;
        }

        public int getRemotePort( )
        {
            return remotePort;
        }

        public void setRemotePort( int remotePort )
        {
            this.remotePort = remotePort;
        }

        public String getMethod( )
        {
            return method;
        }

        public void setMethod( String method )
        {
            this.method = method;
        }

        public String getProtocol( )
        {
            return protocol;
        }

        public void setProtocol( String protocol )
        {
            this.protocol = protocol;
        }
    }



    @Override
    public void filter( ContainerRequestContext containerRequestContext ) throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug( "Filter {}, {}", servletRequest.getRemoteAddr( ), servletRequest.getRemoteHost( ) );
        }
        AuditInfo auditInfo = auditInfoThreadLocal.get( );
        auditInfo.setRemoteAddress( servletRequest.getRemoteAddr( ) );
        auditInfo.setLocalAddress( servletRequest.getLocalAddr( ) );
        auditInfo.setProtocol( servletRequest.getProtocol( ) );
        auditInfo.setRemoteHost( servletRequest.getRemoteHost( ) );
        auditInfo.setRemotePort( servletRequest.getRemotePort( ) );
        auditInfo.setMethod( containerRequestContext.getMethod( ) );
    }

    public static AuditInfo getAuditInfo() {
        return auditInfoThreadLocal.get( );
    }
}
