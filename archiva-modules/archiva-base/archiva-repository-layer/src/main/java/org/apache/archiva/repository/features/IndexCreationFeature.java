package org.apache.archiva.repository.features;

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
 *
 * This feature provides some information about index creation.
 *
 */
public class IndexCreationFeature implements RepositoryFeature<IndexCreationFeature> {

    private boolean skipPackedIndexCreation = false;

    public IndexCreationFeature() {

    }

    public IndexCreationFeature(boolean skipPackedIndexCreation) {
        this.skipPackedIndexCreation = skipPackedIndexCreation;
    }

    @Override
    public IndexCreationFeature get() {
        return this;
    }

    /**
     * Returns true, if no packed index files should be created.
     * @return True, if no packed index files are created, otherwise false.
     */
    public boolean isSkipPackedIndexCreation() {
        return skipPackedIndexCreation;
    }

    /**
     * Sets the flag for packed index creation.
     *
     * @param skipPackedIndexCreation
     */
    public void setSkipPackedIndexCreation(boolean skipPackedIndexCreation) {
        this.skipPackedIndexCreation = skipPackedIndexCreation;
    }
}
