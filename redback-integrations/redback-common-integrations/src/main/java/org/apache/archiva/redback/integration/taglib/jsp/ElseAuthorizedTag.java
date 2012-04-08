package org.apache.archiva.redback.integration.taglib.jsp;

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

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

/**
 * IfAuthorizedTag:
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 * @version $Id$
 */
public class ElseAuthorizedTag
    extends ConditionalTagSupport
{
    protected boolean condition()
        throws JspTagException
    {
        Boolean authzStatus = (Boolean) pageContext.getAttribute( "ifAuthorizedTag" );

        if ( authzStatus != null )
        {
            pageContext.removeAttribute( "ifAuthorizedTag" );

            return !authzStatus.booleanValue();
        }

        authzStatus = (Boolean) pageContext.getAttribute( "ifAnyAuthorizedTag" );

        if ( authzStatus != null )
        {
            pageContext.removeAttribute( "ifAnyAuthorizedTag" );

            return !authzStatus.booleanValue();
        }

        throw new JspTagException( "ElseAuthorizedTag should follow either IfAuthorized or IfAnyAuthorized" );
    }
}
