package org.apache.maven.archiva.xml;

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

import org.apache.commons.collections.Closure;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Gather the text from a collection of {@link Element}'s into a {@link List}
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ElementTextListClosure
    implements Closure
{
    private List list = new ArrayList();

    public void execute( Object input )
    {
        if ( input instanceof Element )
        {
            Element elem = (Element) input;
            list.add( elem.getTextTrim() );
        }
    }

    public List getList()
    {
        return list;
    }
}