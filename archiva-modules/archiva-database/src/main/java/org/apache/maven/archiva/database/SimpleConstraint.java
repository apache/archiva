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
 * SimpleConstraint 
 *
 * @version $Id$
 */
public interface SimpleConstraint extends Constraint
{
    /**
     * Get the parameters used for this query. (required if using parameterized SQL)
     * 
     * NOTE: This is DAO implementation specific.
     * 
     * @return the parameters. (can be null)
     */
    public Object[] getParameters();

    /**
     * Get the SELECT query value for the constraint.
     * 
     * @return the SELECT value for this constraint. (can be null)
     */
    public abstract String getSelectSql();

    /**
     * For simple Constraints the results class must be specified.
     * 
     * @return the result class.
     */
    public Class getResultClass();
    
    /**
     * When working with result classes that are not persistable,
     * it is advisable to tell the underlying DAO to not do the persistable related efforts.
     * 
     * @return true if result classes are persistable.
     */
    public boolean isResultsPersistable();
}
