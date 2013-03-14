package org.apache.archiva.admin.model;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.beans.AbstractRepository;
import org.apache.archiva.admin.model.beans.ManagedRepository;

/**
 * apply basic repository validation : id and name.
 * Check if already exists.
 *
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public interface RepositoryCommonValidator
{

    String REPOSITORY_ID_VALID_EXPRESSION = "^[a-zA-Z0-9._-]+$";

    String REPOSITORY_NAME_VALID_EXPRESSION = "^([a-zA-Z0-9.)/_(-]|\\s)+$";


    void basicValidation( AbstractRepository abstractRepository, boolean update )
        throws RepositoryAdminException;

    /**
     * validate cronExpression and location format
     *
     * @param managedRepository
     * @since 1.4-M2
     */
    void validateManagedRepository( ManagedRepository managedRepository )
        throws RepositoryAdminException;

    /**
     * replace some interpolations ${appserver.base} with correct values
     *
     * @param directory
     * @return
     */
    String removeExpressions( String directory );

}
