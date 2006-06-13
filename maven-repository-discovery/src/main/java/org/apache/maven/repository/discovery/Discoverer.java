package org.apache.maven.repository.discovery;

import java.util.Iterator;

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
 * @author Edwin Punzalan
 */
public interface Discoverer
{
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
