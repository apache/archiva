package org.apache.maven.repository.manager.web.action;

import com.opensymphony.xwork.Action;
import com.opensymphony.webwork.interceptor.ParameterAware;

import java.util.Map;
import java.util.HashMap;

import org.apache.maven.repository.manager.web.utils.ConfigurationManager;


/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.IndexConfigurationAction"
 */
public class IndexConfigurationAction
    implements Action, ParameterAware
{
    private Map parameters;

    /**
     * @plexus.requirement
     */
    private ConfigurationManager configManager;

    public Map getParameters() {
        return parameters;
    }

    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }

    /**
     * Method that is executed when the action is invoked.
     *
     * @return  a String that specifies where to go to next
     * @throws Exception
     */
    public String execute()
        throws Exception
    {
        String[] indexPath = (String[]) parameters.get( ConfigurationManager.INDEXPATH );
        Map map = new HashMap();

        if ( indexPath != null && indexPath.length == 1 && indexPath[0] != null )
        {
            String[] discoverSnapshots = (String[]) parameters.get( ConfigurationManager.DISCOVER_SNAPSHOTS );
            String[] minimalIndexPath = (String[]) parameters.get( ConfigurationManager.MIN_INDEXPATH );
            if( minimalIndexPath != null && minimalIndexPath.length == 1 && minimalIndexPath[0] != null )
            {
                map.put( ConfigurationManager.MIN_INDEXPATH, minimalIndexPath[0] );
            }

            map.put( ConfigurationManager.INDEXPATH, indexPath[0] );
            map.put( ConfigurationManager.DISCOVER_SNAPSHOTS, discoverSnapshots[0] );

            String[] blacklistPatterns = ( String[] ) parameters.get( ConfigurationManager.DISCOVERY_BLACKLIST_PATTERNS );
            if( blacklistPatterns != null && blacklistPatterns.length == 1 && blacklistPatterns[0] != null )
            {                 
                map.put( ConfigurationManager.DISCOVERY_BLACKLIST_PATTERNS, blacklistPatterns[0] );
            }

            configManager.updateConfiguration( map );

            return SUCCESS;
        }
        else
        {
            return ERROR;
        }
    }
}
