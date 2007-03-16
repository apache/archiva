package org.apache.maven.archiva.repository.scanner;

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

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.repository.ArchivaRepository;
import org.apache.maven.archiva.repository.consumer.Consumer;
import org.apache.maven.archiva.repository.consumer.ConsumerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ScanConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ScanConsumer implements Consumer
{
    private int processCount = 0;

    public List getExcludePatterns()
    {
        return Collections.EMPTY_LIST;
    }

    public List getIncludePatterns()
    {
        List includes = new ArrayList();
        includes.add( "**/*.jar" );
        return includes;
    }

    public String getName()
    {
        return "Scan Consumer";
    }

    public boolean init( ArchivaRepository repository )
    {
        return true;
    }

    public void processFile( BaseFile file ) throws ConsumerException
    {
        this.processCount++;
    }

    public void processFileProblem( BaseFile file, String message )
    {
        /* do nothing */
    }

    public int getProcessCount()
    {
        return processCount;
    }

    public void setProcessCount( int processCount )
    {
        this.processCount = processCount;
    }
}
