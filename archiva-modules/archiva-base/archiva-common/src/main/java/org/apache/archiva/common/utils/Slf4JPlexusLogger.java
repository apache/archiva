package org.apache.archiva.common.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Slf4JPlexusLogger - temporary logger to provide an Slf4j Logger to those components
 * outside of the archiva codebase that require a plexus logger.
 *
 * @version $Id$
 */
public class Slf4JPlexusLogger implements org.codehaus.plexus.logging.Logger {
    private Logger log;

    public Slf4JPlexusLogger(Class<?> clazz) {
        log = LoggerFactory.getLogger(clazz);
    }

    public Slf4JPlexusLogger(String name) {
        log = LoggerFactory.getLogger(name);
    }

    public void debug(String message) {
        log.debug(message);
    }

    public void debug(String message, Throwable throwable) {
        log.debug(message, throwable);
    }

    public void error(String message) {
        log.error(message);
    }

    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    public void fatalError(String message) {
        log.error(message);
    }

    public void fatalError(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    public org.codehaus.plexus.logging.Logger getChildLogger(String name) {
        return new Slf4JPlexusLogger(log.getName() + "." + name);
    }

    public String getName() {
        return log.getName();
    }

    public int getThreshold() {
        if (log.isTraceEnabled()) {
            return org.codehaus.plexus.logging.Logger.LEVEL_DEBUG;
        } else if (log.isDebugEnabled()) {
            return org.codehaus.plexus.logging.Logger.LEVEL_DEBUG;
        } else if (log.isInfoEnabled()) {
            return org.codehaus.plexus.logging.Logger.LEVEL_INFO;
        } else if (log.isWarnEnabled()) {
            return org.codehaus.plexus.logging.Logger.LEVEL_WARN;
        } else if (log.isErrorEnabled()) {
            return org.codehaus.plexus.logging.Logger.LEVEL_ERROR;
        }

        return org.codehaus.plexus.logging.Logger.LEVEL_DISABLED;
    }

    public void info(String message) {
        log.info(message);
    }

    public void info(String message, Throwable throwable) {
        log.info(message, throwable);
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    public boolean isFatalErrorEnabled() {
        return log.isErrorEnabled();
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    public void setThreshold(int threshold) {
        /* do nothing */
    }

    public void warn(String message) {
        log.warn(message);
    }

    public void warn(String message, Throwable throwable) {
        log.warn(message, throwable);
    }
}
