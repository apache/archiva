package org.apache.maven.repository.indexing.record;

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
 * The fields in a minimal artifact index record.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo should be an enum
 */
public class StandardIndexRecordFields
{
    public static final String FILENAME = "filename";

    public static final String GROUPID = "groupId";

    public static final String GROUPID_EXACT = GROUPID + "_u";

    public static final String ARTIFACTID = "artifactId";

    public static final String ARTIFACTID_EXACT = ARTIFACTID + "_u";

    public static final String VERSION = "version";

    public static final String VERSION_EXACT = VERSION + "_u";

    public static final String BASE_VERSION = "baseVersion";

    public static final String BASE_VERSION_EXACT = BASE_VERSION + "_u";

    public static final String TYPE = "type";

    public static final String CLASSIFIER = "classifier";

    public static final String PACKAGING = "packaging";

    public static final String REPOSITORY = "repo";

    public static final String LAST_MODIFIED = "lastModified";

    public static final String FILE_SIZE = "fileSize";

    public static final String MD5 = "md5";

    public static final String SHA1 = "sha1";

    public static final String CLASSES = "classes";

    public static final String PLUGIN_PREFIX = "pluginPrefix";

    public static final String FILES = "files";

    public static final String INCEPTION_YEAR = "inceptionYear";

    public static final String PROJECT_NAME = "projectName";

    public static final String PROJECT_DESCRIPTION = "projectDesc";

    private StandardIndexRecordFields()
    {
        // No touchy!
    }
}
