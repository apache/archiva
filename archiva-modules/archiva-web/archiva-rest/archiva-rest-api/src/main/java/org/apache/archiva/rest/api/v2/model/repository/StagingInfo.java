package org.apache.archiva.rest.api.v2.model.repository;/*
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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="StagingInfo",description = "Information about staging feature of a repository")
public class StagingInfo
{
    private String stagingRepository;
    private boolean stageRepoNeeded;

    @Schema(name="staging_repo",description = "The repository id of the staging repository")
    public String getStagingRepository( )
    {
        return stagingRepository;
    }

    public void setStagingRepository( String stagingRepository )
    {
        this.stagingRepository = stagingRepository;
    }

    @Schema(name="stage_repo_needed",description = "True, if this repository needs a staging repository")
    public boolean isStageRepoNeeded( )
    {
        return stageRepoNeeded;
    }

    public void setStageRepoNeeded( boolean stageRepoNeeded )
    {
        this.stageRepoNeeded = stageRepoNeeded;
    }
}
