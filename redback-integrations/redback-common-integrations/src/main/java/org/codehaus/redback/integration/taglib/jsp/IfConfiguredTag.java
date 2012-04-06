package org.codehaus.redback.integration.taglib.jsp;

/*
 * Copyright 2006 The Codehaus.
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

import org.codehaus.plexus.redback.configuration.UserConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

/**
 * IfConfiguredTag:
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 * @version $Id$
 */
public class IfConfiguredTag
    extends ConditionalTagSupport
{
    private String option;

    private String value;

    public void setOption( String option )
    {
        this.option = option;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    protected boolean condition()
        throws JspTagException
    {
        ApplicationContext applicationContext =
            WebApplicationContextUtils.getRequiredWebApplicationContext( pageContext.getServletContext() );

        UserConfiguration config = applicationContext.getBean( "userConfiguration", UserConfiguration.class );

        if ( value != null )
        {
            String configValue = config.getString( option );

            if ( value.equals( configValue ) )
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return config.getBoolean( option );
        }
    }
}
