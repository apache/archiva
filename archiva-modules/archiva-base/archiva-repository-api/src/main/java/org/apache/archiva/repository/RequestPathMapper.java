package org.apache.archiva.repository;

/**
 *
 * Maps request paths to native repository paths. Normally HTTP requests and the path in the repository
 * storage should be identically.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface RequestPathMapper
{
    /**
     * Maps a request path to a repository path. The request path should be relative
     * to the repository. The resulting path should always start with a '/'.
     * The returned object contains additional information, if this request
     *
     * @param requestPath
     * @return
     */
    RelocatablePath relocatableRequestToRepository(String requestPath);


    String requestToRepository(String requestPath);


    /**
     * Maps a repository path to a request path. The repository path is relative to the
     * repository. The resulting path should always start with a '/'.
     *
     * @param repositoryPath
     * @return
     */
    String repositoryToRequest(String repositoryPath);

}
