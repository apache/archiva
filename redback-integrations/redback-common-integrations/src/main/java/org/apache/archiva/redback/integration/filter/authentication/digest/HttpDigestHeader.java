package org.apache.archiva.redback.integration.filter.authentication.digest;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.commons.codec.binary.Base64;
import org.apache.archiva.redback.integration.HttpUtils;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * HttpDigestHeader
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@Service( "httpClientHeader" )
@Scope( "prototype" )
public class HttpDigestHeader
{
    private Logger log = LoggerFactory.getLogger( HttpDigestHeader.class );

    public String username;

    public String realm;

    public String nonce;

    public String uri;

    public String response;

    public String qop;

    public String nc;

    public String cnonce;

    public void parseClientHeader( String rawHeader, String expectedRealm, String digestKey )
        throws HttpAuthenticationException
    {
        Properties authHeaderProps = HttpUtils.complexHeaderToProperties( rawHeader, ",", "=" );

        username = authHeaderProps.getProperty( "username" );
        realm = authHeaderProps.getProperty( "realm" );
        nonce = authHeaderProps.getProperty( "nonce" );
        uri = authHeaderProps.getProperty( "uri" );
        response = authHeaderProps.getProperty( "response" );
        qop = authHeaderProps.getProperty( "qop" );
        nc = authHeaderProps.getProperty( "nc" );
        cnonce = authHeaderProps.getProperty( "cnonce" );

        // [RFC 2067] Validate all required values
        if ( StringUtils.isEmpty( username ) || StringUtils.isEmpty( realm ) || StringUtils.isEmpty( nonce )
            || StringUtils.isEmpty( uri ) || StringUtils.isEmpty( response ) )
        {
            log.debug( "Missing mandatory fields: Raw Digest Header : [{}]", rawHeader );

            throw new HttpAuthenticationException( "Missing mandatory digest fields per RFC2069." );
        }

        // [RFC 2617] Validate realm.
        if ( !StringUtils.equals( expectedRealm, realm ) )
        {
            log.debug( "Realm name is invalid: expected [{}] but got [{}]", expectedRealm, realm );

            throw new HttpAuthenticationException( "Response realm does not match expected realm." );
        }

        // [RFC 2617] Validate "auth" qop
        if ( StringUtils.equals( "auth", qop ) )
        {
            if ( StringUtils.isEmpty( nc ) || StringUtils.isEmpty( cnonce ) )
            {
                log.debug( "Missing mandatory qop fields: nc [{}] cnonce [{}]", nc, cnonce );

                throw new HttpAuthenticationException( "Missing mandatory qop digest fields per RFC2617." );
            }
        }

        // [RFC 2617] Validate nonce
        if ( !Base64.isArrayByteBase64( nonce.getBytes() ) )
        {
            log.debug( "Nonce is not encoded in Base64: nonce [{}]", nonce );

            throw new HttpAuthenticationException( "Response nonce is not encoded in Base64." );
        }

        // Decode nonce
        String decodedNonce = new String( Base64.decodeBase64( nonce.getBytes() ) );
        String nonceTokens[] = StringUtils.split( decodedNonce, ":" );

        // Validate nonce format
        if ( nonceTokens.length != 2 )
        {
            log.debug( "Nonce format expected [2] elements, but got [{}] instead.  Decoded nonce [{}]",
                       nonceTokens.length, decodedNonce );

            throw new HttpAuthenticationException(
                "Nonce format is invalid.  " + "Received an unexpected number of sub elements." );
        }

        // Extract nonce timestamp
        long nonceTimestamp = 0;

        try
        {
            nonceTimestamp = Long.parseLong( nonceTokens[0] );
        }
        catch ( NumberFormatException e )
        {
            throw new HttpAuthenticationException( "Unexpected nonce timestamp." );
        }

        // Extract nonce signature
        String expectedSignature = Digest.md5Hex( nonceTimestamp + ":" + digestKey );

        if ( !StringUtils.equals( expectedSignature, nonceTokens[1] ) )
        {
            log.error( "Nonce parameter has been compromised." );

            throw new HttpAuthenticationException( "Nonce parameter has been compromised." );
        }
    }
}
