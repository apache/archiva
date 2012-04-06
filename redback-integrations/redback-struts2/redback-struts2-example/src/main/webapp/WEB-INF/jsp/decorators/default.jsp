<%--
  ~ Copyright 2005-2006 The Codehaus.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="redback" uri="/redback/taglib-1.0" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html>
<head>
  <title>Redback Struts2 Example Webapp ::
    <decorator:title default="Redback Struts2 Example Webapp"/>
  </title>

  <style type="text/css" media="all">
    @IMPORT url("/css/main.css");
    @IMPORT url("/css/redback/table.css");
  </style>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
</head>

<body onload="<decorator:getProperty property="body.onload" />" class="composite">

<div id="banner">
  <s:url id="main" action="main" namespace="/" includeParams="none"/>
  <h1><s:a href="%{main}">Redback Struts2 Example Webapp</s:a></h1>
  <div class="clear">
  </div>
</div>

<div id="breadcrumbs">
  <div class="xright">
    <a href="http://www.codehaus.org/">Codehaus</a> |
    <a href="http://redback.codehaus.org/">Redback</a>
  </div>

  <div class="xleft">
    <c:import url="/WEB-INF/jsp/redback/include/securityLinks.jsp"/>
  </div>
  
  <div class="clear">
  </div>
</div>

  <p class="note">
    Guest access is :
    <redback:ifAuthorized permission="guest-access">
      <b>Enabled</b>
    </redback:ifAuthorized>
    <redback:elseAuthorized>
      <b>Disabled</b>
    </redback:elseAuthorized>
  </p>

  <p class="note">
    <c:if test="${sessionScope.securitySession.user != null}">
    	Encoded Password: <c:out value="${sessionScope.securitySession.user.encodedPassword}"/><br/>
    	Password Change Required Password: <c:out value="${sessionScope.securitySession.user.passwordChangeRequired}"/>
    </c:if>
  </p>


  <p class="note">The gray content is arriving via the /WEB-INF/jsp/decorators/default.jsp managed by sitemesh.<br/>
  Everything within the white box below is the actual jsp content.</p>
  <div id="nestedContent">
    <decorator:body/>
  </div>

  <div id="xworkinfo">
  
    <%--
    <strong>application scope:</strong>
    <ul>
    <c:choose>
      <c:when test="${!empty applicationScope}">
        <c:forEach var="ss" items="${applicationScope}">
          <li>
            <em><c:out value="${ss.key}" /></em> :
              <c:choose>
                <c:when test="${ss != null}">
                  (<c:out value="${ss.value.class.name}" /> ) <br />
                  &nbsp; &nbsp; &nbsp; <c:out value="${ss.value}" />
                </c:when>
                <c:otherwise>
                  &lt;null&gt;
                </c:otherwise>
              </c:choose>
          </li>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <li>[ empty ]</li>
      </c:otherwise>
    </c:choose>
    </ul>
      --%>
  
    <strong>session scope:</strong>
    <ul>
    <c:forEach var="ss" items="${sessionScope}">
      <li>
        <em><c:out value="${ss.key}" /></em> : 
          <c:choose>
            <c:when test="${ss.value != null}">
              (<c:out value="${ss.value.class.name}" /> ) <br />
              &nbsp; &nbsp; &nbsp; <c:out value="${ss.value}" />
            </c:when>
            <c:otherwise>
              &lt;null&gt;
            </c:otherwise>
          </c:choose>
      </li>
    </c:forEach>
    </ul>
    
    <strong>request scope:</strong>
    <ul>
    <c:forEach var="rs" items="${requestScope}">
      <li>
        <em><c:out value="${rs.key}" /></em> : 
          <c:choose>
            <c:when test="${rs.value != null}">
              (<c:out value="${rs.value.class.name}" /> ) <br />
              &nbsp; &nbsp; &nbsp; <c:out value="${rs.value}" />
            </c:when>
            <c:otherwise>
              &lt;null&gt;
            </c:otherwise>
          </c:choose>
      </li>
    </c:forEach>
    </ul>
    
    <strong>page scope:</strong>
    <ul>
    <c:forEach var="ps" items="${requestScope}">
      <li>
        <em><c:out value="${ps.key}" /></em> : 
          <c:choose>
            <c:when test="${ps.value != null}">
              (<c:out value="${ps.value.class.name}" /> ) <br />
              &nbsp; &nbsp; &nbsp; <c:out value="${ps.value}" />
            </c:when>
            <c:otherwise>
              &lt;null&gt;
            </c:otherwise>
          </c:choose>
      </li>
    </c:forEach>
    </ul>
     
  </div>

<div class="clear">
</div>

<div id="footer">
  <div class="xright">&#169; 2006 Codehaus.org </div>
  <div class="clear">
  </div>
</div>
</body>
</html>
