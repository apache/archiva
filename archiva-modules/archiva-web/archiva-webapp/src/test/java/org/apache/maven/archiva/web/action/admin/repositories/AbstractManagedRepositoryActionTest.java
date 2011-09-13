package org.apache.maven.archiva.web.action.admin.repositories;

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

import com.opensymphony.xwork2.validator.ActionValidatorManager;
import org.apache.archiva.admin.model.managed.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.struts2.StrutsSpringTestCase;

import java.io.File;

public abstract class AbstractManagedRepositoryActionTest
    extends StrutsSpringTestCase
{
    protected static final String EMPTY_STRING = "";

    // valid inputs; validation testing
    protected static final String REPOSITORY_ID_VALID_INPUT = "abcXYZ0129._-";

    protected static final String REPOSITORY_LOCATION_VALID_INPUT = "abcXYZ0129._/\\~:?!&=-";

    protected static final String REPOSITORY_INDEX_DIR_VALID_INPUT = "abcXYZ0129._/\\~:?!&=-";

    protected static final String REPOSITORY_NAME_VALID_INPUT = "abcXYZ   0129.)/   _(-";

    protected static final int REPOSITORY_RETENTION_COUNT_VALID_INPUT = 1;

    protected static final int REPOSITORY_DAYS_OLDER_VALID_INPUT = 1;

    // invalid inputs; validation testing
    protected static final String REPOSITORY_ID_INVALID_INPUT = "<> \\/~+[ ]'\"";

    protected static final String REPOSITORY_LOCATION_INVALID_INPUT = "<> ~+[ ]'\"";

    protected static final String REPOSITORY_INDEX_DIR_INVALID_INPUT = "<> ~+[ ]'\"";

    protected static final String REPOSITORY_NAME_INVALID_INPUT = "<>\\~+[]'\"";

    protected static final int REPOSITORY_RETENTION_COUNT_INVALID_INPUT = 101;

    protected static final int REPOSITORY_DAYS_OLDER_INVALID_INPUT = -1;

    // testing requisite; validation testing
    protected ActionValidatorManager actionValidatorManager;

    protected static final String REPO_ID = "repo-ident";

    protected File location;

    @Override
    protected String[] getContextLocations()
    {
        return new String[]{ "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" };
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        DefaultActionValidatorManagerFactory defaultActionValidatorManagerFactory =
            new DefaultActionValidatorManagerFactory();

        actionValidatorManager = defaultActionValidatorManagerFactory.createDefaultActionValidatorManager();
    }

    protected void populateRepository( ManagedRepository repository )
    {
        repository.setId( REPO_ID );
        repository.setName( "repo name" );
        repository.setLocation( location.getAbsolutePath() );
        repository.setLayout( "default" );
        repository.setCronExpression( "* 0/5 * * * ?" );
        repository.setDaysOlder( 31 );
        repository.setRetentionCount( 20 );
        repository.setReleases( true );
        repository.setSnapshots( false );
        repository.setScanned( false );
        repository.setDeleteReleasedSnapshots( true );
    }

    protected ManagedRepository createManagedRepository( String id, String name, String location )
    {
        ManagedRepository managedRepositoryConfiguration = new ManagedRepository();

        managedRepositoryConfiguration.setId( id );
        managedRepositoryConfiguration.setName( name );
        managedRepositoryConfiguration.setLocation( location );

        return managedRepositoryConfiguration;
    }

    // over-loaded
    // for simulating empty/null form purposes; excluding primitive data-typed values
    protected ManagedRepository createManagedRepository( String id, String name, String location,
                                                                      String indexDir )
    {
        ManagedRepository managedRepositoryConfiguration = new ManagedRepository();

        managedRepositoryConfiguration.setId( id );
        managedRepositoryConfiguration.setName( name );
        managedRepositoryConfiguration.setLocation( location );
        managedRepositoryConfiguration.setIndexDirectory( indexDir );

        return managedRepositoryConfiguration;
    }

    protected ManagedRepository createManagedRepository( String id, String name, String location, String indexDir,
                                                         int daysOlder, int retentionCount )
    {
        ManagedRepository managedRepositoryConfiguration = new ManagedRepository();

        managedRepositoryConfiguration.setId( id );
        managedRepositoryConfiguration.setName( name );
        managedRepositoryConfiguration.setLocation( location );
        managedRepositoryConfiguration.setIndexDirectory( indexDir );
        managedRepositoryConfiguration.setDaysOlder( daysOlder );
        managedRepositoryConfiguration.setRetentionCount( retentionCount );

        return managedRepositoryConfiguration;
    }


    protected ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return applicationContext.getBean( ManagedRepositoryAdmin.class );
    }
}
