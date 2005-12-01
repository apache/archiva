package org.apache.maven.repository;

/* 
 * Copyright 2001-2005 The Apache Software Foundation. 
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
 * This class is used to ignore several files that may be present inside the repository so that the other classes
 * may not worry about them and then can concentrate on doing their tasks.
 *
 */
public class RepositoryFileFilter implements java.io.FileFilter
{
    public boolean accept(java.io.File pathname)
    {
        if ( pathname.isDirectory() )
        {
            if ( ".svn".equals( pathname.getName() ) ) return false;
            if ( "CVS".equals( pathname.getName() ) ) return false;
        }
        else
        {
            String name = pathname.getName();
            if ( name.endsWith( ".md5" ) ) return false;
            if ( name.endsWith( ".sha1" ) ) return false;
        }

        return true;
    }
}
