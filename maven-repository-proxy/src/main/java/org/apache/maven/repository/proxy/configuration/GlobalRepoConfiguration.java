package org.apache.maven.repository.proxy.configuration;

/**
 * @author Ben Walding
 */
public class GlobalRepoConfiguration
    extends FileRepoConfiguration
{
    public GlobalRepoConfiguration( String basePath )
    {
        super( "global", "file:///" + basePath, "Global Repository", false, true, false, 0 );
    }

}