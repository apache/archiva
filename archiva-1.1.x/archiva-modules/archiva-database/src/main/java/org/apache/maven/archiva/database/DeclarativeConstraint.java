package org.apache.maven.archiva.database;

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

/**
 * DeclarativeConstraint
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface DeclarativeConstraint
    extends Constraint
{
    /**
     * Get the declared imports used for this query. (optional)
     * <p/>
     * NOTE: This is DAO implementation specific.
     *
     * @return the imports. (can be null)
     */
    public abstract String[] getDeclaredImports();

    /**
     * Get the declared parameters used for this query. (optional)
     * <p/>
     * NOTE: This is DAO implementation specific.
     *
     * @return the parameters. (can be null)
     */
    public abstract String[] getDeclaredParameters();

    /**
     * The JDOQL filter to apply to the query. (optional)
     * <p/>
     * NOTE: This is DAO implementation specific.
     *
     * @return the filter to apply. (can be null)
     */
    public abstract String getFilter();

    /**
     * Get the parameters used for this query. (required if using {@link #getDeclaredParameters()} )
     * <p/>
     * NOTE: This is DAO implementation specific.
     *
     * @return the parameters. (can be null)
     */
    public abstract Object[] getParameters();

    /**
     * Get the sort direction name.
     *
     * @return the sort direction name. ("ASC" or "DESC") (only valid if {@link #getSortColumn()} is specified.)
     */
    public abstract String getSortDirection();

    /**
     * Get the sort column name.
     *
     * @return the sort column name. (can be null)
     */
    public abstract String getSortColumn();

    /**
     * Get the variables used within the query.
     * <p/>
     * NOTE: This is DAO implementation specific.
     *
     * @return the variables used within the query.
     */
    public abstract String[] getVariables();

    /**
     * Get the SELECT WHERE (condition) value for the constraint.
     *
     * @return the equivalent of the SELECT WHERE (condition) value for this constraint. (can be null)
     */
    public abstract String getWhereCondition();

    /**
     * Get the declared range used for this query. (optional)
     * <p/>
     * NOTE: This is DAO implementation specific.
     *
     * @return the range. (can be null)
     */
    public abstract int[] getRange();
}
