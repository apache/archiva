package org.codehaus.redback.integration.mail;

/*
* Copyright 2005-2006 The Codehaus.
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

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.jdo.JdoFactory;
import org.codehaus.plexus.redback.keys.AuthenticationKey;
import org.codehaus.plexus.redback.keys.KeyManager;
import org.codehaus.plexus.redback.keys.KeyManagerException;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.jpox.SchemaTool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Test the Mailer class.
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class MailGeneratorTest
    extends TestCase
{
    @Inject
    @Named(value = "mailGenerator#velocity")
    private MailGenerator generator;

    @Inject
    @Named(value = "mailGenerator#custom-url")
    private MailGenerator customGenerator;

    @Inject
    @Named(value = "userSecurityPolicy")
    private UserSecurityPolicy policy;

    @Inject
    @Named(value = "keyManager#memory")
    private KeyManager keyManager;

    @Inject
    @Named(value = "jdoFactory#users")
    DefaultConfigurableJdoFactory jdoFactory;

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Before
    public void setUp()
        throws Exception
    {
        CacheManager.getInstance().clearAll();
        super.setUp();

        jdoFactory.setPassword( "" );

        jdoFactory.setProperty( "org.jpox.transactionIsolation", "READ_COMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.poid.transactionIsolation", "READ_COMMITTED" ); //$NON-NLS-1$ //$NON-NLS-2$

        jdoFactory.setProperty( "org.jpox.autoCreateSchema", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
        
        Properties properties = jdoFactory.getProperties();

        for ( Entry<Object, Object> entry : properties.entrySet() )
        {
            System.setProperty( (String) entry.getKey(), (String) entry.getValue() );
        }

        SchemaTool.createSchemaTables( new URL[] { getClass()
            .getResource( "/org/codehaus/plexus/redback/keys/jdo/package.jdo" ) }, new URL[] {}, null, false, null ); //$NON-NLS-1$

        log.info( "jdoFactory driverName {} " , jdoFactory.getDriverName() );

        PersistenceManagerFactory pmf = jdoFactory.getPersistenceManagerFactory();

        assertNotNull( pmf );

        PersistenceManager pm = pmf.getPersistenceManager();

        pm.close();        
        
    }

    @Test
    public void testGeneratePasswordResetMail()
        throws KeyManagerException
    {
        AuthenticationKey authkey = keyManager.createKey( "username", "Password Reset Request",
                                                          policy.getUserValidationSettings().getEmailValidationTimeout() );

        String content = generator.generateMail( "passwordResetEmail", authkey, "baseUrl" );

        assertNotNull( content );
        assertTrue( content.indexOf( '$' ) == -1 ); // make sure everything is properly populate
    }

    @Test
    public void testGenerateAccountValidationMail()
        throws KeyManagerException
    {
        AuthenticationKey authkey = keyManager.createKey( "username", "New User Email Validation",
                                                          policy.getUserValidationSettings().getEmailValidationTimeout() );

        String content = generator.generateMail( "newAccountValidationEmail", authkey, "baseUrl" );

        assertNotNull( content );
        assertTrue( content.indexOf( '$' ) == -1 ); // make sure everything is properly populate
    }

    @Test
    public void testGenerateAccountValidationMailCustomUrl()
        throws Exception
    {
        AuthenticationKey authkey = keyManager.createKey( "username", "New User Email Validation",
                                                          policy.getUserValidationSettings().getEmailValidationTimeout() );

        String content = customGenerator.generateMail( "newAccountValidationEmail", authkey, "baseUrl" );

        assertNotNull( content );
        assertTrue( content.indexOf( "baseUrl" ) == -1 ); // make sure everything is properly populate
        assertTrue( content.indexOf( "MY_APPLICATION_URL/security" ) > 0 ); // make sure everything is properly populate
    }

    @Test
    public void testGeneratePasswordResetMailCustomUrl()
        throws Exception
    {
        AuthenticationKey authkey = keyManager.createKey( "username", "Password Reset Request",
                                                          policy.getUserValidationSettings().getEmailValidationTimeout() );

        String content = customGenerator.generateMail( "passwordResetEmail", authkey, "baseUrl" );

        assertNotNull( content );
        
        log.info( "mail content " + content );
        
        assertTrue( content.indexOf( "baseUrl" ) == -1 ); // make sure everything is properly populate
        assertTrue( content.indexOf( "MY_APPLICATION_URL/security" ) > 0 ); // make sure everything is properly populate
    }
}
