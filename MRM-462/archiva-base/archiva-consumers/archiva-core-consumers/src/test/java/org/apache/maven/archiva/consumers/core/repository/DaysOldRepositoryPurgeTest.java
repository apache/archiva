package org.apache.maven.archiva.consumers.core.repository;

import org.apache.maven.archiva.model.ArchivaArtifact;

import java.io.File;
import java.util.Date;


/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DaysOldRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{
    public static final String PATH_TO_BY_DAYS_OLD_ARTIFACT =
        "org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        repoPurge =
            new DaysOldRepositoryPurge( getRepository(), getLayout(), dao, getRepoConfiguration().getDaysOlder() );
    }

    private void setLastModified()
    {
        File dir =
            new File( "target/test-classes/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/" );
        File[] contents = dir.listFiles();
        for ( int i = 0; i < contents.length; i++ )
        {
            contents[i].setLastModified( 1179382029 );
        }
    }

    public void testIfAJarIsFound()
        throws Exception
    {
        populateDb();

        setLastModified();

        repoPurge.process( PATH_TO_BY_DAYS_OLD_ARTIFACT );

        assertTrue( true );

        assertFalse( new File(
            "target/test-classes/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.sha1" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.md5" ).exists() );
        assertFalse( new File(
            "target/test-classes/test-repo/org/apache/maven/plugins/maven-install-plugin/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.sha1" ).exists() );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        repoPurge = null;
    }

    private void populateDb()
        throws Exception
    {
        // Create it
        ArchivaArtifact artifact =
            dao.createArtifact( "org.apache.maven.plugins", "maven-install-plugin", "2.2-SNAPSHOT", "", "jar" );
        assertNotNull( artifact );

        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );

        // Save it.
        ArchivaArtifact savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        //POM
        artifact = dao.createArtifact( "org.apache.maven.plugins", "maven-install-plugin", "2.2-SNAPSHOT", "", "pom" );
        assertNotNull( artifact );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );
        savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );
    }
}
