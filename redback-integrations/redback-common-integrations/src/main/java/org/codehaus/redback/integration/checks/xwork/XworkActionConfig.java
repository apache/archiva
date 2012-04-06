package org.codehaus.redback.integration.checks.xwork;

/*
 * Copyright 2005-2006 The Codehaus.
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

import java.util.ArrayList;
import java.util.List;

/**
 * XworkActionConfig
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class XworkActionConfig
{
    public String name;

    public String clazz;

    public String method;

    public List<String> results = new ArrayList<String>();

    public XworkActionConfig( String name, String className, String method )
    {
        this.name = name;
        this.clazz = className;
        this.method = method;
    }

    public XworkActionConfig addResult( String name )
    {
        results.add( name );
        return this;
    }
}
