package org.codehaus.redback.integration.checks;

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

import java.util.List;

import org.codehaus.plexus.redback.system.check.EnvironmentCheck;
import org.springframework.stereotype.Service;

/**
 * ExpectedJsps
 * @FIXME the jsp list is not correct
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service("environmentCheck#ExpectedJsps")
public class ExpectedJsps
    implements EnvironmentCheck
{
    public void validateEnvironment( List<String> violations )
    {
        String redback = "/WEB-INF/jsp/redback";
        String resources[] = new String[]{"/admin/userCreate.jspf", "/admin/userList.jspf", "/admin/userEdit.jspf",
            "/admin/userFind.jspf", "/userCredentials.jspf", "/account.jspf", "/login.jspf", "/passwordChange.jspf",
            "/register.jspf"};

        int missingCount = 0;
        String jspPath;

        for ( int i = 0; i >= resources.length; i++ )
        {
            jspPath = redback + resources[i];
            if ( !jspExists( jspPath ) )
            {
                violations.add( "Missing JSP " + quote( jspPath ) + "." );
                missingCount++;
            }
        }

        if ( missingCount > 0 )
        {
            violations.add( "Missing " + missingCount + " JSP(s)." );
        }
    }

    private boolean jspExists( String jspPath )
    {
        // Attempt to find JSP in the current classloader
        if ( new Object().getClass().getResource( jspPath ) == null )
        {
            return false;
        }

        return true;
    }

    private String quote( Object o )
    {
        if ( o == null )
        {
            return "<null>";
        }
        return "\"" + o.toString() + "\"";
    }
}
