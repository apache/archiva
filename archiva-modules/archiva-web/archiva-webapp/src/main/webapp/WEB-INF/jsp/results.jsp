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

<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="archiva" tagdir="/WEB-INF/tags" %>

<html>
<head>
  <title>Search Results</title>
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
      table.appendChild( row );
    }
  </script>  
</head>

<body>

<c:url var="iconCreateUrl" value="/images/icons/create.png" />

<c:if test="${fromFilterSearch == true}">
  <h1>Advanced Search</h1>
</c:if>
<c:if test="${fromFilterSearch == false}">
  <h1>Search</h1>
</c:if> 

<c:url var="imgNextPageUrl" value="/images/icon_next_page.gif"/>
<c:url var="imgPrevPageUrl" value="/images/icon_prev_page.gif"/>
<c:url var="imgPrevPageDisabledUrl" value="/images/icon_prev_page_disabled.gif"/>
<c:url var="imgNextPageDisabledUrl" value="/images/icon_next_page_disabled.gif"/>

<div id="contentArea">
  <div id="searchBoxResults">

    <c:if test="${fromFilterSearch == true}">
      <table>
        <tr>
          <td>
            <b>*</b> To do a filtered or advanced search, select the criteria from the list below and click the <img src="${iconCreateUrl}"/> icon. Specify the term you want to be matched in the created text field.
          </td>
        </tr>
        <tr>
          <td>
          <s:form id="filteredSearch" method="get" action="filteredSearch" validate="true">
            <s:hidden name="fromFilterSearch" value="%{#attr.fromFilterSearch}" theme="simple"/>  
            <label><strong>Advanced Search Fields: </strong></label><s:select name="searchField" list="searchFields" theme="simple"/> 
            <s:a href="#" title="Add Search Field" onclick="addSearchField( document.filteredSearch.searchField.options[document.filteredSearch.searchField.selectedIndex].text, document.filteredSearch.searchField.value, 'dynamicFields' )" theme="simple">
              <img src="${iconCreateUrl}" />
            </s:a>
            <table id="dynamicTable">
              <tr>
                <td><label>Repository: </td>
                <td><s:select name="repositoryId" list="managedRepositoryList" theme="simple"/></td> 
              </tr>          
              <tr>
                <td/>
                <td/>  
              </tr>
            </table> 
            <s:submit value="Search" theme="simple"/>  
          </s:form>  
          </td>
        </tr>
      </table>
    </c:if>
    <c:if test="${fromFilterSearch == false}">
      <s:form method="get" action="quickSearch" validate="true">
        <s:textfield label="Search for" size="50" name="q"/>
        <s:checkbox label="Search within results" name="searchResultsOnly"/>        
        <s:hidden name="completeQueryString" value="%{#attr.completeQueryString}"/>        
        <s:submit label="Go!"/>
      </s:form> 
      <script type="text/javascript">
        document.getElementById("quickSearch_q").focus();
      </script>
    </c:if>

  <p>
    <s:actionerror/>
  </p>
  </div>

  <h1>Results</h1>

  <div id="resultsBox">
    <c:choose>

      <%-- search was made from the indices --%>
      <c:when test="${databaseResults == null}">
        <c:set var="hitsNum">${fn:length(results.hits) + (currentPage  * results.limits.pageSize)}</c:set>
        <c:choose>
          <c:when test="${results.totalHits > results.limits.pageSize}">
            <c:choose>
              <c:when test="${fn:length(results.hits) < rowCount}">
                <p>Hits: ${(rowCount * currentPage) + 1} to ${hitsNum} of ${results.totalHits}</p>
              </c:when>
              <c:otherwise>
                <p>Hits: ${(hitsNum - results.limits.pageSize) + 1} to ${hitsNum} of ${results.totalHits}</p>
              </c:otherwise>
            </c:choose>
          </c:when>
          <c:otherwise>
            <p>Hits: 1 to ${hitsNum} of ${results.totalHits}</p>
          </c:otherwise>        
        </c:choose>
        <c:choose>
          <c:when test="${empty results.hits}">
            <p>No results</p>
          </c:when>
          <c:otherwise>
      	      	    
      	  <%-- Pagination start --%>
      	    <p>                       
            <%-- Prev & Next icons --%>
              <c:if test="${fromFilterSearch == false}">
               <c:set var="prevPageUrl">
                 <s:url action="quickSearch" namespace="/">
                   <s:param name="q" value="%{#attr.q}"/>                
                   <s:param name="currentPage" value="%{#attr.currentPage - 1}"/>
                 </s:url>
       	      </c:set>
       	      <c:set var="nextPageUrl">
                 <s:url action="quickSearch" namespace="/">
                   <s:param name="q" value="%{#attr.q}"/>                
                   <s:param name="currentPage" value="%{#attr.currentPage + 1}"/>
                 </s:url>
       	      </c:set>    
              </c:if>

              <c:if test="${fromFilterSearch == true}">
               <c:set var="prevPageUrl">
                 <s:url action="filteredSearch" namespace="/">
                   <s:param name="rowCount" value="%{#attr.rowCount}"/>
                   <s:param name="groupId" value="%{#attr.groupId}"/>
                   <s:param name="artifactId" value="%{#attr.artifactId}"/>
                   <s:param name="version" value="%{#attr.version}"/>
                   <s:param name="className" value="%{#attr.className}"/>
                   <s:param name="repositoryId" value="%{#attr.repositoryId}"/>
                   <s:param name="filterSearch" value="%{#attr.filterSearch}"/>
                   <s:param name="fromResultsPage" value="true"/>
                   <s:param name="currentPage" value="%{#attr.currentPage - 1}"/>
                   <s:param name="searchResultsOnly" value="%{#attr.searchResultsOnly}"/>
                   <s:param name="completeQueryString" value="%{#attr.completeQueryString}"/>
                 </s:url>
       	      </c:set>
       	      <c:set var="nextPageUrl">
                <s:url action="filteredSearch" namespace="/">
                  <s:param name="rowCount" value="%{#attr.rowCount}"/>
                  <s:param name="groupId" value="%{#attr.groupId}"/>
                  <s:param name="artifactId" value="%{#attr.artifactId}"/>
                  <s:param name="version" value="%{#attr.version}"/>
                  <s:param name="className" value="%{#attr.className}"/>
                  <s:param name="repositoryId" value="%{#attr.repositoryId}"/>
                  <s:param name="filterSearch" value="%{#attr.filterSearch}"/>
  		          <s:param name="fromResultsPage" value="true"/>
                  <s:param name="currentPage" value="%{#attr.currentPage + 1}"/>
 		          <s:param name="searchResultsOnly" value="%{#attr.searchResultsOnly}"/>
		          <s:param name="completeQueryString" value="%{#attr.completeQueryString}"/>
                </s:url>
      	      </c:set>    
             </c:if>
            
            <c:choose>
              <c:when test="${currentPage == 0}">                               
	            <img src="${imgPrevPageDisabledUrl}"/>
	          </c:when>
	          <c:otherwise>
	            <a href="${prevPageUrl}">
	              <img src="${imgPrevPageUrl}"/>
	            </a>      
	          </c:otherwise>
            </c:choose>
			
			<%-- Google-style pagination --%>
			<c:choose>
			  <c:when test="${totalPages > 11}">
			    <c:choose>
			      <c:when test="${(currentPage - 5) < 0}">
			        <c:set var="beginVal">0</c:set>
			        <c:set var="endVal">10</c:set> 
			      </c:when>			        
			      <c:when test="${(currentPage + 5) > (totalPages - 1)}">
			        <c:set var="beginVal">${(totalPages -1) - 10}</c:set>
			        <c:set var="endVal">${totalPages - 1}</c:set>
			      </c:when>
			      <c:otherwise>
			        <c:set var="beginVal">${currentPage - 5}</c:set>
			        <c:set var="endVal">${currentPage + 5}</c:set>
			      </c:otherwise>
			    </c:choose>  
			  </c:when>
			  <c:otherwise>
			    <c:set var="beginVal">0</c:set>
			    <c:set var="endVal">${totalPages - 1}</c:set> 
			  </c:otherwise>
			</c:choose>
						
			<c:forEach var="i" begin="${beginVal}" end="${endVal}">
              <c:if test="${fromFilterSearch == false}">
                <c:choose>                   			    
		          <c:when test="${i != currentPage}">
		            <c:set var="specificPageUrl">
		              <s:url action="quickSearch" namespace="/">
		                <s:param name="q" value="%{#attr.q}"/>
		                <s:param name="currentPage" value="%{#attr.i}"/>
		                <s:param name="searchResultsOnly" value="%{#attr.searchResultsOnly}"/>
		                <s:param name="completeQueryString" value="%{#attr.completeQueryString}"/>
  		              </s:url>
		      	    </c:set>
			        <a href="${specificPageUrl}">${i + 1}</a>
			      </c:when>
			      <c:otherwise>		
		       	    <b>${i + 1}</b>   
		          </c:otherwise>				  			    
                </c:choose>
              </c:if>

              <c:if test="${fromFilterSearch == true}">
                <c:choose>                  			    
		          <c:when test="${i != currentPage}">
		            <c:set var="specificPageUrl">
		              <s:url action="filteredSearch" namespace="/">
                        <s:param name="rowCount" value="%{#attr.rowCount}"/>
                        <s:param name="groupId" value="%{#attr.groupId}"/>
                        <s:param name="artifactId" value="%{#attr.artifactId}"/>
                        <s:param name="version" value="%{#attr.version}"/>
                        <s:param name="className" value="%{#attr.className}"/>
                        <s:param name="repositoryId" value="%{#attr.repositoryId}"/>
                        <s:param name="filterSearch" value="%{#attr.filterSearch}"/>
		                <s:param name="fromResultsPage" value="true"/>
		                <s:param name="currentPage" value="%{#attr.i}"/>
		                <s:param name="searchResultsOnly" value="%{#attr.searchResultsOnly}"/>
		                <s:param name="completeQueryString" value="%{#attr.completeQueryString}"/>
		              </s:url>
		      	    </c:set>
				    <a href="${specificPageUrl}">${i + 1}</a>
			      </c:when>
			      <c:otherwise>		
		            <b>${i + 1}</b>   
			      </c:otherwise>
                </c:choose>
              </c:if>
			</c:forEach>
			
            <c:choose>
			  <c:when test="${currentPage == (totalPages - 1)}">
			    <img src="${imgNextPageDisabledUrl}"/>
              </c:when>
              <c:otherwise>
	            <a href="${nextPageUrl}">
	              <img src="${imgNextPageUrl}"/>
	            </a>
	          </c:otherwise>   
            </c:choose>
            </p>    
          <%-- Pagination end --%>
            
            <c:forEach items="${results.hits}" var="record" varStatus="i">
              <c:choose>
                <c:when test="${not empty (record.groupId)}">
                  <h3 class="artifact-title">
                    <archiva:showArtifactTitle groupId="${record.groupId}" artifactId="${record.artifactId}"/>
                  </h3>
                  <p>
                    <archiva:showArtifactLink groupId="${record.groupId}" artifactId="${record.artifactId}"
                                         versions="${record.versions}" repositoryId="${record.repositoryId}"/>
                  </p>
                </c:when>
                <c:otherwise>
                  <p>
                    <c:url var="hiturl" value="/repository/${record.url}" />
                    <a href="${hiturl}">${record.urlFilename}</a>
                  </p>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </c:otherwise>
        </c:choose>
      </c:when>

      <%-- search was made from the database (find artifact)--%>
      <c:otherwise>
        <p>Hits: ${fn:length(databaseResults)}</p>

        <c:choose>
          <c:when test="${empty databaseResults}">
            <p>No results</p>
          </c:when>
          <c:otherwise>
            <c:forEach items="${databaseResults}" var="artifactModel" varStatus="i">
              <c:choose>
                <c:when test="${not empty (artifactModel.namespace)}">
                  <h3 class="artifact-title">
                    <archiva:showArtifactTitle groupId="${artifactModel.namespace}"
                                               artifactId="${artifactModel.project}"
                                               version="${artifactModel.version}"/>

                  </h3>
                  <p>
                    <archiva:showArtifactLink  groupId="${artifactModel.namespace}"
                                               artifactId="${artifactModel.project}"
                                               version="${artifactModel.version}"/>

                  </p>
                </c:when>
                <c:otherwise>
                  <p>
                    <c:url var="hiturl" value="/repository/${artifactModel.repositoryId}" />
                    <a href="${hiturl}">${artifactModel.repositoryId}</a>
                  </p>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </c:otherwise>
        </c:choose>

      </c:otherwise>
    </c:choose>
  </div>
</div>
</body>
</html>