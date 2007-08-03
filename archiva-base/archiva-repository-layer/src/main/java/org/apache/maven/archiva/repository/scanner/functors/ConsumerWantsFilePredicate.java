package org.apache.maven.archiva.repository.scanner.functors;

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

import org.apache.commons.collections.Predicate;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;

/**
 * ConsumerWantsFilePredicate 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ConsumerWantsFilePredicate
    implements Predicate
{
    private BaseFile basefile;

    private boolean isCaseSensitive = true;

    private int wantedFileCount = 0;

    public boolean evaluate( Object object )
    {
        boolean satisfies = false;

        if ( object instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) object;
            if ( wantsFile( consumer, StringUtils.replace( basefile.getRelativePath(), "\\", "/" ) ) )
            {
                satisfies = true;
                wantedFileCount++;
            }
        }

        return satisfies;
    }

    public BaseFile getBasefile()
    {
        return basefile;
    }

    public int getWantedFileCount()
    {
        return wantedFileCount;
    }

    public boolean isCaseSensitive()
    {
        return isCaseSensitive;
    }

    public void setBasefile( BaseFile basefile )
    {
        this.basefile = basefile;
        this.wantedFileCount = 0;
    }

    public void setCaseSensitive( boolean isCaseSensitive )
    {
        this.isCaseSensitive = isCaseSensitive;
    }

    private boolean wantsFile( RepositoryContentConsumer consumer, String relativePath )
    {
        Iterator it;

        // Test excludes first.
        if ( consumer.getExcludes() != null )
        {
            it = consumer.getExcludes().iterator();
            while ( it.hasNext() )
            {
                String pattern = (String) it.next();
                if ( SelectorUtils.matchPath( pattern, relativePath, isCaseSensitive ) )
                {
                    // Definately does NOT WANT FILE.
                    return false;
                }
            }
        }

        // Now test includes.
        it = consumer.getIncludes().iterator();
        while ( it.hasNext() )
        {
            String pattern = (String) it.next();
            if ( SelectorUtils.matchPath( pattern, relativePath, isCaseSensitive ) )
            {
                // Specifically WANTS FILE.
                return true;
            }
        }

        // Not included, and Not excluded?  Default to EXCLUDE.
        return false;
    }
}
