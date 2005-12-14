package org.apache.maven.repository.reporting;

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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This class validates well-formedness of pom xml file.
 */
public class InvalidPomArtifactReportProcessor
    implements ArtifactReportProcessor
{
   
    /**
     * @param model
     * @param artifact The pom xml file to be validated, passed as an artifact object.
     * @param reporter The artifact reporter object.
     * @param repository the repository where the artifact is located.
     */
    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                ArtifactRepository repository )
    {
        InputStream is = null;        
        
        if((artifact.getType().toLowerCase()).equals("pom")){
            
            if(repository.getProtocol().equals("file")){
                try{
                    is = new FileInputStream(repository.getBasedir() + artifact.getGroupId() + "/" + 
                         artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/" + 
                         artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "." + 
                         artifact.getType());
                }catch(FileNotFoundException fe){
                    reporter.addFailure(artifact, "Artifact not found.");
                }
            }else{
                try{
                    URL url = new URL(repository.getUrl() + artifact.getGroupId() + "/" + 
                         artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/" + 
                         artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "." + 
                         artifact.getType());
                    is = url.openStream();
                    
                }catch(MalformedURLException me){
                    reporter.addFailure(artifact, "Error retrieving artifact from remote repository.");
                }catch(IOException ie){
                    reporter.addFailure(artifact, "Error retrieving artifact from remote repository.");
                }
            }
            
            Reader reader = new InputStreamReader(is);
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            
            try{
                Model pomModel = pomReader.read(reader);
                reporter.addSuccess(artifact);
            }catch(XmlPullParserException xe){
                reporter.addFailure(artifact, "The pom xml file is not well-formed. Error while parsing.");                
            }catch(IOException oe){
                reporter.addFailure(artifact, "Error while reading the pom xml file.");
            }
            
        }else{
            reporter.addWarning(artifact, "The artifact is not a pom xml file.");
        }
    }

}
