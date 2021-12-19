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

import java.time.Period;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="ArtifactCleanupInfo",description = "Information for artifact cleanup feature of repository")
public class ArtifactCleanupInfo
{
    boolean deleteReleasedSnapshots;
    Period retentionPeriod;
    int retentionCount;

    @Schema(name="delete_released_snapshots",description = "True, if snapshots are deleted after a release was published")
    public boolean isDeleteReleasedSnapshots( )
    {
        return deleteReleasedSnapshots;
    }

    public void setDeleteReleasedSnapshots( boolean deleteReleasedSnapshots )
    {
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
    }

    @Schema(name="retention_period",description = "Time, after that snapshot artifacts are marked for deletion")
    public Period getRetentionPeriod( )
    {
        return retentionPeriod;
    }

    public void setRetentionPeriod( Period retentionPeriod )
    {
        this.retentionPeriod = retentionPeriod;
    }

    @Schema(name="retention_count",description = "Maximum number of snapshot artifacts to keep")
    public int getRetentionCount( )
    {
        return retentionCount;
    }

    public void setRetentionCount( int retentionCount )
    {
        this.retentionCount = retentionCount;
    }
}
