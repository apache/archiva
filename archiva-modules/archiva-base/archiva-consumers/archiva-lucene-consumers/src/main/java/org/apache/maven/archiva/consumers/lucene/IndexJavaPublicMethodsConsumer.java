package org.apache.maven.archiva.consumers.lucene;

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

import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.updater.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.bytecode.BytecodeRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * IndexJavaPublicMethodsConsumer 
 *
 *         <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer"
 *                   role-hint="index-public-methods"
 *                   instantiation-strategy="per-lookup"
 */
public class IndexJavaPublicMethodsConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseUnprocessedArtifactConsumer
{
    /**
     * @plexus.configuration default-value="index-public-methods"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Index the java public methods for Full Text Search."
     */
    private String description;
    
    /**
     * @plexus.requirement role-hint="lucene"
     */
    private RepositoryContentIndexFactory repoIndexFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repoFactory;
    
    private static final String CLASSES = "classes";
    
    private static final String METHODS = "methods";
    
    private List<String> includes = new ArrayList<String>();

    public IndexJavaPublicMethodsConsumer()
    {
        includes.add( "jar" );
        includes.add( "war" );
        includes.add( "ear" );
        includes.add( "zip" );
        includes.add( "tar.gz" );
        includes.add( "tar.bz2" );
        includes.add( "car" );
        includes.add( "sar" );
        includes.add( "mar" );
        includes.add( "rar" );
    }
    
    public void beginScan()
    {
        // TODO Auto-generated method stubx        
    }

    public void completeScan()
    {
        // TODO Auto-generated method stub

    }

    public List<String> getIncludedTypes()
    {   
        return includes;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {   
        try
        {
            ManagedRepositoryContent repoContent =
                repoFactory.getManagedRepositoryContent( artifact.getModel().getRepositoryId() );    
            File file = new File( repoContent.getRepoRoot(), repoContent.toPath( artifact ) );
            
            if( file.getAbsolutePath().endsWith( ".jar" ) || file.getAbsolutePath().endsWith( ".war" ) || 
                    file.getAbsolutePath().endsWith( ".ear" ) || file.getAbsolutePath().endsWith( ".zip" ) || 
                    file.getAbsolutePath().endsWith( ".tar.gz" ) || file.getAbsolutePath().endsWith( ".tar.bz2" ) ||
                    file.getAbsolutePath().endsWith( ".car" ) || file.getAbsolutePath().endsWith( ".sar" ) ||
                    file.getAbsolutePath().endsWith( ".mar" ) || file.getAbsolutePath().endsWith( ".rar" ) )
            {            
                if( file.exists() )
                {
                    List<String> files = readFilesInArchive( file );
                    Map<String, List<String>> mapOfClassesAndMethods =
                        getPublicClassesAndMethodsFromFiles( file.getAbsolutePath(), files );
                    
                    // NOTE: what about public variables? should these be indexed too?
                    RepositoryContentIndex bytecodeIndex = repoIndexFactory.createBytecodeIndex( repoContent.getRepository() );
                    
                    artifact.getModel().setRepositoryId( repoContent.getId() );
                    
                    BytecodeRecord bytecodeRecord = new BytecodeRecord();
                    bytecodeRecord.setFilename( file.getName() );
                    bytecodeRecord.setClasses( mapOfClassesAndMethods.get( CLASSES ) );
                    bytecodeRecord.setFiles( files );
                    bytecodeRecord.setMethods( mapOfClassesAndMethods.get( METHODS ) );
                    bytecodeRecord.setArtifact( artifact );
                    bytecodeRecord.setRepositoryId( repoContent.getId() );
                    bytecodeIndex.modifyRecord( bytecodeRecord );
                }
            }
        } 
        catch ( RepositoryException e )
        {
            throw new ConsumerException( "Can't run index cleanup consumer: " + e.getMessage() );
        }
        catch ( RepositoryIndexException e )
        {
            throw new ConsumerException( "Error encountered while adding artifact to index: " + e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( "Error encountered while getting file contents: " + e.getMessage() );
        }
    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        return false;
    }
    
    private List<String> readFilesInArchive( File file )
        throws IOException
    {
        ZipFile zipFile = new ZipFile( file );
        List<String> files;
        
        try
        {
            files = new ArrayList<String>( zipFile.size() );    
            for ( Enumeration entries = zipFile.entries(); entries.hasMoreElements(); )
            {
                ZipEntry entry = (ZipEntry) entries.nextElement();                
                files.add( entry.getName() );
            }
        }
        finally
        {
            closeQuietly( zipFile );
        }
        return files;
    }
    
    private void closeQuietly( ZipFile zipFile )
    {
        try
        {
            if ( zipFile != null )
            {
                zipFile.close();
            }
        }
        catch ( IOException e )
        {
            // ignored
        }
    }
    
    private static boolean isClass( String name )
    {   
        return name.endsWith( ".class" ) && name.lastIndexOf( "$" ) < 0;
    }
    
    private Map<String, List<String>> getPublicClassesAndMethodsFromFiles( String zipFile, List<String> files )
    {
        Map<String, List<String>> map = new HashMap<String, List<String>>(); 
        List<String> methods = new ArrayList<String>();
        List<String> classes = new ArrayList<String>();
                
        for( String file : files )
        {               
            if( isClass( file ) )
            {
                try
                {
                    ClassParser parser = new ClassParser( zipFile, file );
                    JavaClass javaClass = parser.parse();
                    
                    if( javaClass.isPublic() )
                    {
                        classes.add( javaClass.getClassName() );
                    }                    
                    
                    Method[] methodsArr = javaClass.getMethods();
                    for( Method method : methodsArr )
                    {   
                        if( method.isPublic() )
                        {                            
                            methods.add( method.getName() );
                        }
                    }
                }
                catch ( IOException e )
                {   
                    // ignore
                }
            }
        }
        
        map.put( CLASSES, classes );
        map.put( METHODS, methods );
        
        return map;
    }

}
