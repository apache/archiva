package org.apache.maven.archiva.web.test;

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

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.codehaus.plexus.commandline.ExecutableResolver;
import org.codehaus.plexus.commandline.DefaultExecutableResolver;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.Collections;


/**
 * Test maven connection to archiva
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class MavenConnectionTest
    extends AbstractArchivaTestCase
{
    public static final String PATH_TO_ARCHIVA_XML = "/target/appserver-base/conf/archiva.xml";

    public static final String PATH_TO_SETTINGS_XML = "/target/local-repo/settings.xml";

    public static final String NEW_LOCAL_REPO_VALUE = "/target/local-repo";

    /**
     * @throws Exception
     */
    public void setUp()
        throws Exception
    {
        super.setUp();

        String newValue = getBasedir() + NEW_LOCAL_REPO_VALUE;
        updateXml( new File( getBasedir(), PATH_TO_ARCHIVA_XML ), newValue );
        updateXml( new File( getBasedir(), PATH_TO_SETTINGS_XML ), newValue );
    }

    /**
     * Update localRepository element value
     *
     * @param f
     * @param newValue
     * @throws Exception
     */
    private void updateXml( File f, String newValue )
        throws Exception
    {
        SAXBuilder builder = new SAXBuilder();
        FileReader reader = new FileReader( f );
        Document document = builder.build( reader );

        Element localRepository =
            (Element) XPath.newInstance( "./" + "localRepository" ).selectSingleNode( document.getRootElement() );
        localRepository.setText( newValue );

        // re-write xml file
        FileWriter writer = new FileWriter( f );
        XMLOutputter output = new XMLOutputter();
        output.output( document, writer );
    }

    private void clickManagedRepositories()
    {
        goToLoginPage();
        submitLoginPage( adminUsername, adminPassword );

        clickLinkWithText( "Managed Repositories" );
        assertPage( "Administration" );
        assertTextPresent( "Administration" );
    }

    private void removeManagedRepository( String id )
    {
        clickManagedRepositories();

        clickLinkWithLocator( "//a[contains(@href, '/admin/deleteRepository!input.action?repoId=" + id + "')]" );
        clickLinkWithLocator( "deleteRepository_operationdelete-contents", false );
        clickButtonWithValue( "Go" );

        assertPage( "Administration" );
    }

    /**
     * Click Settings from the navigation menu
     */
    private void clickProxiedRepositories()
    {
        goToLoginPage();
        submitLoginPage( adminUsername, adminPassword );

        clickLinkWithText( "Proxied Repositories" );
        assertPage( "Administration" );
        assertTextPresent( "Proxied Repositories" );
    }

    /**
     * Remove the created test repo
     */
    protected void removeProxiedRepository()
    {
        if ( !isLinkPresent( "Login" ) )
        {
            logout();
        }

        clickProxiedRepositories();

        if ( isTextPresent( "Delete Repository " ) )
        {
            clickLinkWithText( "Delete Repository" );
            assertPage( "Configuration" );
            clickLinkWithLocator( "deleteProxiedRepository_operationdelete-entry", false );
            clickButtonWithValue( "Go" );

            assertPage( "Administration" );
            assertTextNotPresent( "Test Proxied Repository" );
        }

        logout();
    }

    /**
     * Execute 'mvn' from commandline
     *
     * @param workingDir
     * @param outputFile
     * @return
     * @throws Exception
     */
    private int executeMaven( String workingDir, File outputFile )
        throws Exception
    {

        ExecutableResolver executableResolver = new DefaultExecutableResolver();

        String actualExecutable = "mvn";
        File workingDirectory = new File( workingDir );

        List path = executableResolver.getDefaultPath();

        if ( path == null )
        {
            path = Collections.EMPTY_LIST;
        }

        File e = executableResolver.findExecutable( "mvn", path );

        if ( e != null )
        {
            actualExecutable = e.getAbsolutePath();
        }

        File actualExecutableFile = new File( actualExecutable );

        if ( !actualExecutableFile.exists() )
        {
            actualExecutable = "mvn";
        }

        // Set command line
        Commandline cmd = new Commandline();

        cmd.addSystemEnvironment();

        cmd.addEnvironment( "MAVEN_TERMINATE_CMD", "on" );

        cmd.setExecutable( actualExecutable );

        cmd.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        cmd.createArgument().setValue( "clean" );

        cmd.createArgument().setValue( "install" );

        cmd.createArgument().setValue( "-s" );

        cmd.createArgument().setValue( getBasedir() + "/target/local-repo/settings.xml" );

        // Excute command

        Writer writer = new FileWriter( outputFile );

        StreamConsumer consumer = new WriterStreamConsumer( writer );

        int exitCode = CommandLineUtils.executeCommandLine( cmd, consumer, consumer );

        writer.flush();

        writer.close();

        return exitCode;
    }

    public void testBadDependency()
        throws Exception
    {
        File outputFile = new File( getBasedir(), "/target/projects/bad-dependency/bad-dependency.log" );
        int exitCode = executeMaven( getBasedir() + "/target/projects/bad-dependency", outputFile );

        assertEquals( 1, exitCode );

        File f = new File( getBasedir(),
                           "/target/local-repo/org/apache/mavem/archiva/web/test/foo-bar/1.0-SNAPSHOT/foo-bar-1.0-SNAPSHOT.jar" );
        assertTrue( !f.exists() );

        BufferedReader reader = new BufferedReader( new FileReader( outputFile ) );
        String str;
        boolean foundSnapshot = false, foundBadDep = false;

        while ( ( str = reader.readLine() ) != null )
        {
            //System.out.println( str );
            if ( str.indexOf(
                "mvn install:install-file -DgroupId=org.apache.maven.archiva.web.test -DartifactId=foo-bar" ) != -1 )
            {
                foundSnapshot = true;
            }
            else if ( str.indexOf(
                "mvn install:install-file -DgroupId=org.apache.maven.archiva.web.test -DartifactId=bad-dependency" ) !=
                -1 )
            {
                foundBadDep = true;
            }
        }

        reader.close();

        assertTrue( foundSnapshot );
        assertTrue( foundBadDep );
    }

    /*
    @todo: commented out since tests are currently failing due to MRM-323

    public void testDownloadArtifactFromManagedRepo()
        throws Exception
    {
        clickManagedRepositories();
        
        clickLinkWithText( "Add Repository" );
        assertTextPresent( "Configuration" );

        setFieldValue( "addRepository_id", "snapshots" );
        setFieldValue( "urlName", "snapshots" );
        setFieldValue( "addRepository_name", "snapshots-repository" );
        setFieldValue( "addRepository_directory", getBasedir() + "/target/snapshots" );

        clickButtonWithValue( "Add Repository" );
        assertPage( "Administration" );

        clickLinkWithText( "User Management" );
        clickLinkWithLocator( "//a[contains(@href, '/security/useredit.action?username=admin')]" );
        clickLinkWithText( "Edit Roles" );
        checkField( "addRolesToUser_addSelectedRolesRepository Observer - snapshots" );
        checkField( "addRolesToUser_addSelectedRolesRepository Manager - snapshots" );

        clickButtonWithValue( "Add Selected Roles" );
        assertPage( "[Admin] User List" );

        logout();
       
        File outputFile = new File( getBasedir(), "/target/projects/bad-dependency/bad-dependency2.log" );
        int exitCode = executeMaven( getBasedir() + "/target/projects/bad-dependency",
            outputFile );

        assertEquals( 0, exitCode );

        File f = new File( getBasedir(),
            "/target/local-repo/org/apache/maven/archiva/web/test/foo-bar-1.0-SNAPSHOT.jar" );
        assertTrue( f.exists() );

        BufferedReader reader = new BufferedReader( new FileReader( outputFile ) );
        String str;
                 
        while( ( str = reader.readLine() ) != null)
        {
            System.out.println( str );
        }
        reader.close();

        removeManagedRepository( "snapshots" );
    }


    public void testDownloadArtifactFromProxiedRepo()
        throws Exception
    {
        //add managed repository
        clickManagedRepositories();

        clickLinkWithText( "Add Repository" );
        assertTextPresent( "Configuration" );

        setFieldValue( "addRepository_id", "repository" );
        setFieldValue( "urlName", "repository" );
        setFieldValue( "addRepository_name", "repository" );
        setFieldValue( "addRepository_directory", getBasedir() + "/target/repository" );
        
        clickButtonWithValue( "Add Repository" );
        waitPage();
        assertPage( "Administration" );

        clickLinkWithText( "User Management" );
        clickLinkWithLocator( "//a[contains(@href, '/security/useredit.action?username=admin')]" );
        clickLinkWithText( "Edit Roles" );
        checkField( "addRolesToUser_addSelectedRolesRepository Observer - repository" );
        checkField( "addRolesToUser_addSelectedRolesRepository Manager - repository" );

        clickButtonWithValue( "Add Selected Roles" );
        assertPage( "[Admin] User List" );
        logout();

        //add proxied repository
        clickProxiedRepositories();
        clickLinkWithText( "Add Repository" );
        assertPage( "Configuration" );
        setFieldValue( "id", "central" );
        setFieldValue( "name", "Central Repository" );
        setFieldValue( "url", "http://mirrors.ibiblio.org/pub/mirrors/maven2" );
        clickButtonWithValue( "Add Repository" );
        waitPage();

        assertPage( "Administration" );
        assertTextPresent( "Central Repository" );
        assertLinkPresent( "Edit Repository" );

        logout();

        File outputFile = new File( getBasedir(), "/target/projects/dependency-in-proxied/dependency-in-proxied.log" );
        int exitCode = executeMaven( getBasedir() + "/target/projects/dependency-in-proxied",
            outputFile );

        assertEquals( 0, exitCode );

        File f = new File( getBasedir(),"/target/repository/com/lowagie/itext/1.3/itext-1.3.jar" );
        assertTrue( f.exists() );

        f = new File( getBasedir(), "/target/local-repo/com/lowagie/itext/1.3/itext-1.3.jar" );
        assertTrue( f.exists() );


        BufferedReader reader = new BufferedReader( new FileReader( outputFile ) );
        String str;

        while( ( str = reader.readLine() ) != null)
        {
            System.out.println( str );
        }
        reader.close();

        removeProxiedRepository();
        removeManagedRepository( "repository" );        
    }

    */

    /**
     * @throws Exception
     */
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }
}
