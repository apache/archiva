package org.codehaus.plexus.redback.example.web.action;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.redback.struts2.action.RedbackActionSupport;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * PlexusSecuritySystemAction:
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 * @version $Id$
 */
@Controller("status")
@Scope("prototype")
public class StatusAction
    extends RedbackActionSupport
{

    public String status()
    {
        return SUCCESS;
    }
}
