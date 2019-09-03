package org.apache.archiva.proxy.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.Test;

import static org.junit.Assert.*;

public class NetworkProxyTest {

    @Test
    public void getId() {
        NetworkProxy proxy = new NetworkProxy();
        assertNull(proxy.getId());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertEquals("test-proxy", proxy.getId());
    }

    @Test
    public void setId() {
        NetworkProxy proxy = new NetworkProxy();
        proxy.setId("test-proxy1");
        assertEquals("test-proxy1",proxy.getId());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        proxy.setId("test-proxy2");
        assertEquals("test-proxy2", proxy.getId());
    }

    @Test
    public void getProtocol() {
        NetworkProxy proxy = new NetworkProxy();
        assertEquals("http", proxy.getProtocol());
        proxy = new NetworkProxy("test-proxy", "https", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertEquals("https", proxy.getProtocol());

    }

    @Test
    public void setProtocol() {
        NetworkProxy proxy = new NetworkProxy();
        proxy.setProtocol("https");
        assertEquals("https", proxy.getProtocol());
        proxy = new NetworkProxy("test-proxy", "https", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        proxy.setProtocol("http");
        assertEquals("http", proxy.getProtocol());
    }

    @Test
    public void getHost() {
        NetworkProxy proxy = new NetworkProxy();
        assertNull(proxy.getHost());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertEquals("test.apache.org", proxy.getHost());

    }

    @Test
    public void setHost() {
        NetworkProxy proxy = new NetworkProxy();
        proxy.setHost("test1.apache.org");
        assertEquals("test1.apache.org",proxy.getHost());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        proxy.setHost("test2.apache.org");
        assertEquals("test2.apache.org", proxy.getHost());
        proxy.setHost("test3.apache.org");
        assertEquals("test3.apache.org", proxy.getHost());
    }

    @Test
    public void getPort() {
        NetworkProxy proxy = new NetworkProxy();
        assertEquals(8080,proxy.getPort());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertEquals(80, proxy.getPort());
    }

    @Test
    public void setPort() {
        NetworkProxy proxy = new NetworkProxy();
        proxy.setPort(8090);
        assertEquals(8090,proxy.getPort());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        proxy.setPort(9090);
        assertEquals(9090, proxy.getPort());
        proxy.setPort(9091);
        assertEquals(9091, proxy.getPort());
    }

    @Test
    public void getUsername() {
        NetworkProxy proxy = new NetworkProxy();
        assertNull(proxy.getUsername());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertEquals("testuser", proxy.getUsername());
    }

    @Test
    public void setUsername() {
        NetworkProxy proxy = new NetworkProxy();
        proxy.setUsername("testuser1");
        assertEquals("testuser1",proxy.getUsername());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        proxy.setUsername("testuser2");
        assertEquals("testuser2", proxy.getUsername());
        proxy.setUsername("testuser3");
        assertEquals("testuser3", proxy.getUsername());
    }

    @Test
    public void getPassword() {
        NetworkProxy proxy = new NetworkProxy();
        assertNull(proxy.getPassword());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertEquals("xxxx", new String(proxy.getPassword()));
        char[] testPwd = {'a', 'b', 'c', 'd'};
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", testPwd);
        assertEquals("abcd", new String(proxy.getPassword()));
        testPwd[0]='0';
        assertEquals("abcd", new String(proxy.getPassword()));
    }

    @Test
    public void setPassword() {
        NetworkProxy proxy = new NetworkProxy();
        assertNull(proxy.getPassword());
        proxy.setPassword("ucdx".toCharArray());
        assertEquals("ucdx", new String(proxy.getPassword()));
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertEquals("xxxx", new String(proxy.getPassword()));
        char[] testPwd = {'a', 'b', 'c', 'd'};
        proxy.setPassword(testPwd);
        assertEquals("abcd", new String(proxy.getPassword()));
        testPwd[0]='0';
        assertEquals("abcd", new String(proxy.getPassword()));
    }

    @Test
    public void isUseNtlm() {
        NetworkProxy proxy = new NetworkProxy();
        assertFalse(proxy.isUseNtlm());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertFalse(proxy.isUseNtlm());
    }

    @Test
    public void setUseNtlm() {
        NetworkProxy proxy = new NetworkProxy();
        assertFalse(proxy.isUseNtlm());
        proxy.setUseNtlm(true);
        assertTrue(proxy.isUseNtlm());
        proxy = new NetworkProxy("test-proxy", "http", "test.apache.org", 80, "testuser", "xxxx".toCharArray());
        assertFalse(proxy.isUseNtlm());
        proxy.setUseNtlm(true);
        assertTrue(proxy.isUseNtlm());
    }

}