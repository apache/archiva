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

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Admin: Merge Staging Repository</title>
  <s:head/>
</head>

<body>

<h1>Admin: Merge Staging Repository</h1>

<p>
  Are you sure you want to merge the repository?
</p>

<div class="infobox">
  <table class="infotable">

    <c:choose>
      <c:when test="${empty (conflictSourceArtifacts)}">
        <h1>No conflicting artifacts</h1>
        <s:form method="post" action="merge" namespace="/admin" validate="false" theme="simple">
          <s:hidden name="repoid"/>
          <div class="buttons">
            <s:submit value="MergeAll" method="doMerge"/>
          </div>
        </s:form>
      </c:when>
      <c:otherwise>
        <div class="warningbox">
          <p>
            <strong>WARNING: The following are the artifacts in conflict.</strong>
          </p>
        </div>
        <c:forEach items="${conflictSourceArtifacts}" var="artifact">
          <tr>
            <td>Artifact Id :</td>
            <td><code>${artifact.id}</code></td>
          </tr>
        </c:forEach>
        <tr>
          <td>
            <s:form action="merge" method="post" namespace="/admin" validate="false">
              <s:hidden name="repoid"/>
              <div class="buttons">
                <table>
                  <tr>
                    <td>
                      <table>
                        <tr>
                          <td>
                            <s:submit value="Merge All" method="doMerge"/>
                          </td>
                        </tr>
                      </table>
                    </td>
                    <td>
                      <table>
                        <tr>
                          <td>
                            <s:submit value="Merge With Skip" method="mergeBySkippingConflicts"/>
                          </td>
                        </tr>
                      </table>

                    </td>
                  </tr>
                </table>
              </div>
            </s:form>
          </td>
        </tr>
      </c:otherwise>
    </c:choose>
  </table>
</div>
</body>
</html>
