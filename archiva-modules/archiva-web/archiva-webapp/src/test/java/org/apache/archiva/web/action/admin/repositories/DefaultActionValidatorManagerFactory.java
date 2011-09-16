package org.apache.archiva.web.action.admin.repositories;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.DefaultTextProvider;
import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.conversion.ObjectTypeDeterminer;
import com.opensymphony.xwork2.conversion.impl.DefaultObjectTypeDeterminer;
import com.opensymphony.xwork2.conversion.impl.XWorkBasicConverter;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.inject.ContainerBuilder;
import com.opensymphony.xwork2.inject.Scope;
import com.opensymphony.xwork2.ognl.OgnlReflectionProvider;
import com.opensymphony.xwork2.ognl.OgnlUtil;
import com.opensymphony.xwork2.ognl.OgnlValueStackFactory;
import com.opensymphony.xwork2.ognl.accessor.CompoundRootAccessor;
import com.opensymphony.xwork2.util.CompoundRoot;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionProvider;
import com.opensymphony.xwork2.validator.ActionValidatorManager;
import com.opensymphony.xwork2.validator.DefaultActionValidatorManager;
import com.opensymphony.xwork2.validator.DefaultValidatorFactory;
import com.opensymphony.xwork2.validator.DefaultValidatorFileParser;
import ognl.PropertyAccessor;

import java.util.HashMap;

/**
 * Factory for creating the DefaultActionValidatorManager to be used for the validation tests.
 */
public class DefaultActionValidatorManagerFactory
{
    
    // ObjectFactory.setObjectFactory(..) was removed in struts 2.1, so we have to workaround with this
    //  to make the validation tests work
    public ActionValidatorManager createDefaultActionValidatorManager()
        throws ClassNotFoundException
    {
        Container container = createBootstrapContainer();

        ActionContext context = new ActionContext( new HashMap<String, Object>() );
        context.setValueStack( createValueStack( container ) );
        ActionContext.setContext( context );

        OgnlReflectionProvider reflectionProvider = new OgnlReflectionProvider();

        reflectionProvider.setOgnlUtil( container.getInstance( OgnlUtil.class ) );

        ObjectFactory objectFactory = new ObjectFactory();
        objectFactory.setReflectionProvider( reflectionProvider );

        DefaultValidatorFileParser fileParser = new DefaultValidatorFileParser();
        fileParser.setObjectFactory( objectFactory );

        DefaultValidatorFactory validatorFactory = new DefaultValidatorFactory( objectFactory, fileParser );

        DefaultActionValidatorManager defaultValidatorManager = new DefaultActionValidatorManager();
        defaultValidatorManager.setValidatorFactory( validatorFactory );
        defaultValidatorManager.setValidatorFileParser( fileParser );

        return defaultValidatorManager;
    }

    private ValueStack createValueStack( Container container )
        throws ClassNotFoundException
    {
        OgnlValueStackFactory stackFactory = new OgnlValueStackFactory();

        stackFactory.setXWorkConverter( container.getInstance( XWorkConverter.class ) );
        stackFactory.setContainer( container );
        stackFactory.setTextProvider( container.getInstance( TextProvider.class ) );

        ValueStack stack = stackFactory.createValueStack();

        return stack;
    }

    private Container createBootstrapContainer()
    {
        ContainerBuilder builder = new ContainerBuilder();
        builder.factory( ObjectFactory.class, Scope.SINGLETON );
        builder.factory( ReflectionProvider.class, OgnlReflectionProvider.class, Scope.SINGLETON );
        builder.factory( ValueStackFactory.class, OgnlValueStackFactory.class, Scope.SINGLETON );
        builder.factory( XWorkConverter.class, Scope.SINGLETON );
        builder.factory( XWorkBasicConverter.class, Scope.SINGLETON );
        builder.factory( TextProvider.class, "system", DefaultTextProvider.class, Scope.SINGLETON );
        builder.factory( ObjectTypeDeterminer.class, DefaultObjectTypeDeterminer.class, Scope.SINGLETON );
        builder.factory( PropertyAccessor.class, CompoundRoot.class.getName(), CompoundRootAccessor.class, Scope.SINGLETON );
        builder.factory( OgnlUtil.class, Scope.SINGLETON );
        builder.constant( "devMode", "false" );

        return builder.create( true );
    }
}
