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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Rule to help creating folder for repository based on testmethod name
 * @author Eric
 */
public class ArchivaTemporaryFolderRule implements TestRule {
    private File d;
    private Description desc = Description.EMPTY;

    public void before() throws IOException {
        // hard coded maven target file
        File f1 = new File("target" + File.separator + "archivarepo" + File.separator + ArchivaTemporaryFolderRule.resumepackage(desc.getClassName()) + File.separator + desc.getMethodName());
        f1.mkdirs();
        Path p = Files.createDirectories(f1.toPath());
        d = p.toFile();
    }

    public File getRoot() {
        return d;
    }

    public void after() throws IOException {
        FileUtils.deleteDirectory(getRoot());
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
            sb.append(p[i].charAt(0)).append(File.separator);
        }
        sb.append(p[p.length - 1]);
        return sb.toString();
    }
    
}
