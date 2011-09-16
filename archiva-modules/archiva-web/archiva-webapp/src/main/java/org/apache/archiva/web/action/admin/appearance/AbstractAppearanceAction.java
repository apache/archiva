package org.apache.archiva.web.action.admin.appearance;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.archiva.web.action.AbstractActionSupport;

import javax.inject.Inject;

/**
 * AbstractAppearanceAction
 *
 * @version $Id$
 */
public abstract class AbstractAppearanceAction
    extends AbstractActionSupport
    implements Preparable
{

    @Inject
    protected ArchivaAdministration archivaAdministration;

    private String organisationLogo;

    private String organisationUrl;

    private String organisationName;

    public String getOrganisationLogo()
    {
        return organisationLogo;
    }

    public String getOrganisationName()
    {
        return organisationName;
    }

    public String getOrganisationUrl()
    {
        return organisationUrl;
    }

    public void setOrganisationLogo( String organisationLogo )
    {
        this.organisationLogo = organisationLogo;
    }

    public void setOrganisationName( String organisationName )
    {
        this.organisationName = organisationName;
    }

    public void setOrganisationUrl( String organisationUrl )
    {
        this.organisationUrl = organisationUrl;
    }

    public void prepare()
        throws Exception
    {

        OrganisationInformation orgInfo = archivaAdministration.getOrganisationInformation();
        if ( orgInfo != null )
        {
            setOrganisationLogo( orgInfo.getLogoLocation() );
            setOrganisationName( orgInfo.getName() );
            setOrganisationUrl( orgInfo.getUrl() );
        }

    }

    public ArchivaAdministration getArchivaAdministration()
    {
        return archivaAdministration;
    }

    public void setArchivaAdministration( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }
}
