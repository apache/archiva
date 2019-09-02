/*
 * Copyright 2014 The Apache Software Foundation.
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
package org.apache.archiva.webdav;

import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rule to help creating folder for repository based on testmethod name
 * @author Eric
 */
public class ArchivaTemporaryFolderRule implements TestRule {



    private Path d;
    private Description desc = Description.EMPTY;

    private AtomicReference<Path> projectBase = new AtomicReference<>( );

    private Path getProjectBase() {
        if (this.projectBase.get()==null) {
            String pathVal = System.getProperty("mvn.project.base.dir");
            Path baseDir;
            if ( StringUtils.isEmpty(pathVal)) {
                baseDir= Paths.get("").toAbsolutePath();
            } else {
                baseDir = Paths.get(pathVal).toAbsolutePath();
            }
            this.projectBase.compareAndSet(null, baseDir);
        }
        return this.projectBase.get();
    }

    public void before() throws IOException {
        // hard coded maven target file
        Path f1 = getProjectBase().resolve("target/archivarepo").resolve(ArchivaTemporaryFolderRule.resumepackage(desc.getClassName())).resolve(desc.getMethodName());
        d = Files.createDirectories( f1 );
    }

    public Path getRoot() {
        return d;
    }

    public void after() throws IOException {
        org.apache.archiva.common.utils.FileUtils.deleteDirectory(getRoot());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        desc = description;
        return statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }
    /**
     * Return a filepath from FQN class name with only first char of package and classname
     * @param packagename
     * @return 
     */ 
    public static String resumepackage(String packagename) {
        StringBuilder sb = new StringBuilder();
        String[] p = packagename.split("\\.");
        for (int i = 0; i < p.length - 2; i++) 
        {
            sb.append(p[i].charAt(0)).append( FileSystems.getDefault( ).getSeparator());
        }
        sb.append(p[p.length - 1]);
        return sb.toString();
    }
    
}
