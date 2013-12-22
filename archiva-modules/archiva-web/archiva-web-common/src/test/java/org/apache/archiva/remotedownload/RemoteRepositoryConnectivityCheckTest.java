package org.apache.archiva;

import com.google.common.io.Files;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.remotedownload.AbstractDownloadTest;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * @author Olivier Lamy
 */
public class RemoteRepositoryConnectivityCheckTest
    extends AbstractDownloadTest
{

    @BeforeClass
    public static void setAppServerBase()
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        System.setProperty( "appserver.base", "target/" + RemoteRepositoryConnectivityCheckTest.class.getName() );
    }


    @AfterClass
    public static void resetAppServerBase()
    {
        System.setProperty( "appserver.base", previousAppServerBase );
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-test-common.xml classpath*:spring-context-artifacts-download.xml";
    }

    @Test
    public void checkRemoteConnectivity()
        throws Exception
    {

        Server repoServer =
            buildStaticServer( new File( System.getProperty( "basedir" ) + "/src/test/repositories/test-repo" ) );
        repoServer.start();

        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );

        try
        {

            int repoServerPort = repoServer.getConnectors()[0].getLocalPort();

            RemoteRepository repo = getRemoteRepository();

            repo.setUrl( "http://localhost:" + repoServerPort );

            service.addRemoteRepository( repo );

            service.checkRemoteConnectivity( repo.getId() );
        }
        finally
        {
            service.deleteRemoteRepository( "id-new" );
            repoServer.stop();
        }
    }

    @Test
    public void checkRemoteConnectivityEmptyRemote()
        throws Exception
    {

        File tmpDir = Files.createTempDir();
        Server repoServer = buildStaticServer( tmpDir );
        repoServer.start();

        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );

        try
        {

            int repoServerPort = repoServer.getConnectors()[0].getLocalPort();

            RemoteRepository repo = getRemoteRepository();

            repo.setUrl( "http://localhost:" + repoServerPort );

            service.addRemoteRepository( repo );

            service.checkRemoteConnectivity( repo.getId() );
        }
        finally
        {
            service.deleteRemoteRepository( "id-new" );
            FileUtils.deleteQuietly( tmpDir );
            repoServer.stop();
        }
    }

    protected Server buildStaticServer( File path )
    {
        Server repoServer = new Server( 0 );

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed( true );
        resourceHandler.setWelcomeFiles( new String[]{ "index.html" } );
        resourceHandler.setResourceBase( path.getAbsolutePath() );

        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[]{ resourceHandler, new DefaultHandler() } );
        repoServer.setHandler( handlers );

        return repoServer;
    }


    RemoteRepository getRemoteRepository()
    {
        return new RemoteRepository( "id-new", "new one", "http://foo.com", "default", "foo", "foopassword", 120,
                                     "cool repo" );
    }

}
