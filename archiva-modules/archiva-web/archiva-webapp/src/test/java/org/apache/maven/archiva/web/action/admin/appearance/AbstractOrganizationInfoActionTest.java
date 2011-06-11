package org.apache.maven.archiva.web.action.admin.appearance;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.web.action.AbstractWebworkTestCase;
import org.easymock.MockControl;

/**
 */
public abstract class AbstractOrganizationInfoActionTest
    extends AbstractWebworkTestCase
{
    protected MockControl archivaConfigurationControl;

    protected ArchivaConfiguration configuration;

    protected AbstractAppearanceAction action;

    protected Configuration config;

    protected abstract AbstractAppearanceAction getAction();

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        config = new Configuration();
        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        configuration = (ArchivaConfiguration) archivaConfigurationControl.getMock();

        configuration.getConfiguration();
        archivaConfigurationControl.setReturnValue( config, 1, 2 );

        configuration.save( config );
        archivaConfigurationControl.setVoidCallable( 1, 2 );

        archivaConfigurationControl.replay();
    }

    protected void reloadAction()
    {
        action = getAction();
        action.setConfiguration( configuration );
    }
}
