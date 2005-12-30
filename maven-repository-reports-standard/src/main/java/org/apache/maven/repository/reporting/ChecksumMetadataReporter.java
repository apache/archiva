package org.apache.maven.repository.reporting;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class reports invalid and mismatched checksums of artifacts and metadata files.
 * It validates MD5 and SHA-1 checksums.
 *
 * @todo remove this duplicate class for the parent using one role
 * @plexus.component role="org.apache.maven.repository.reporting.MetadataReportProcessor" role-hint="checksum-metadata" instantiation-strategy="per-lookup"
 */
public class ChecksumMetadataReporter
    extends ChecksumArtifactReporter
    implements MetadataReportProcessor
{
}
