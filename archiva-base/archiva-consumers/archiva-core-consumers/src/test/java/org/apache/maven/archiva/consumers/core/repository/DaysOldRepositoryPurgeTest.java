package org.apache.maven.archiva.consumers.core.repository;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.jdo.JdoAccess;

import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOHelper;
import javax.jdo.spi.JDOImplHelper;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.io.File;


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

        lookupRepositoryPurge( "days-old" );
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
        // Create it
        ArchivaArtifact artifact =
            dao.createArtifact( "org.apache.maven.plugins", "maven-install-plugin", "2.2-SNAPSHOT", "", "jar" );
        assertNotNull( artifact );

        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setOrigin( "test" );

        // Save it.
        ArchivaArtifact savedArtifact = dao.saveArtifact( artifact );
        assertNotNull( savedArtifact );

        setLastModified();

        repoPurge.process( PATH_TO_BY_DAYS_OLD_ARTIFACT, getRepoConfiguration() );

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
}
