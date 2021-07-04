package org.apache.archiva.repository.validation;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A repository validator validates given repository data against certain rules.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface RepositoryValidator<R extends Repository> extends RepositoryChecker<R, Map<String, List<ValidationError>>>, Comparable<RepositoryValidator<R>>
{

    String REPOSITORY_ID_VALID_EXPRESSION = "^[a-zA-Z0-9._-]+$";
    String[] REPOSITORY_ID_ALLOWED = new String[]{"alphanumeric, '.', '-','_'"};
    String REPOSITORY_NAME_VALID_EXPRESSION = "^([a-zA-Z0-9.)/_(-]|\\s)+$";
    String[] REPOSITORY_NAME_ALLOWED = new String[]{"alphanumeric", "whitespace", "/", "(", ")", "_", ".", "-"};
    String REPOSITORY_LOCATION_VALID_EXPRESSION = "^[-a-zA-Z0-9._/~:?!&amp;=\\\\]+$";


    int DEFAULT_PRIORITY=1000;

    /**
     * Returns the repository type for which this validator can be used. If the validator is applicable
     * to all types, it should return {@link RepositoryType#ALL}
     *
     * @return the repository type for which this validator is applicable
     */
    default RepositoryType getType() {
        return RepositoryType.ALL;
    }

    /**
     * Returns the priority of this validator. Smaller values mean higher priority.
     * All common validators have priority {@link #DEFAULT_PRIORITY}
     *
     * Validators are called in numerical order of their priority.
     *
     * @return
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }


    /**
     * Orders by priority
     *
     * @see Comparable#compareTo(Object)
     */
    @Override
    default int compareTo( RepositoryValidator o ) {
        if (o==null) {
            return 1;
        } else
        {
            return this.getPriority( ) - o.getPriority( );
        }
    }

    /**
     * Sets the repository registry to the given instance.
     * @param repositoryRegistry the repository registry
     */
    void setRepositoryRegistry( RepositoryRegistry repositoryRegistry );

    Class<R> getFlavour();

    default boolean isFlavour(Class<?> clazz) {
        return getFlavour( ).isAssignableFrom( clazz );
    }

    @SuppressWarnings( "unchecked" )
    default <RR extends Repository> RepositoryValidator<RR> narrowTo( Class<RR> clazz ) {
        if (isFlavour( clazz )) {
            return (RepositoryValidator<RR>) this;
        } else {
            throw new IllegalArgumentException( "Could not narrow to " + clazz );
        }
    }
}
