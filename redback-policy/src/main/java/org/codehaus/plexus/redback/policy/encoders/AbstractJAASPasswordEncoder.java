package org.codehaus.plexus.redback.policy.encoders;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.redback.policy.PasswordEncoder;
import org.codehaus.plexus.redback.policy.PasswordEncodingException;
import org.codehaus.plexus.redback.users.Messages;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Abstract Password Encoder that uses the {@link MessageDigest} from JAAS.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AbstractJAASPasswordEncoder
    implements PasswordEncoder
{
    private String algorithm;

    private Object systemSalt;

    public AbstractJAASPasswordEncoder( String algorithm )
    {
        this.algorithm = algorithm;
    }

    public void setSystemSalt( Object salt )
    {
        this.systemSalt = salt;
    }

    public String encodePassword( String rawPass, Object salt )
    {
        if ( rawPass == null )
        {
            throw new IllegalArgumentException( "rawPass parameter cannot be null." );
        }

        MessageDigest md = null;
        try
        {
            md = MessageDigest.getInstance( this.algorithm );
            String precode = rawPass;

            // Only checking for null, not using StringUtils.isNotEmpty() as
            // whitespace can make up a valid salt. 
            if ( salt != null )
            {
                // Conforming to acegi password encoding standards for compatibility
                precode += "{" + salt + "}";
            }
            md.update( precode.getBytes( "UTF-8" ) ); //$NON-NLS-1$

            byte raw[] = md.digest();
            Base64 base64 = new Base64( 0, new byte[0] );
            return ( base64.encodeToString( raw ) );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new PasswordEncodingException(
                Messages.getString( "password.encoder.no.such.algoritm", this.algorithm ), e ); //$NON-NLS-1$
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new PasswordEncodingException( Messages.getString( "password.encoder.unsupported.encoding" ),
                                                 e ); //$NON-NLS-1$
        }
    }

    public boolean isPasswordValid( String encPass, String rawPass, Object salt )
    {
        if ( StringUtils.isEmpty( encPass ) )
        {
            // TODO: Throw exception?
            return false;
        }

        // PLXREDBACK-36 Commented out because a user with an empty password can't login due to the checking.
        // Empty password checking can also be achieve by turning on MustHavePasswordRule.
        //if ( StringUtils.isEmpty( rawPass ) )
        //{
        //    TODO: Throw exception?
        //    return false;
        //}

        String testPass = encodePassword( rawPass, salt );
        return ( encPass.equals( testPass ) );
    }

    public String encodePassword( String rawPass )
    {
        return encodePassword( rawPass, this.systemSalt );
    }

    public boolean isPasswordValid( String encPass, String rawPass )
    {
        return isPasswordValid( encPass, rawPass, this.systemSalt );
    }

}
