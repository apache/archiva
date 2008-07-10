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

<%@ taglib uri="/webwork" prefix="ww" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<html>
<head>
  <title>Search Results</title>
  <ww:head/>
</head>

<body>

<h1>Search</h1>

<c:url var="imgNextPageUrl" value="/images/icon_next_page.gif"/>
<c:url var="imgPrevPageUrl" value="/images/icon_prev_page.gif"/>
<c:url var="imgPrevPageDisabledUrl" value="/images/icon_prev_page_disabled.gif"/>
<c:url var="imgNextPageDisabledUrl" value="/images/icon_next_page_disabled.gif"/>

<div id="contentArea">
  <div id="searchBox">
    <%@ include file="/WEB-INF/jsp/include/quickSearchForm.jspf" %>
  </div>

  <h1>Results</h1>

  <div id="resultsBox">
    <c:choose>

      <%-- search was made from the indices --%>
      <c:when test="${databaseResults == null}">
        <c:set var="hitsNum">${fn:length(results.hits) + (currentPage * results.limits.pageSize)}</c:set>
        <p>Hits: ${(hitsNum - results.limits.pageSize) + 1} to ${hitsNum} of ${results.totalHits}</p>
        
        <c:choose>
          <c:when test="${empty results.hits}">
            <p>No results</p>
          </c:when>
          <c:otherwise>
      	      	    
      	  <%-- Pagination start --%>
      	    <p>                       
            <%-- Prev & Next icons --%>
            <c:set var="prevPageUrl">
              <ww:url action="quickSearch" namespace="/">
                <ww:param name="q" value="%{'${q}'}"/>                
                <ww:param name="currentPage" value="%{'${currentPage - 1}'}"/>
              </ww:url>
      	    </c:set>
      	    <c:set var="nextPageUrl">
              <ww:url action="quickSearch" namespace="/">
                <ww:param name="q" value="%{'${q}'}"/>                
                <ww:param name="currentPage" value="%{'${currentPage + 1}'}"/>
              </ww:url>
      	    </c:set>    
            
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
                         
            <c:forEach var="i" begin="0" end="${totalPages - 1}">
			  <c:choose>			    			    
				<c:when test="${ (i != currentPage) }">
				   <c:set var="specificPageUrl">
		              <ww:url action="quickSearch" namespace="/">
		                <ww:param name="q" value="%{'${q}'}"/>
		                <ww:param name="currentPage" value="%{'${i}'}"/>
		              </ww:url>
		      	  </c:set>
				  <a href="${specificPageUrl}">${i + 1}</a>
				</c:when>
				<c:otherwise>		
					<b>${i + 1}</b>   
				</c:otherwise>
			  </c:choose> 
			</c:forEach>
			
			<c:choose>
			  <c:when test="${ currentPage eq ( totalPages - 1 ) }">
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
                    <my:showArtifactTitle groupId="${record.groupId}" artifactId="${record.artifactId}"
                                          version="${record.version}"/>
                  </h3>
                  <p>
                    <my:showArtifactLink groupId="${record.groupId}" artifactId="${record.artifactId}"
                                         version="${record.version}" versions="${record.versions}"/>
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
                <c:when test="${not empty (artifactModel.groupId)}">
                  <h3 class="artifact-title">
                    <my:showArtifactTitle groupId="${artifactModel.groupId}" artifactId="${artifactModel.artifactId}"
                                          version="${artifactModel.version}"/>
                  </h3>
                  <p>
                    <my:showArtifactLink groupId="${artifactModel.groupId}" artifactId="${artifactModel.artifactId}"
                                         version="${artifactModel.version}" versions="${artifactModel.versions}"/>
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
