package org.apache.archiva.common.filelock;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Olivier Lamy
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml" } )
public class DefaultFileLockManagerTest
{

    final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "fileLockManager#default" )
    FileLockManager fileLockManager;

    class ConcurentFileWrite
        extends MultithreadedTestCase
    {

        FileLockManager fileLockManager;

        File file = new File( System.getProperty( "buildDirectory" ), "foo.txt" );

        File largeJar = new File( System.getProperty( "basedir" ), "src/test/cassandra-all-2.0.3.jar" );

        ConcurentFileWrite( FileLockManager fileLockManager )
            throws IOException
        {
            this.fileLockManager = fileLockManager;
            file.createNewFile();

        }

        @Override
        public void initialize()
        {

        }

        public void thread1()
            throws FileLockException, IOException
        {
            logger.info( "thread1" );
            Lock lock = fileLockManager.writeFileLock( this.file );
            try
            {
                lock.getFile().delete();
                FileUtils.copyFile( largeJar, lock.getFile() );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread1 ok" );
        }

        public void thread2()
            throws FileLockException, IOException
        {
            logger.info( "thread2" );
            Lock lock = fileLockManager.writeFileLock( this.file );
            try
            {
                lock.getFile().delete();
                FileUtils.copyFile( largeJar, lock.getFile() );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread2 ok" );
        }

        public void thread3()
            throws FileLockException, IOException
        {
            logger.info( "thread3" );
            Lock lock = fileLockManager.readFileLock( this.file );
            try
            {
                IOUtils.copy( new FileInputStream( lock.getFile() ),
                              new FileOutputStream( File.createTempFile( "foo", ".jar" ) ) );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread3 ok" );
        }

        public void thread4()
            throws FileLockException, IOException
        {
            logger.info( "thread4" );
            Lock lock = fileLockManager.writeFileLock( this.file );
            try
            {
                lock.getFile().delete();
                FileUtils.copyFile( largeJar, lock.getFile() );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread4 ok" );
        }

        public void thread5()
            throws FileLockException, IOException
        {
            logger.info( "thread5" );
            Lock lock = fileLockManager.writeFileLock( this.file );
            try
            {
                lock.getFile().delete();
                FileUtils.copyFile( largeJar, lock.getFile() );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread5 ok" );
        }

        public void thread6()
            throws FileLockException, IOException
        {
            logger.info( "thread6" );
            Lock lock = fileLockManager.readFileLock( this.file );
            try
            {
                IOUtils.copy( new FileInputStream( lock.getFile() ),
                              new FileOutputStream( File.createTempFile( "foo", ".jar" ) ) );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread6 ok" );
        }

        public void thread7()
            throws FileLockException, IOException
        {
            logger.info( "thread7" );
            Lock lock = fileLockManager.writeFileLock( this.file );
            try
            {
                lock.getFile().delete();
                FileUtils.copyFile( largeJar, lock.getFile() );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread7 ok" );
        }

        public void thread8()
            throws FileLockException, IOException
        {
            logger.info( "thread8" );
            Lock lock = fileLockManager.readFileLock( this.file );
            try
            {
                IOUtils.copy( new FileInputStream( lock.getFile() ),
                              new FileOutputStream( File.createTempFile( "foo", ".jar" ) ) );
            }
            finally
            {
                fileLockManager.release( lock );
            }
            logger.info( "thread8 ok" );
        }



    }

    @Test
    public void testWrite()
        throws Throwable
    {
        ConcurentFileWrite concurentFileWrite = new ConcurentFileWrite( fileLockManager );
        //concurentFileWrite.setTrace( true );
        TestFramework.runOnce( concurentFileWrite );
    }

}
