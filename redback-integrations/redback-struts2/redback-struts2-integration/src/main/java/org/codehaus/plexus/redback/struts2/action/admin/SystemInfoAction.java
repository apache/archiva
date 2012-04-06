package org.codehaus.plexus.redback.struts2.action.admin;

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

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.action.AbstractSecurityAction;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.role.RoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SystemInfoAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-sysinfo" )
@Scope( "prototype" )
public class SystemInfoAction
    extends AbstractSecurityAction
{
    // ------------------------------------------------------------------
    // Component Requirements
    // ------------------------------------------------------------------

    /**
     *
     */
    @Inject
    private SecuritySystem securitySystem;

    /**
     *  role-hint="commons-configuration"
     */
    @Inject
    @Named( value = "commons-configuration" )
    private Registry registry;

    /**
     *  role-hint="cached"
     */
    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager rbacManager;

    // Class.getClass() and some JPOX classes
    private static final List<String> ignoredReaders = Arrays.asList( "class", "copy" );

    private static final String NULL = "&lt;null&gt;";

    private static final char LN = Character.LINE_SEPARATOR;

    private static final String INDENT = "  ";

    private static final int MAXDEPTH = 10;

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private StringBuilder details;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String show()
    {
        details = new StringBuilder();

        details.append( "Configuration: " );
        dumpObject( details, registry, INDENT );
        details.append( registry.dump() );
        details.append( LN );

        details.append( LN ).append( "<hr/>" ).append( LN );
        details.append( "RBAC Manager: " );
        dumpObject( details, rbacManager, INDENT );

        details.append( LN ).append( "<hr/>" ).append( LN );
        details.append( "SecuritySystem: " );
        dumpObject( details, securitySystem, INDENT );

        return SUCCESS;
    }

    private void dumpObject( StringBuilder sb, Object obj, String indent )
    {
        dumpObjectSwitchboard( new ArrayList<Object>(), sb, obj, indent, 0 );
    }

    /**
     * The recursive object dumping switchboard.
     *
     * @param seenObjects objects already seen (to prevent cycles)
     * @param sb          the stringbuffer to populate
     * @param obj         the object to dump
     * @param indent      the current indent string.
     * @param depth       the depth in the tree.
     */
    private void dumpObjectSwitchboard( List<Object> seenObjects, StringBuilder sb, Object obj, String indent,
                                        int depth )
    {
        if ( obj == null )
        {
            sb.append( NULL ).append( LN );
            return;
        }

        if ( depth > MAXDEPTH )
        {
            sb.append( StringEscapeUtils.escapeHtml( "<MAX DEPTH>" ) );
            sb.append( LN );
            return;
        }

        depth++;

        String className = obj.getClass().getName();

        sb.append( '(' ).append( className ).append( ") " );

        if ( obj instanceof List )
        {
            dumpIterator( seenObjects, sb, ( (List<?>) obj ).iterator(), indent, depth );
        }
        else if ( obj instanceof Set )
        {
            dumpIterator( seenObjects, sb, ( (Set<?>) obj ).iterator(), indent, depth );
        }
        else if ( obj instanceof Map )
        {
            dumpIterator( seenObjects, sb, ( (Map<?, ?>) obj ).entrySet().iterator(), indent, depth );
        }
        else if ( obj instanceof Iterator )
        {
            dumpIterator( seenObjects, sb, (Iterator<?>) obj, indent, depth );
        }
        else
        {
            // Filter classes that start with java or javax
            if ( className.startsWith( "java." ) || className.startsWith( "javax." ) )
            {
                sb.append( StringEscapeUtils.escapeHtml( obj.toString() ) ).append( LN );
                return;
            }

            // prevent cycles
            if ( seenObjects.contains( obj ) )
            {
                // No need to dump.
                sb.append( StringEscapeUtils.escapeHtml( "<seen already preventing cycle in dump> " ) );
                sb.append( LN );
                return;
            }

            // Adding object to seen list (to prevent cycles)
            seenObjects.add( obj );

            dumpObjectReaders( seenObjects, sb, obj, indent, depth );
        }
        depth--;
    }

    @SuppressWarnings( "unchecked" )
    private void dumpObjectReaders( List<Object> seenObjects, StringBuilder sb, Object obj, String indent, int depth )
    {
        sb.append( obj.toString() ).append( LN );
        String name = null;

        try
        {
            Map<String, Object> readers = PropertyUtils.describe( obj );
            for ( Map.Entry<String, Object> readerEntry : readers.entrySet() )
            {
                name = (String) readerEntry.getKey();

                if ( ignoredReaders.contains( name ) )
                {
                    // skip this reader.
                    continue;
                }

                sb.append( indent );
                sb.append( name ).append( ':' );

                Object value = readerEntry.getValue();
                if ( value == null )
                {
                    sb.append( NULL ).append( LN );
                }
                else
                {
                    dumpObjectSwitchboard( seenObjects, sb, value, INDENT + indent, depth );
                }
            }
        }
        catch ( Throwable e )
        {
            sb.append( LN ).append( indent );
            sb.append( "Unable to read bean [" ).append( obj.getClass().getName() );
            if ( StringUtils.isNotBlank( name ) )
            {
                sb.append( ".get" ).append( StringUtils.capitalize( name ) ).append( "()" );
            }
            sb.append( "]: " ).append( '(' ).append( e.getClass().getName() ).append( ") " );
            sb.append( e.getMessage() ).append( LN );
        }
    }

    private void dumpIterator( List<Object> seenObjects, StringBuilder sb, Iterator<?> iterator, String indent,
                               int depth )
    {
        sb.append( LN );
        while ( iterator.hasNext() )
        {
            Object entry = iterator.next();
            sb.append( indent );
            dumpObjectSwitchboard( seenObjects, sb, entry, indent + " | ", depth );
        }
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public String getDetails()
    {
        return details.toString();
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.CONFIGURATION_EDIT_OPERATION, Resource.GLOBAL );
        return bundle;
    }
}
