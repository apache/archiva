package org.codehaus.plexus.redback.struts2.interceptor;

/*
 * Copyright 2006-2007 The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork2.ActionInvocation;

public interface ActionInvocationTracker
{

    static final String SESSION_KEY = ActionInvocationTracker.class.getName();

    void setHistorySize( int size );

    int getHistorySize();

    int getHistoryCount();

    SavedActionInvocation getPrevious();

    SavedActionInvocation getCurrent();

    SavedActionInvocation getActionInvocationAt( int index );

    void addActionInvocation( ActionInvocation invocation );

    void setBackTrack();

    void unsetBackTrack();

    boolean isBackTracked();
}
