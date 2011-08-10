package org.apache.maven.archiva.web.interceptor;

import com.opensymphony.xwork2.interceptor.ParametersInterceptor;

import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: olamy
 * Date: 10/08/11
 * Time: 16:55
 * To change this template use File | Settings | File Templates.
 */
public class ArchivaParametersInterceptor extends ParametersInterceptor
{

    private String acceptedParamNames = "[a-zA-Z0-9\\-\\.\\]\\[\\(\\)_'\\s]+";
    private Pattern acceptedPattern = Pattern.compile(acceptedParamNames);

    @Override
    protected boolean acceptableName( String name )
    {
        boolean accept = super.acceptableName( name );
        if (!accept)
        {
            // [MRM-1487] second try adding '-' in pattern
            accept = acceptedPattern.matcher( name ).matches();
        }
        return accept;
    }
}
