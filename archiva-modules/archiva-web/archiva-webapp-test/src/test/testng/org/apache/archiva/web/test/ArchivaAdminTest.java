package org.apache.archiva.web.test;

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

import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.Collections;

import org.apache.archiva.web.test.parent.AbstractArchivaTest;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.codehaus.plexus.commandline.ExecutableResolver;
import org.codehaus.plexus.commandline.DefaultExecutableResolver;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

@Test( groups = { "about" }, alwaysRun = true )
public class ArchivaAdminTest 
	extends AbstractArchivaTest
{
	public static final String PATH_TO_ARCHIVA_XML = "/target/appserver-base/conf/archiva.xml";

    public static final String PATH_TO_SETTINGS_XML = "/target/local-repo/settings.xml";

    public static final String NEW_LOCAL_REPO_VALUE = "/target/local-repo";
	
    @BeforeSuite
    public void initializeContinuum()
        throws Exception
    {
        super.open();
        getSelenium().open( baseUrl );
        String title = getSelenium().getTitle();
        if ( title.equals( "Apache Archiva \\ Create Admin User" ) )
        {
            assertCreateAdmin();
            String fullname = p.getProperty( "ADMIN_FULLNAME" );
            String username = p.getProperty( "ADMIN_USERNAME" );
            String mail = p.getProperty( "ADMIN_EMAIL" );
            String password = p.getProperty( "ADMIN_PASSWORD" );
            submitAdminData( fullname, mail, password );            
            assertAuthenticatedPage( username );
            submit();
            clickLinkWithText( "Logout" );
       }
        super.close();
    }

    @BeforeTest( groups = { "about" } )
    public void open()
        throws Exception
    {
        super.open();
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
    
    /*private void clickRepositories()
    {
    	goToLoginPage();
    	submitLoginPage( getAdminUsername() , getAdminPassword() );
    	clickLinkWithText( "Repositories" );
    	assertPage( "Apache Archiva \\ Administration" );
    	assertTextPresent( "Administration - Repositories" );
    }
    
    private void removedManagedRepository( String id)
    {
    	clickRepositories();
    	clickLinkWithLocator( "//a[contains(@href, '/admin/confirmDeleteRepository.action?repoid=" + id + "')]" );
    	clickButtonWithValue( "Delete Configuration and Contents" );
    }*/
    
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
    
/*	public void testBadDependency()
	    throws Exception
	{
	    File outputFile = new File( getBasedir(), "/target/projects/bad-dependency/bad-dependency.log" );
	    int exitCode = executeMaven( getBasedir() + "/target/projects/bad-dependency", outputFile );
	
	    Assert.assertEquals( 1, exitCode );
	
	    File f = new File( getBasedir(),
	                       "/target/local-repo/org/apache/maven/archiva/web/test/foo-bar/1.0-SNAPSHOT/foo-bar-1.0-SNAPSHOT.jar" );
	    Assert.assertTrue( !f.exists() );
	
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
	    
	    Assert.assertTrue( foundSnapshot );
	    Assert.assertTrue( foundBadDep );
	}*/
	
    public void displayLandingPage()
    {
        getSelenium().open( baseUrl );
        getSelenium().waitForPageToLoad( maxWaitTimeInMs );
        assertPage( "Apache Archiva \\ Quick Search" );
    }

    @Override
    @AfterTest( groups = { "about" } )
    public void close()
  	throws Exception
    {
        super.close();
    }
}