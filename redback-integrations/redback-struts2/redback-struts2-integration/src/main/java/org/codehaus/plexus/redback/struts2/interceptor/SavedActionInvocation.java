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

import java.util.HashMap;
import java.util.Map;

public class SavedActionInvocation
{
    private String namespace;

    private String actionName;

    private Map<String, Object> parameterMap;

    private String methodName;

    @SuppressWarnings("unchecked")
    public SavedActionInvocation( ActionInvocation invocation )
    {
        namespace = invocation.getProxy().getNamespace();
        actionName = invocation.getProxy().getActionName();
        methodName = invocation.getProxy().getMethod();

        parameterMap = new HashMap<String, Object>();

        parameterMap.putAll( invocation.getInvocationContext().getParameters() );
    }

    public String getNamespace()
    {
        return namespace;
    }

    public String getActionName()
    {
        return actionName;
    }

    public Map<String,Object> getParametersMap()
    {
        return parameterMap;
    }

    public String getMethodName()
    {
        return methodName;
    }
}
