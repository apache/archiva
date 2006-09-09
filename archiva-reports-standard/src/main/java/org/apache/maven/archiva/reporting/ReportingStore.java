package org.apache.maven.archiva.reporting;

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

import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * A component for loading the reporting database into the model.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo this is something that could possibly be generalised into Modello.
 */
public interface ReportingStore
{
    /**
     * The Plexus role for the component.
     */
    String ROLE = ReportingStore.class.getName();

    /**
     * Get the reports from the store. A cached version may be used.
     *
     * @param repository the repository to load the reports for
     * @return the reporting database
     * @throws ReportingStoreException if there was a problem reading the store
     */
    ReportingDatabase getReportsFromStore( ArtifactRepository repository )
        throws ReportingStoreException;

    /**
     * Save the reporting to the store.
     *
     * @param database   the reports to store
     * @param repository the repositorry to store the reports in
     * @throws ReportingStoreException if there was a problem writing the store
     */
    void storeReports( ReportingDatabase database, ArtifactRepository repository )
        throws ReportingStoreException;

}
