package org.apache.archiva.metadata.repository.jcr;

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

import java.time.ZoneId;

/**
 * Node types and properties defined in the schema.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface JcrConstants
{



    String BASE_NODE_TYPE = "archiva:base";
    String NAMESPACE_NODE_TYPE = "archiva:namespace";
    String PROJECT_NODE_TYPE = "archiva:project";
    String PROJECT_VERSION_NODE_TYPE = "archiva:projectVersion";
    String ARTIFACT_NODE_TYPE = "archiva:artifact";
    String REPOSITORY_NODE_TYPE = "archiva:repository";
    String FACET_NODE_TYPE = "archiva:facet";
    String MIXIN_META_SCM = "archiva:meta_scm";
    String MIXIN_META_CI = "archiva:meta_ci";
    String MIXIN_META_ISSUE = "archiva:meta_issue";
    String MIXIN_META_ORGANIZATION = "archiva:meta_organization";
    String MIXIN_META_LICENSE = "archiva:meta_license";
    String MIXIN_META_MAILINGLIST = "archiva:meta_mailinglist";
    String DEPENDENCY_NODE_TYPE = "archiva:dependency";

    // Must be alphabetically ordered!
    String[] PROJECT_VERSION_VERSION_PROPERTIES = {"ci.system","ci.url", "description", "incomplete", "issue.system","issue.url", "name", "org.name", "org.url", "url", "scm.connection", "scm.developerConnection", "scm.url"};
}
