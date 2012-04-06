package org.codehaus.plexus.redback.struts2;

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
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.config.entities.ActionConfig;

public class ActionProxyStub
    implements ActionProxy
{
    public static final String ACTION_NAME = "stub_action";

    public static final String NAMESPACE = "namespace";

    public static final String METHOD = "method";

    private String methodName;

    private String actionName;

    public Object getAction()
    {
        return null;
    }

    public void setActionName( String name )
    {
        actionName = name;
    }

    public void prepare() throws Exception
    {
        //Do nothing
    }

    public String getActionName()
    {
        if ( actionName != null )
        {
            return actionName;
        }
        else
        {
            return ACTION_NAME;
        }
    }

    public ActionConfig getConfig()
    {
        return null;
    }

    public void setExecuteResult( boolean result )
    {

    }

    public boolean getExecuteResult()
    {
        return false;
    }

    public ActionInvocation getInvocation()
    {
        return null;
    }

    public String getNamespace()
    {
        return NAMESPACE;
    }

    public String execute()
    {
        return null;
    }

    public void setMethod( String name )
    {
        methodName = name;
    }

    public String getMethod()
    {
        if ( methodName != null )
        {
            return methodName;
        }
        else
        {
            return METHOD;
        }
    }
}
