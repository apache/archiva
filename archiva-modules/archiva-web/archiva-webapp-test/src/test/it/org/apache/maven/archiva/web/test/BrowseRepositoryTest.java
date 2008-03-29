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

/**
 * Archiva's webapp UI test for browsing the indexed repository.
 * 
 * @author <a href="mailto:nramirez@exist.com">Napoleon Esmundo C. Ramirez</a>
 */
public class BrowseRepositoryTest
    extends AbstractArchivaTestCase
{
    private void browseArtifact()
    {
        clickLinkWithText( "Browse" );
        assertPage( "Browse Repository" );
        assertTextPresent( "Browse Repository" );
        assertLinkPresent( "org.apache.maven.archiva.web.test/" );
        
        clickLinkWithText( "org.apache.maven.archiva.web.test/" );
        assertPage( "Browse Repository" );
        assertTextPresent( "Browse Repository" );
        assertTextPresent( "Artifacts" );
        assertLinkPresent( "artifact-a/" );
        
        clickLinkWithText( "artifact-a/" );
        assertPage( "Browse Repository" );
        assertTextPresent( "Browse Repository" );
        assertTextPresent( "Versions" );
        assertLinkPresent( "1.0/" );
        
        clickLinkWithText( "1.0/" );
        assertPage( "Browse Repository" );
        assertTextPresent( "artifact-a" );
        assertLinkPresent( "Main" );
    }
    
    public void testBrowseArtifactInfo()
    {
        browseArtifact();
        
        assertTextPresent( "Info" );
        assertLinkPresent( "Dependencies" );
        assertLinkPresent( "Dependency Tree" );
        assertLinkPresent( "Used By" );
        assertLinkPresent( "Mailing Lists" );
        
        assertEquals("Group ID", getSelenium().getTable("//table[1].0.0"));
        assertEquals("org.apache.maven.archiva.web.test", getSelenium().getTable("//table[1].0.1"));
        assertEquals("Artifact ID", getSelenium().getTable("//table[1].1.0"));
        assertEquals("artifact-a", getSelenium().getTable("//table[1].1.1"));
        assertEquals("Version", getSelenium().getTable("//table[1].2.0"));
        assertEquals("1.0", getSelenium().getTable("//table[1].2.1"));
        assertEquals("Packaging", getSelenium().getTable("//table[1].3.0"));
        assertEquals("jar", getSelenium().getTable("//table[1].3.1"));
    }
    
    public void testBrowseArtifactDependencies()
    {
        browseArtifact();
        
        clickLinkWithText( "Dependencies" );
        assertLinkPresent( "Info" );
        assertTextPresent( "Dependencies" );
        assertLinkPresent( "Dependency Tree" );
        assertLinkPresent( "Used By" );
        assertLinkPresent( "Mailing Lists" );
        
        assertPage( "Browse Repository" );
        assertTextPresent( "artifact-a" );
        assertLinkPresent( "artifact-b" );
    }
    
    public void testBrowseArtifactDependencyTree()
    {
        browseArtifact();
        
        clickLinkWithText( "Dependency Tree" );
        assertLinkPresent( "Info" );
        assertLinkPresent( "Dependencies" );
        assertTextPresent( "Dependency Tree" );
        assertLinkPresent( "Used By" );
        assertLinkPresent( "Mailing Lists" );
        
        assertPage( "Browse Repository" );
        assertTextPresent( "artifact-a" );
        assertLinkPresent( "artifact-b" );
        assertLinkPresent( "artifact-c" );
    }
    
    public void testBrowseArtifactUsedBy()
    {
        browseArtifact();
        
        clickLinkWithText( "Used By" );
        assertLinkPresent( "Info" );
        assertLinkPresent( "Dependencies" );
        assertLinkPresent( "Dependency Tree" );
        assertTextPresent( "Used By" );
        assertLinkPresent( "Mailing Lists" );
        
        assertPage( "Browse Repository" );
        assertTextPresent( "artifact-a" );
        assertLinkPresent( "artifact-s" );
    }
    
    public void testBrowseArtifactMailingLists()
    {
        browseArtifact();
        
        clickLinkWithText( "Mailing Lists" );
        assertLinkPresent( "Info" );
        assertLinkPresent( "Dependencies" );
        assertLinkPresent( "Dependency Tree" );
        assertLinkPresent( "Used By" );
        assertTextPresent( "Mailing Lists" );
        
        assertPage( "Browse Repository" );
        assertTextPresent( "artifact-a" );
        assertTextPresent( "No mailing lists" );
    }
    
    public void testBrowseUpRepositoryDirectory()
    {
        browseArtifact();
        
        assertLinkPresent( "artifact-a" );
        
        clickLinkWithText( "artifact-a" );
        assertPage( "Browse Repository" );
        assertTextPresent( "Browse Repository" );
        assertTextPresent( "artifact-a" );
        assertTextPresent( "Versions" );
        assertLinkPresent( "1.0/" );
        assertLinkPresent( "test" );
        
        clickLinkWithText( "test" );
        assertPage( "Browse Repository" );
        assertTextPresent( "Browse Repository" );
        assertTextPresent( "Artifacts" );
        assertLinkPresent( "artifact-a/" );
        assertLinkPresent( "web" );
        
        clickLinkWithText( "web" );
        assertPage( "Browse Repository" );
        assertTextPresent( "Browse Repository" );
        assertTextPresent( "Groups" );
        assertLinkPresent( "org.apache.maven.archiva.web.test/" );
        assertLinkPresent( "[top]" );
        
        clickLinkWithText( "[top]" );
        assertPage( "Browse Repository" );
        assertTextPresent( "Browse Repository" );
        assertTextPresent( "Groups" );
        assertLinkPresent( "org.apache.maven.archiva.web.test/" );
    }
    
    public void testBrowseDependencyArtifact()
    {
        browseArtifact();
        
        clickLinkWithText( "Dependencies" );
        assertPage( "Browse Repository" );
        assertTextPresent( "artifact-a" );
        assertLinkPresent( "artifact-b" );
        
        clickLinkWithText( "artifact-b" );
        assertEquals("Group ID", getSelenium().getTable("//table[1].0.0"));
        assertEquals("org.apache.maven.archiva.web.test", getSelenium().getTable("//table[1].0.1"));
        assertEquals("Artifact ID", getSelenium().getTable("//table[1].1.0"));
        assertEquals("artifact-b", getSelenium().getTable("//table[1].1.1"));
        assertEquals("Version", getSelenium().getTable("//table[1].2.0"));
        assertEquals("2.0", getSelenium().getTable("//table[1].2.1"));
        assertEquals("Packaging", getSelenium().getTable("//table[1].3.0"));
        assertEquals("jar", getSelenium().getTable("//table[1].3.1"));
    }
}
