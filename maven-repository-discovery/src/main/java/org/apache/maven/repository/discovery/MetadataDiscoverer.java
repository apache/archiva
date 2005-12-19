package org.apache.maven.repository.discovery;

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

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Interface for discovering metadata files.
 * 
 * @author Maria Odea Ching
 */
public interface MetadataDiscoverer {
	String ROLE = MetadataDiscoverer.class.getName();

	/**
	 * Search for metadata files in the repository.
	 * 
	 * @param repositoryBase
	 *            The repository directory.
	 * @param blacklistedPatterns
	 *            Patterns that are to be excluded from the discovery process.
	 * @return
	 */
	List discoverMetadata(File repositoryBase, String blacklistedPatterns);

	/**
	 * Get the list of paths kicked out during the discovery process.
	 * 
	 * @return the paths as Strings.
	 */
	Iterator getKickedOutPathsIterator();

	/**
	 * Get the list of paths excluded during the discovery process.
	 * 
	 * @return the paths as Strings.
	 */
	Iterator getExcludedPathsIterator();
}
