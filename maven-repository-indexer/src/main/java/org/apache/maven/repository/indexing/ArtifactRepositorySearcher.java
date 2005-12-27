package org.apache.maven.repository.indexing;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * This class searches the index for existing artifacts that contains the
 * specified query string.
 * 
 */
public class ArtifactRepositorySearcher implements RepositorySearcher {

	private IndexSearcher searcher;
	private ArtifactRepository repository;
	private ArtifactFactory factory;
	private static final String NAME = "name";
	private static final String GROUPID = "groupId";
	private static final String ARTIFACTID = "artifactId";
	private static final String VERSION = "version";
	private static final String JAR_TYPE = "jar";
	private static final String XML_TYPE = "xml";
	private static final String POM_TYPE = "pom";
	
	/**
	 * Constructor
	 * 
	 * @param indexPath
	 * @param repository
	 */
	public ArtifactRepositorySearcher(String indexPath,
			ArtifactRepository repository) {

		this.repository = repository;
		factory = new DefaultArtifactFactory();

		try {
			searcher = new IndexSearcher(indexPath);
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}
	
	protected Analyzer getAnalyzer()
    {
        //PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(new SimpleAnalyzer());
        //wrapper.addAnalyzer(VERSION, new StandardAnalyzer());
        
		//return wrapper;
        return new ArtifactRepositoryIndexAnalyzer(new SimpleAnalyzer());
    }

	/**
	 * Search the artifact that contains the query string in the specified
	 * search field.
	 * 
	 * @param queryString
	 * @param searchField
	 * @return
	 */
	public List searchArtifact(String queryString, String searchField) {

		
		QueryParser parser = new QueryParser(searchField,
				getAnalyzer());
		Query qry = null;
		List artifactList = new ArrayList();

		try {
			qry = parser.parse(queryString);
		} catch (ParseException pe) {
			pe.printStackTrace();
			return artifactList;
		}

		try {
			Hits hits = searcher.search(qry);
			 //System.out.println("HITS SIZE --> " + hits.length());

			for (int i = 0; i < hits.length(); i++) {
				Document doc = hits.doc(i);
				// System.out.println("===========================");
				// System.out.println("NAME :: " + (String) doc.get(NAME));
				// System.out.println("GROUP ID :: " + (String)
				// doc.get(GROUPID));
				// System.out.println("ARTIFACT ID :: " + (String)
				// doc.get(ARTIFACTID));
				//System.out.println("VERSION :: " + (String)
				// doc.get(VERSION));
				// System.out.println("SHA! :: " + (String) doc.get(SHA1));
				// System.out.println("MD5 :: " + (String) doc.get(MD5));
				// System.out.println("CLASSES :: " + (String)
				// doc.get(CLASSES));
				// System.out.println("PACKAGES :: " + (String)
				// doc.get(PACKAGES));
				// System.out.println("FILES :: " + (String) doc.get(FILES));
				// System.out.println("===========================");

				String name = (String) doc.get(NAME);
				String type = "";
				if ((name.substring(name.length() - 3).toLowerCase())
						.equals(JAR_TYPE))
					type = JAR_TYPE;
				else if ((name.substring(name.length() - 3).toLowerCase())
						.equals(XML_TYPE)
						|| (name.substring(name.length() - 3).toLowerCase())
								.equals(POM_TYPE))
					type = POM_TYPE;

				if (!type.equals("") && type != null) {
					ArtifactHandler handler = new DefaultArtifactHandler(type);
					VersionRange version = VersionRange
							.createFromVersion((String) doc.get(VERSION));

					Artifact artifact = new DefaultArtifact((String) doc
							.get(GROUPID), (String) doc.get(ARTIFACTID),
							version, "compile", type, "", handler);

					/*
					 * Artifact artifact = factory.createArtifact((String)
					 * doc.get(GROUPID), (String) doc.get(ARTIFACTID), (String)
					 * doc.get(VERSION), "", type);
					 */
					artifact.setRepository(repository);
					artifact.setFile(new File(repository.getBasedir() + "/"
							+ (String) doc.get(NAME)));

					artifactList.add(artifact);
				}
			}

		} catch (IOException ie) {
			ie.printStackTrace();
			return artifactList;
		}

		return artifactList;
	}
}
