package org.apache.maven.repository.proxy.configuration;

/**
 * Immutable.
 *
 * @author Ben Walding
 */
public class MavenProxyConfiguration
{
    private final String key;

    private final String host;

    private final int port;

    private final String username;

    private final String password;

    public MavenProxyConfiguration( String key, String host, int port, String username, String password )
    {
        this.key = key;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getHost()
    {
        return host;
    }

    public String getKey()
    {
        return key;
    }

    public String getPassword()
    {
        return password;
    }

    public int getPort()
    {
        return port;
    }

    public String getUsername()
    {
        return username;
    }

}
