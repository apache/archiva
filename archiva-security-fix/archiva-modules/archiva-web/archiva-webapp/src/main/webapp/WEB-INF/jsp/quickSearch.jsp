<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>
<html>
<head>
  <title>Quick Search</title>
  <ww:head/>
</head>

<ww:if test="%{infoMessage != null}">
  <p>${infoMessage}</p>
</ww:if>

<body>

<h1>Search</h1>

<div id="contentArea">
<div id="searchBox">
  <ww:form method="get" action="quickSearch" validate="true">
    <ww:textfield label="Search for" size="50" name="q"/>
    <ww:hidden name="completeQueryString" value="${completeQueryString}"/>        
    <ww:submit label="Go!"/>
  </ww:form>

  <script type="text/javascript">
    document.getElementById("quickSearch_q").focus();
  </script>

  <ww:url id="filteredSearchUrl" action="advancedSearch"/>
  <ww:a href="%{filteredSearchUrl}">
    Advanced Search
  </ww:a>

  <p>
    <ww:actionerror/>
  </p>
</div>

  <p>
    Enter your search terms. A variety of data will be searched for your keywords.<br/>
    To search for Java classes, packages or methods, use the keyword <code>bytecode:</code>
    before the term. For example: 
    <code>bytecode:MyClass</code>, or:
    <code>bytecode:myMethod</code>
  </p>
</div>
</body>
</html>
