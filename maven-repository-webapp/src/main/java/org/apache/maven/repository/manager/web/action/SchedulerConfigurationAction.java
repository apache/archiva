package org.apache.maven.repository.manager.web.action;

import com.opensymphony.xwork.Action;
import com.opensymphony.webwork.interceptor.ParameterAware;

import java.util.Map;
import java.util.HashMap;

import org.apache.maven.repository.manager.web.utils.ConfigurationManager;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.SchedulerConfigurationAction"
 */
public class SchedulerConfigurationAction
    implements Action, ParameterAware
{
    /**
     * @plexus.requirement
     */
    private ConfigurationManager plexusConfig;

    private Map parameters;

    public Map getParameters()
    {
        return parameters;
    }

    public void setParameters( Map parameters )
    {
        this.parameters = parameters;
    }

    /**
     * Execute this method if the action was invoked
     *
     * @return String success or error
     */
    public String execute()
    {
        Map map;
        try
        {
            map = new HashMap();

            String[] cronExpression = (String[]) parameters.get( ConfigurationManager.DISCOVERY_CRON_EXPRESSION );

            if( cronExpression[0] != null  )
            {
                map.put( ConfigurationManager.DISCOVERY_CRON_EXPRESSION, cronExpression[0] );

                plexusConfig.updateConfiguration( map );

                return SUCCESS;
            }
            else
            {
                return ERROR;
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
            return ERROR;
        }
    }
}
