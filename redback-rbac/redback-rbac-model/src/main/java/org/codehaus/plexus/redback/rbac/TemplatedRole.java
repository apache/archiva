package org.codehaus.plexus.redback.rbac;

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


/**
 * TemplatedRole
 *
 * @author <a href="hisidro@exist.com">Henry Isidro</a>
 */
public class TemplatedRole
{
    private Role role;
    
    private String templateNamePrefix;
    
    private String delimiter;
    
    public TemplatedRole(Role role, String templateNamePrefix, String delimeter)
    {
        this.role = role;
        this.templateNamePrefix = templateNamePrefix;
        this.delimiter = delimeter;
    }
    
    public String getResource()
    {
        int index = role.getName().indexOf( getDelimiter() );
        
        return role.getName().substring( index + 3);
    }

    public Role getRole()
    {
        return role;
    }

    public void setRole( Role role )
    {
        this.role = role;
    }

    public String getTemplateNamePrefix()
    {
        return templateNamePrefix;
    }

    public void setTemplateNamePrefix( String templateNamePrefix )
    {
        this.templateNamePrefix = templateNamePrefix;
    }

    public String getDelimiter()
    {
        return delimiter;
    }

    public void setDelimiter( String delimiter )
    {
        this.delimiter = delimiter;
    }

    public String getName()
    {
        return this.role.getName();
    }

    public void setName( String name )
    {
        this.role.setName( name );
    }
}
