package org.apache.archiva.common;
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
public abstract class AbstractMapper<B,T,R> implements MultiModelMapper<B,T,R>
{
    @Override
    public <S2, T2, R2> boolean supports( Class<S2> baseType, Class<T2> destinationType, Class<R2> reverseSourceType )
    {
        return (
            baseType.isAssignableFrom( getBaseType( ) ) &&
                destinationType.isAssignableFrom( getDestinationType( ) ) &&
                reverseSourceType.isAssignableFrom( getReverseSourceType( ) )
        );
    }

    @Override
    public int hashCode( )
    {
        return getHash();
    }

    @Override
    public boolean equals( Object obj )
    {
        return super.hashCode( ) == obj.hashCode( );
    }
}
