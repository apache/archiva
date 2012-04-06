package org.codehaus.plexus.redback.authorization;

/*
* Copyright 2011 The Codehaus.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface RedbackAuthorization
{

    /**
     * @return at least one of those redback roles is needed
     */
    String[] permissions() default ( "" );

    /**
     * @return the redback ressource karma needed
     */
    String resource() default ( "" );

    /**
     * @return doc
     */
    String description() default ( "" );

    /**
     * @return <code>true</code> if doesn't need any special permission
     */
    boolean noRestriction() default false;

    /**
     * @return if this service need only authentication and not special karma
     */
    boolean noPermission() default false;
}
