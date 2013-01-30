package org.apache.archiva.redback.rest.api.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * jaxrs fail to return List<String> so use this contains for rest services returning that
 *
 * @author Olivier Lamy
 * @since 2.1
 */
@XmlRootElement( name = "stringList" )
public class StringList
{
    private List<String> strings;

    public StringList()
    {
        // no op
    }

    public StringList( List<String> strings )
    {
        this.strings = strings;
    }

    public List<String> getStrings()
    {
        return strings == null ? new ArrayList<String>( 0 ) : strings;
    }

    public void setStrings( List<String> strings )
    {
        this.strings = strings;
    }
}
