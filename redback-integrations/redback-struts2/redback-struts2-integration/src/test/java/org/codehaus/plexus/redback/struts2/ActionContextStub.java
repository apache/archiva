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

import com.opensymphony.xwork2.ActionContext;

import java.util.HashMap;

public class ActionContextStub
    extends ActionContext
{
    public static final String CONTEXT_NAME = "context_name";

    public static final String PARAMETER_1 = "parameter_1";

    public static final String PARAMETER_2 = "parameter_2";

    public static final String PARAMETER_3 = "parameter_3";

    public static final String VALUE_1 = "value_1";

    public static final String VALUE_2 = "value_2";

    public static final String VALUE_3 = "value_3";

    @SuppressWarnings("unchecked")
    public ActionContextStub()
    {
        super( new HashMap() );
        this.setName( CONTEXT_NAME );
        this.setSession( new HashMap() );

        this.setParameters( new HashMap<String,Object>() );
        this.getParameters().put( PARAMETER_1, VALUE_1 );
        this.getParameters().put( PARAMETER_2, VALUE_2 );
        this.getParameters().put( PARAMETER_3, VALUE_3 );
    }
}
