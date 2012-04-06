package org.codehaus.plexus.redback.struts2.action;

import java.util.Map;

import org.apache.struts2.interceptor.SessionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionSupport;

/**
 *
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public abstract class RedbackActionSupport
    extends ActionSupport
    implements SessionAware
{
    protected Logger log = LoggerFactory.getLogger( this.getClass() );
    
    protected Map<String,Object> session;

    public void setSession( Map<String, Object > map )
    {
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.session = map;
    }
}
