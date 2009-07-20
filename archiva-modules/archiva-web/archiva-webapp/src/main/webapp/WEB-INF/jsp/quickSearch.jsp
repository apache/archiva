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
  <script type="text/javascript">  
    function addSearchField(fieldText, field, divName)
    {     
      var element = document.getElementById( field );
      if( element != null )
      {
        alert( "Cannot add field! Field has already been added." );
        return 0;
      }

      var table = document.getElementById( "dynamicTable" );
      var row = document.createElement( "TR" );
      var label = document.createElement("TD");
      label.innerHTML = fieldText + ": ";	
     
      var textfield = document.createElement( "TD" );
      var inp1 =  document.createElement( "INPUT" );
      inp1.setAttribute( "type", "text" );
      inp1.setAttribute( "size", "30" );
      inp1.setAttribute( "id", field );
      inp1.setAttribute( "name", field );
      textfield.appendChild( inp1 );

      row.appendChild( label ); 
      row.appendChild( textfield );
      table.tBodies[0].appendChild( row );

    }
  </script>  

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
  
  <%-- advanced search --%>
  <script type="text/javascript">
    $(document).ready(function(){
    
    $("table.settings-search").hide();
    $("a.expand-search").click(function(event){
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

  <c:url var="iconCreateUrl" value="/images/icons/create.png" />
  
  <s:form method="get" id="quickSearch" action="quickSearch" validate="true">    
    <s:textfield label="Search for" size="50" name="q"/> 
    <s:hidden name="completeQueryString" value="%{completeQueryString}"/>  
    <s:submit value="Search"/>      	
  </s:form>  
  <p>
    <s:actionerror/>
  </p>
  <a class="expand-search" href="#"><strong>Advanced Search >></strong></a>
  <table class="settings-search">
    <tr>
      <td>
        <b>*</b> To do a filtered or advanced search, select the criteria from the list below and click the <img src="${iconCreateUrl}"/> icon. Specify the term you want to be matched in the created text field.
      </td>
    </tr>
    <tr>
      <td>    
        <s:form id="filteredSearch" method="get" action="filteredSearch" validate="true">  
          <label><strong>Advanced Search Fields: </strong></label><s:select name="searchField" list="searchFields" theme="simple"/> 
          <s:a href="#" title="Add Search Field" onclick="addSearchField( document.filteredSearch.searchField.options[document.filteredSearch.searchField.selectedIndex].text, document.filteredSearch.searchField.value, 'dynamicFields' )" theme="simple">
            <img src="${iconCreateUrl}" />
          </s:a>
          <table id="dynamicTable">
            <tbody>    
              <tr>
                <td><label>Repository: </td>
                <td><s:select name="repositoryId" list="managedRepositoryList" theme="simple"/></td> 
              </tr>       
              <tr>
                <td/>
                <td/>  
              </tr>
            </tbody>
          </table> 
          <s:submit value="Search" theme="simple"/>  
        </s:form>  
      </td>
    </tr>    
  </table>
</div>

<div id="searchHint">  
  Enter your search terms. A variety of data will be searched for your keywords. <a class="expand" href="#"><img src="<c:url value="/images/icon_info_sml.gif"/>" /></a>
  
  <table class="settings">
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