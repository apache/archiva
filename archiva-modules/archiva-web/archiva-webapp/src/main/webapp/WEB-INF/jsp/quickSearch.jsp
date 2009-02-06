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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
  <title>Quick Search</title>
  <s:head/>
  <script type="text/javascript" src="<c:url value='/js/jquery/jquery-1.2.6.pack.js'/>"></script>
  <script type="text/javascript">
    $(document).ready(function(){
    
    $("table.settings").hide();
    $("a.expand").click(function(event){
      event.preventDefault();
      $(this).next().toggle("slow");
    });
  });
  </script>
</head>

<s:if test="%{infoMessage != null}">
  <p>${infoMessage}</p>
</s:if>

<body>

<h1>Search</h1>

<div id="contentArea">
<div id="searchBox">
  <s:form method="get" action="quickSearch" validate="true">
    <s:textfield label="Search for" size="50" name="q"/>
    <s:hidden name="completeQueryString" value="%{completeQueryString}"/>        
    <s:submit value="Search"/>
  </s:form>

  <s:url id="filteredSearchUrl" action="advancedSearch"/>
  <s:a href="%{filteredSearchUrl}">
    Advanced Search >>
  </s:a>

  <p>
    <s:actionerror/>
  </p>
</div>
<div id="searchHint">
  
  Enter your search terms. A variety of data will be searched for your keywords. <a class="expand" href="#"><img src="<c:url value="/images/icon_info_sml.gif"/>" /></a>
  <table class="settings">
    <tr>
      <td>
        <b>*</b> To search for Java classes or packages, just type the class name or package name in the search box.<br/>  
      </td>
    </tr>
    <tr>
      <td>
        <b>*</b> To perform a boolean <code>NOT</code> search, use the keyword <code>NOT</code> after your search
         term, followed by the term you want to exclude. For example, to exclude artifacts with 
         a dependency on the artifact you are searching for from showing up in the search results:  
         <code>myQueryTerm NOT dependency</code> 
      </td>
    </tr>
  </table>
  
</div>
</div>
</body>
</html>