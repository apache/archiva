package org.apache.archiva.common.filelock;

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

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;

//import org.apache.commons.io.IOUtils;

/**
 * @author Olivier Lamy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:/META-INF/spring-context.xml"})
public class DefaultFileLockManagerTest {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @Named(value = "fileLockManager#default")
    FileLockManager fileLockManager;

    class ConcurrentFileWrite
            extends MultithreadedTestCase {


        AtomicInteger success = new AtomicInteger(0);

        FileLockManager fileLockManager;

        Path file = Paths.get(System.getProperty("buildDirectory"), "foo.txt");

        Path largeJar = Paths.get(System.getProperty("basedir"), "src/test/cassandra-all-2.0.3.jar");

        ConcurrentFileWrite(FileLockManager fileLockManager)
                throws IOException {
            this.fileLockManager = fileLockManager;
            //file.createNewFile();

        }

        @Override
        public void initialize() {

        }

        // Files.copy is not atomic so have to try several times in
        // a multithreaded test
        private void copyFile(Path source, Path destination) {
            int attempts = 10;
            boolean finished = false;
            while (!finished && attempts-- > 0) {
                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                    finished = true;
                } catch (IOException ex) {
                    //
                }
            }
        }

        public void thread1()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread1");
                Lock lock = fileLockManager.writeFileLock(this.file);
                try {
                    Files.deleteIfExists(lock.getFile());
                    copyFile(largeJar, lock.getFile());
                } finally {
                    fileLockManager.release(lock);
                }
                logger.info("thread1 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }

        public void thread2()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread2");
                Lock lock = fileLockManager.writeFileLock(this.file);
                try {
                    Files.deleteIfExists(lock.getFile());
                    copyFile(largeJar, lock.getFile());
                } finally {
                    fileLockManager.release(lock);
                }
                logger.info("thread2 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }

        public void thread3()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread3");
                Lock lock = fileLockManager.readFileLock(this.file);
                Path outFile = null;
                try {
                    outFile = Files.createTempFile("foo", ".jar");
                    Files.copy(lock.getFile(),
                            Files.newOutputStream(outFile));
                } finally {
                    fileLockManager.release(lock);
                    if (outFile!=null) Files.delete( outFile );
                }
                logger.info("thread3 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }

        public void thread4()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread4");
                Lock lock = fileLockManager.writeFileLock(this.file);
                try {
                    Files.deleteIfExists(lock.getFile());
                    copyFile(largeJar, lock.getFile());
                } finally {
                    fileLockManager.release(lock);
                }
                logger.info("thread4 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }

        public void thread5()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread5");
                Lock lock = fileLockManager.writeFileLock(this.file);
                try {
                    Files.deleteIfExists(lock.getFile());
                    copyFile(largeJar, lock.getFile());
                } finally {
                    fileLockManager.release(lock);
                }
                logger.info("thread5 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }

        public void thread6()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread6");
                Lock lock = fileLockManager.readFileLock(this.file);
                Path outFile = null;
                try {
                    outFile = Files.createTempFile("foo", ".jar");
                    Files.copy(lock.getFile(), Files.newOutputStream( outFile ));
                } finally {
                    fileLockManager.release(lock);
                    if (outFile!=null) Files.delete( outFile );
                }
                logger.info("thread6 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }

        public void thread7()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread7");
                Lock lock = fileLockManager.writeFileLock(this.file);
                try {
                    Files.deleteIfExists(lock.getFile());
                    copyFile(largeJar, lock.getFile());
                } finally {
                    fileLockManager.release(lock);
                }
                logger.info("thread7 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }

        public void thread8()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread8");
                Lock lock = fileLockManager.readFileLock(this.file);
                Path outFile = null;
                try {
                    outFile = Files.createTempFile("foo", ".jar");
                    Files.copy(lock.getFile(), Files.newOutputStream( outFile ));
                } finally {
                    fileLockManager.release(lock);
                    if (outFile!=null) Files.delete( outFile );
                }
                logger.info("thread8 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }

        public void thread9()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread9");
                Lock lock = fileLockManager.writeFileLock(this.file);
                try {
                    Files.deleteIfExists(lock.getFile());
                    copyFile(largeJar, lock.getFile());
                } finally {
                    fileLockManager.release(lock);
                }
                logger.info("thread9 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }

        public void thread10()
                throws FileLockException, FileLockTimeoutException, IOException {
            try {
                logger.info("thread10");
                Lock lock = fileLockManager.readFileLock(this.file);
                Path outFile = null;
                try {
                    outFile = Files.createTempFile("foo", ".jar");
                    Files.copy(lock.getFile(), Files.newOutputStream( outFile ));
                } finally {
                    fileLockManager.release(lock);
                    if (outFile!=null) Files.delete(outFile);
                }
                logger.info("thread10 ok");
                success.incrementAndGet();
            } catch (Throwable e) {
                logger.error("Error occured {}", e.getMessage());
                e.printStackTrace();
                throw e;
            }

        }


    }


    @Before
    public void initialize() {
        fileLockManager.setSkipLocking(false);
        fileLockManager.clearLockFiles();
    }

    @Test
    public void testWrite()
            throws Throwable {
        ConcurrentFileWrite concurrentFileWrite = new ConcurrentFileWrite(fileLockManager);
        //concurrentFileWrite.setTrace( true );
        TestFramework.runManyTimes(concurrentFileWrite, 10, TestFramework.DEFAULT_CLOCKPERIOD, 20);
        logger.info("success: {}", concurrentFileWrite.success);
        Assert.assertEquals(100, concurrentFileWrite.success.intValue());
    }


}
