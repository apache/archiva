package org.apache.maven.repository.indexing.query;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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
 * Base of all query terms.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractCompoundQueryTerm
    implements CompoundQueryTerm
{
    /**
     * The query being added.
     */
    private Query query;

    /**
     * Class constructor
     *
     * @param query the query represented by this object
     */
    protected AbstractCompoundQueryTerm( Query query )
    {
        this.query = query;
    }

    /**
     * @see CompoundQueryTerm#isRequired()
     */
    public boolean isRequired()
    {
        return false;
    }

    /**
     * @see CompoundQueryTerm#isProhibited()
     */
    public boolean isProhibited()
    {
        return false;
    }

    /**
     * @see CompoundQueryTerm#getQuery()
     */
    public Query getQuery()
    {
        return query;
    }
}
