package org.apache.archiva;

import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.RepositorySessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @author Olivier Lamy
 */
public class TestRepositorySessionFactoryBean
    extends RepositorySessionFactoryBean
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private String beanId;

    public TestRepositorySessionFactoryBean( String beanId )
    {
        super( new Properties(  ) );
        this.beanId = beanId;
    }

    @Override
    public Class<RepositorySessionFactory> getObjectType()
    {
        return RepositorySessionFactory.class;
    }

    @Override
    protected RepositorySessionFactory createInstance()
        throws Exception
    {
        RepositorySessionFactory repositorySessionFactory =
            getBeanFactory().getBean( "repositorySessionFactory#" + this.beanId, RepositorySessionFactory.class );
        logger.info( "create RepositorySessionFactory instance of {}", repositorySessionFactory.getClass().getName() );
        return repositorySessionFactory;
    }
}
