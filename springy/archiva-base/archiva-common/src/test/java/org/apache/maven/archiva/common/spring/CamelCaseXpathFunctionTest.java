package org.apache.maven.archiva.common.spring;

import java.util.Arrays;

import javax.xml.xpath.XPathFunction;

import junit.framework.TestCase;

/**
 * @author ndeloof
 *
 */
public class CamelCaseXpathFunctionTest
    extends TestCase
{

    private XPathFunction function = new CamelCaseXpathFunction();

    /**
     * Test method for {@link org.apache.maven.archiva.common.spring.CamelCaseXpathFunction#toCamelCase(java.lang.String)}.
     */
    public void testToCamelCase()
    throws Exception
    {
        assertEquals( "aCamelCaseProperty", function.evaluate( Arrays.asList( new String[] { "a-camel-case-property" } ) ) );
    }

}
