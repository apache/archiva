package org.apache.archiva.web.api;
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

import org.apache.archiva.web.model.JavascriptLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "javascriptLogger#default" )
public class DefaultJavascriptLogger
    implements JavascriptLogger
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    public Boolean trace( JavascriptLog javascriptLog )
    {
        Logger toUse =
            javascriptLog.getLoggerName() == null ? logger : LoggerFactory.getLogger( javascriptLog.getLoggerName() );

        toUse.trace( javascriptLog.getMessage() );
        return Boolean.TRUE;
    }

    public Boolean debug( JavascriptLog javascriptLog )
    {
        Logger toUse =
            javascriptLog.getLoggerName() == null ? logger : LoggerFactory.getLogger( javascriptLog.getLoggerName() );

        toUse.debug( javascriptLog.getMessage() );
        return Boolean.TRUE;
    }

    public Boolean info( JavascriptLog javascriptLog )
    {
        Logger toUse =
            javascriptLog.getLoggerName() == null ? logger : LoggerFactory.getLogger( javascriptLog.getLoggerName() );

        toUse.info( javascriptLog.getMessage() );
        return Boolean.TRUE;
    }

    public Boolean warn( JavascriptLog javascriptLog )
    {
        Logger toUse =
            javascriptLog.getLoggerName() == null ? logger : LoggerFactory.getLogger( javascriptLog.getLoggerName() );

        toUse.warn( javascriptLog.getMessage() );
        return Boolean.TRUE;
    }

    public Boolean error( JavascriptLog javascriptLog )
    {
        Logger toUse =
            javascriptLog.getLoggerName() == null ? logger : LoggerFactory.getLogger( javascriptLog.getLoggerName() );

        toUse.error( javascriptLog.getMessage() );
        return Boolean.TRUE;
    }
}
