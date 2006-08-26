package org.apache.maven.repository.reporting;

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

import org.apache.maven.artifact.Artifact;

/**
 * 
 */
public class ReportCondition
{
    public static final int SUCCESS = 0;

    public static final int FAILURE = -1;

    public static final int WARNING = 1;

    private int result;

    private Artifact artifact;

    private String reason;

    public ReportCondition( int result, Artifact artifact, String reason )
    {
        this.result = result;
        this.artifact = artifact;
        this.reason = reason;
    }

    public int getResult()
    {
        return result;
    }

    public void setResult( int result )
    {
        this.result = result;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason( String reason )
    {
        this.reason = reason;
    }
}
