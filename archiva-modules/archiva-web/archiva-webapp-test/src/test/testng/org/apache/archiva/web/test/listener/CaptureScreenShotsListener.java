package org.apache.archiva.web.test.listener;

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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.archiva.web.test.parent.AbstractSeleniumTest;
import org.apache.commons.io.FileUtils;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import com.thoughtworks.selenium.Selenium;

public class CaptureScreenShotsListener
    extends TestListenerAdapter
{
    @Override
    public void onTestFailure( ITestResult tr )
    {
        captureError( tr );
        System.out.println( "Test " + tr.getName() + " -> Failed" );
        super.onTestFailure( tr );
    }

    @Override
    public void onTestSuccess( ITestResult tr )
    {
        System.out.println( "Test " + tr.getName() + " -> Success" );
        super.onTestFailure( tr );
    }

    private void captureError( ITestResult tr )
    {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH_mm_ss" );
        String time = sdf.format( new Date() );
        File targetPath = new File( "target", "screenshots" );
        StackTraceElement stackTrace[] = tr.getThrowable().getStackTrace();
        String cName = tr.getTestClass().getName();
        int index = getStackTraceIndexOfCallingClass( cName, stackTrace );
        String methodName = stackTrace[index].getMethodName();
        int lNumber = stackTrace[index].getLineNumber();
        String lineNumber = Integer.toString( lNumber );
        String className = cName.substring( cName.lastIndexOf( '.' ) + 1 );
        targetPath.mkdirs();
        Selenium selenium = AbstractSeleniumTest.getSelenium();
        String fileBaseName = methodName + "_" + className + ".java_" + lineNumber + "-" + time;
        try
        {
            selenium.windowMaximize();
            File fileName = new File( targetPath, fileBaseName + ".png" );
            selenium.captureEntirePageScreenshot( fileName.getAbsolutePath(), "" );
        }
        catch ( RuntimeException e )
        {
            System.out.println( "Error when take screenshot for test " + tr.getName() + ": " + e.getMessage() );
            try
            {
                File fileName = new File( targetPath, fileBaseName + ".html" );
                FileUtils.writeStringToFile( fileName, selenium.getHtmlSource() );
            }
            catch ( IOException ioe )
            {
                System.out.println( ioe.getMessage() );
            }
        }
    }

    private int getStackTraceIndexOfCallingClass( String nameOfClass, StackTraceElement stackTrace[] )
    {
        boolean match = false;
        int i = 0;
        do
        {
            String className = stackTrace[i].getClassName();
            match = Pattern.matches( nameOfClass, className );
            i++;
        }
        while ( match == false );
        i--;
        return i;
    }
}
