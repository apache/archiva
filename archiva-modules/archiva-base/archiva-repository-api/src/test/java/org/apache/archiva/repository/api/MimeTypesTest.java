package org.apache.archiva.repository.api;

import junit.framework.TestCase;

public class MimeTypesTest extends TestCase
{
    public void testMimeTypes() throws Exception
    {
        // Test for some added types.
        assertEquals( "sha1", "text/plain", MimeTypes.getMimeType( "foo.sha1" ) );
        assertEquals( "md5", "text/plain", MimeTypes.getMimeType( "foo.md5" ) );
        assertEquals( "pgp", "application/pgp-encrypted", MimeTypes.getMimeType( "foo.pgp" ) );
        assertEquals( "jar", "application/java-archive", MimeTypes.getMimeType( "foo.jar" ) );
        assertEquals( "Default", "application/octet-stream", MimeTypes.getMimeType(".SomeUnknownExtension"));
    }
}
