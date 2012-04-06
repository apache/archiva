package org.codehaus.redback.integration.taglib.jsp;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

/**
 * IfAuthorizedTag:
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 * @version $Id$
 */
public class ElseAuthorizedTag
    extends ConditionalTagSupport
{
    protected boolean condition()
        throws JspTagException
    {
        Boolean authzStatus = (Boolean) pageContext.getAttribute( "ifAuthorizedTag" );

        if ( authzStatus != null )
        {
            pageContext.removeAttribute( "ifAuthorizedTag" );

            return !authzStatus.booleanValue();
        }

        authzStatus = (Boolean) pageContext.getAttribute( "ifAnyAuthorizedTag" );

        if ( authzStatus != null )
        {
            pageContext.removeAttribute( "ifAnyAuthorizedTag" );

            return !authzStatus.booleanValue();
        }

        throw new JspTagException( "ElseAuthorizedTag should follow either IfAuthorized or IfAnyAuthorized" );
    }
}
