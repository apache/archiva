package org.apache.maven.repository.proxy.files;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

/**
 * @author Edwin Punzalan
 */
public class Checksum
{
    private String algorithm;

    public Checksum( String algorithm )
    {
        this.setAlgorithm( algorithm );
    }

    public String getFileExtension()
    {
        if ( "MD5".equals( algorithm ) )
        {
            return "md5";
        }
        else
        {
            return "sha1";
        }
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public void setAlgorithm( String algorithm )
    {
        this.algorithm = algorithm;
    }
}
