package org.apache.maven.archiva.cli;

import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainer;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

/**
 * @author Jason van Zyl
 */
public interface Cli
{
    int execute( String[] args,
                 ClassWorld classWorld );

    Options buildOptions( Options options );

    void processOptions( CommandLine cli,
                         PlexusContainer container )
        throws Exception;

    String getPomPropertiesPath();
}
