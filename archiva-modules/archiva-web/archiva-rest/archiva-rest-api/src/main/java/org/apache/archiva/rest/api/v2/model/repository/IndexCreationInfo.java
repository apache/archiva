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
@Schema(name="IndexCreationInfo",description = "Information about index creation feature of repositories")
public class IndexCreationInfo
{
    private boolean skipPackedIndexCreation = false;
    private String indexPath;
    private String packedIndexPath;

    @Schema(name="skip_packed_index_creation",description = "True, if the packed index will not be created")
    public boolean isSkipPackedIndexCreation( )
    {
        return skipPackedIndexCreation;
    }

    public void setSkipPackedIndexCreation( boolean skipPackedIndexCreation )
    {
        this.skipPackedIndexCreation = skipPackedIndexCreation;
    }

    @Schema(name="index_path",description = "Path to the index directory relative to the repository base directory")
    public String getIndexPath( )
    {
        return indexPath;
    }

    public void setIndexPath( String indexPath )
    {
        this.indexPath = indexPath;
    }

    @Schema(name="packed_index_path",description = "Path to the packed index directory relative to the repository base directory")
    public String getPackedIndexPath( )
    {
        return packedIndexPath;
    }

    public void setPackedIndexPath( String packedIndexPath )
    {
        this.packedIndexPath = packedIndexPath;
    }
}
