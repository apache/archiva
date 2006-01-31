package org.apache.maven.repository.proxy.files;

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
