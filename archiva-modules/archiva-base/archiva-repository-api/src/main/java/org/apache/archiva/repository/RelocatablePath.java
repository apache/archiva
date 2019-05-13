package org.apache.archiva.repository;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class RelocatablePath
{

    private final String path;
    private final String originPath;
    private final boolean relocated;

    RelocatablePath(String path, String originPath) {
        this.path = path;
        this.originPath = originPath;
        this.relocated = !path.equals(originPath);
    }

    RelocatablePath(String path) {
        this.path = path;
        this.originPath = path;
        this.relocated = false;
    }

    public String getPath( )
    {
        return path;
    }

    public String getOriginPath( )
    {
        return originPath;
    }

    public boolean isRelocated( )
    {
        return relocated;
    }


}
