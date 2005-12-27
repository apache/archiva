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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This class gets all the paths that contain the metadata files.
 * 
 */
public class DefaultMetadataDiscoverer implements MetadataDiscoverer {

	/**
	 * Standard patterns to exclude from discovery as they are not artifacts.
	 */
	private static final String[] STANDARD_DISCOVERY_EXCLUDES = { "bin/**",
			"reports/**", ".maven/**", "**/*.md5", "**/*.MD5", "**/*.sha1",
			"**/*.SHA1", "**/*snapshot-version", "*/website/**",
			"*/licenses/**", "*/licences/**", "**/.htaccess", "**/*.html",
			"**/*.asc", "**/*.txt", "**/README*", "**/CHANGELOG*", "**/KEYS*" };

	/**
	 * Standard patterns to include in discovery of metadata files.
	 */
	private static final String[] STANDARD_DISCOVERY_INCLUDES = {
			"**/*-metadata.xml", "**/*/*-metadata.xml",
			"**/*/*/*-metadata.xml", "**/*-metadata-*.xml",
			"**/*/*-metadata-*.xml", "**/*/*/*-metadata-*.xml" };

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private List excludedPaths = new ArrayList();

	private List kickedOutPaths = new ArrayList();

	/**
	 * Search the repository for metadata files.
	 * 
	 * @param repositoryBase
	 * @param blacklistedPatterns
	 */
	public List discoverMetadata(File repositoryBase, String blacklistedPatterns) {
		List metadataFiles = new ArrayList();
		String[] metadataPaths = scanForMetadataPaths(repositoryBase,
				blacklistedPatterns);

		for (int i = 0; i < metadataPaths.length; i++) {
			RepositoryMetadata metadata = buildMetadata(repositoryBase
					.getPath(), metadataPaths[i]);

			if (metadata != null)
				metadataFiles.add(metadata);
			else
				kickedOutPaths.add(metadataPaths[i]);
		}

		return metadataFiles;
	}

	/**
	 * Create RepositoryMetadata object.
	 * 
	 * @param repo
	 *            The path to the repository.
	 * @param metadataPath
	 *            The path to the metadata file.
	 * @return
	 */
	private RepositoryMetadata buildMetadata(String repo, String metadataPath) {

		RepositoryMetadata metadata = null;

		try {
			URL url = new File(repo + "/" + metadataPath).toURL();
			InputStream is = url.openStream();
			Reader reader = new InputStreamReader(is);
			MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

			Metadata m = metadataReader.read(reader);
			String metaGroupId = m.getGroupId();
			String metaArtifactId = m.getArtifactId();
			String metaVersion = m.getVersion();

			// check if the groupId, artifactId and version is in the
			// metadataPath
			// parse the path, in reverse order
			List pathParts = new ArrayList();
			StringTokenizer st = new StringTokenizer(metadataPath, "/\\");
			while (st.hasMoreTokens()) {
				pathParts.add(st.nextToken());
			}

			Collections.reverse(pathParts);
			// remove the metadata file
			pathParts.remove(0);
			Iterator it = pathParts.iterator();
			String tmpDir = (String) it.next();

			ArtifactHandler handler = new DefaultArtifactHandler("jar");
			VersionRange version = VersionRange.createFromVersion(metaVersion);
			Artifact artifact = new DefaultArtifact(metaGroupId,
					metaArtifactId, version, "compile", "jar", "", handler);

			// snapshotMetadata
			if (tmpDir.equals(metaVersion)) {
				metadata = new SnapshotArtifactRepositoryMetadata(artifact);
			} else if (tmpDir.equals(metaArtifactId)) {
				// artifactMetadata
				metadata = new ArtifactRepositoryMetadata(artifact);
			} else {

				String groupDir = "";
				int ctr = 0;
				for (it = pathParts.iterator(); it.hasNext();) {
					if (ctr == 0)
						groupDir = (String) it.next();
					else
						groupDir = (String) it.next() + "." + groupDir;
					ctr++;
				}

				// groupMetadata
				if (metaGroupId.equals(groupDir))
					metadata = new GroupRepositoryMetadata(metaGroupId);
			}

		} catch (FileNotFoundException fe) {
			return null;
		} catch (XmlPullParserException xe) {
			return null;
		} catch (IOException ie) {
			return null;
		}

		return metadata;
	}

	/**
	 * Scan or search for metadata files.
	 * 
	 * @param repositoryBase
	 *            The repository directory.
	 * @param blacklistedPatterns
	 *            The patterns to be exluded from the search.
	 * @return
	 */
	private String[] scanForMetadataPaths(File repositoryBase,
			String blacklistedPatterns) {

		List allExcludes = new ArrayList();
		allExcludes.addAll(FileUtils.getDefaultExcludesAsList());
		allExcludes.addAll(Arrays.asList(STANDARD_DISCOVERY_EXCLUDES));

		if (blacklistedPatterns != null && blacklistedPatterns.length() > 0) {
			allExcludes.addAll(Arrays.asList(blacklistedPatterns.split(",")));
		}

		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(repositoryBase);
		scanner.setIncludes(STANDARD_DISCOVERY_INCLUDES);
		scanner.setExcludes((String[]) allExcludes.toArray(EMPTY_STRING_ARRAY));
		scanner.scan();

		excludedPaths.addAll(Arrays.asList(scanner.getExcludedFiles()));

		return scanner.getIncludedFiles();

	}

	public Iterator getExcludedPathsIterator() {
		return excludedPaths.iterator();
	}

	public Iterator getKickedOutPathsIterator() {
		return kickedOutPaths.iterator();
	}

}
