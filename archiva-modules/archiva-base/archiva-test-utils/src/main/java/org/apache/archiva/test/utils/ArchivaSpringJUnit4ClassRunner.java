package org.apache.archiva.test.utils;

/*
 * Copyright 2012 The Apache Software Foundation.
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

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Eric
 */
public class ArchivaSpringJUnit4ClassRunner
    extends SpringJUnit4ClassRunner
{

    static {

        if (System.getProperty("archiva.user.configFileName")!=null && !"".equals(System.getProperty("archiva.user.configFileName").trim())) {
            try {
                Path file = Files.createTempFile("archiva-test-conf", ".xml");
                System.setProperty("archiva.user.configFileName", file.toAbsolutePath().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ArchivaSpringJUnit4ClassRunner( Class<?> clazz )
        throws InitializationError
    {
        super( clazz );
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods()
    {
        return ListGenerator.getShuffleList( super.computeTestMethods() );
    }


}
