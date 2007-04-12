package org.apache.maven.archiva.policies;

import org.codehaus.plexus.digest.ChecksumFile;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ChecksumPolicy
    extends AbstractLogEnabled
    implements PostDownloadPolicy
{
    /**
     * The FAIL policy indicates that if the checksum does not match the
     * downloaded file, then remove the downloaded artifact, and checksum
     * files, and fail the transfer to the client side.
     */
    public static final String FAIL = "fail";

    /**
     * The FIX policy indicates that if the checksum does not match the
     * downloaded file, then fix the checksum file locally, and return
     * to the client side the corrected checksum.
     */
    public static final String FIX = "fix";

    /**
     * The IGNORE policy indicates that the checksum is never tested
     * and even bad downloads and checksum files are left in place
     * on the local repository.
     */
    public static final String IGNORED = "ignored";

    /**
     * @plexus.requirement role-hint="sha1"
     */
    private Digester digestSha1;

    /**
     * @plexus.requirement role-hint="md5"
     */
    private Digester digestMd5;

    /**
     * @plexus.requirement
     */
    private ChecksumFile checksumFile;

    private Set validPolicyCodes = new HashSet();

    public ChecksumPolicy()
    {
        validPolicyCodes.add( FAIL );
        validPolicyCodes.add( FIX );
        validPolicyCodes.add( IGNORED );
    }

    public boolean applyPolicy( String policySetting, Properties request, File localFile )
    {
        if ( !validPolicyCodes.contains( policySetting ) )
        {
            // No valid code? false it is then.
            getLogger().error( "Unknown checksum policyCode [" + policySetting + "]" );
            return false;
        }

        if ( IGNORED.equals( policySetting ) )
        {
            // Ignore.
            return true;
        }

        if ( !localFile.exists() )
        {
            // Local File does not exist.
            getLogger().debug( "Local file " + localFile.getAbsolutePath() + " does not exist." );
            return false;
        }

        File sha1File = new File( localFile.getAbsolutePath() + ".sha1" );
        File md5File = new File( localFile.getAbsolutePath() + ".md5" );

        if ( FAIL.equals( policySetting ) )
        {
            if ( !sha1File.exists() && !md5File.exists() )
            {
                getLogger().error( "File " + localFile.getAbsolutePath() + " has no checksum files (sha1 or md5)." );
                localFile.delete();
                return false;
            }

            // Test for sha1 first, then md5

            if ( sha1File.exists() )
            {
                try
                {
                    return checksumFile.isValidChecksum( sha1File );
                }
                catch ( FileNotFoundException e )
                {
                    getLogger().warn( "Unable to find sha1 file: " + sha1File.getAbsolutePath(), e );
                    return false;
                }
                catch ( DigesterException e )
                {
                    getLogger().warn( "Unable to process sha1 file: " + sha1File.getAbsolutePath(), e );
                    return false;
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Unable to process sha1 file: " + sha1File.getAbsolutePath(), e );
                    return false;
                }
            }

            if ( md5File.exists() )
            {
                try
                {
                    return checksumFile.isValidChecksum( md5File );
                }
                catch ( FileNotFoundException e )
                {
                    getLogger().warn( "Unable to find md5 file: " + md5File.getAbsolutePath(), e );
                    return false;
                }
                catch ( DigesterException e )
                {
                    getLogger().warn( "Unable to process md5 file: " + md5File.getAbsolutePath(), e );
                    return false;
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Unable to process md5 file: " + md5File.getAbsolutePath(), e );
                    return false;
                }
            }
        }

        if ( FIX.equals( policySetting ) )
        {
            if ( !sha1File.exists() )
            {
                try
                {
                    checksumFile.createChecksum( localFile, digestSha1 );
                }
                catch ( DigesterException e )
                {
                    getLogger().warn( "Unable to create sha1 file: " + e.getMessage(), e );
                    return false;
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Unable to create sha1 file: " + e.getMessage(), e );
                    return false;
                }
            }
            else
            {
                try
                {
                    checksumFile.isValidChecksum( sha1File );
                }
                catch ( FileNotFoundException e )
                {
                    getLogger().warn( "Unable to find sha1 file: " + sha1File.getAbsolutePath(), e );
                    return false;
                }
                catch ( DigesterException e )
                {
                    getLogger().warn( "Unable to process sha1 file: " + sha1File.getAbsolutePath(), e );
                    return false;
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Unable to process sha1 file: " + sha1File.getAbsolutePath(), e );
                    return false;
                }
            }

            if ( !md5File.exists() )
            {
                try
                {
                    checksumFile.createChecksum( localFile, digestMd5 );
                }
                catch ( DigesterException e )
                {
                    getLogger().warn( "Unable to create md5 file: " + e.getMessage(), e );
                    return false;
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Unable to create md5 file: " + e.getMessage(), e );
                    return false;
                }
            }
            else
            {
                try
                {
                    return checksumFile.isValidChecksum( md5File );
                }
                catch ( FileNotFoundException e )
                {
                    getLogger().warn( "Unable to find md5 file: " + md5File.getAbsolutePath(), e );
                    return false;
                }
                catch ( DigesterException e )
                {
                    getLogger().warn( "Unable to process md5 file: " + md5File.getAbsolutePath(), e );
                    return false;
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Unable to process md5 file: " + md5File.getAbsolutePath(), e );
                    return false;
                }
            }
        }

        getLogger().error( "Unhandled policyCode [" + policySetting + "]" );
        return false;
    }

    public String getDefaultPolicySetting()
    {
        return FIX;
    }

}
