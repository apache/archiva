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

<%@ taglib prefix="s" uri="/struts-tags" %>
<html>
<head>
  <title>Quick Search</title>
  <s:head/>
</head>

<s:if test="%{infoMessage != null}">
  <p>${infoMessage}</p>
</s:if>

<body>

<div id="contentArea">
<div id="searchBox">
  <s:form method="get" action="quickSearch" validate="true">
    <s:textfield label="Search for" size="50" name="q"/>
    <s:hidden name="completeQueryString" value="%{completeQueryString}"/>        
    <s:submit value="Search"/>
  </s:form>

  <script type="text/javascript">
    document.getElementById("quickSearch_q").focus();
  </script>

  <s:url id="filteredSearchUrl" action="advancedSearch"/>
  <s:a href="%{filteredSearchUrl}">
    Advanced Search >>
  </s:a>

  <p>
    <s:actionerror/>
  </p>
</div>
<div id="searchHint">
  <p>
    Enter your search terms. A variety of data will be searched for your keywords.<br/>
    To search for Java classes, packages or methods, use the keyword <code>bytecode:</code>
    before the term. For example: 
    <code>bytecode:MyClass</code>, or:
    <code>bytecode:myMethod</code>
  </p>
</div>
</div>
</body>
</html>
