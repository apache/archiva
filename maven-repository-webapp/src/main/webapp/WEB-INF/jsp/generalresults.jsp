<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
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

<%@ taglib uri="webwork" prefix="ww" %>
<html>
<head>
  <title>Maven Repository Manager</title>
</head>

<body>

<h1>Maven Repository Manager</h1>

<%@ include file="form.jspf" %>

<table border="1px" cellspacing="0">
  <tr>
    <th>Group ID</th>
    <th>Artifact ID</th>
    <th>Version</th>
    <th>Hits</th>
  </tr>
  <ww:iterator value="searchResult">
    <tr>
      <td valign="top">
        <ww:property value="Artifact.getGroupId()"/>
      </td>
      <td valign="top">
        <ww:property value="Artifact.getArtifactId()"/>
      </td>
      <td valign="top">
        <ww:property value="Artifact.getVersion()"/>
      </td>
      <td valign="top">
        <table border="1px" width="100%" cellspacing="0">
          <ww:iterator value="FieldMatchesEntrySet">
            <tr>
              <td valign="top" width="15%" align="right"><ww:property value="Key"/></td>
              <td valign="top">
                <ww:iterator value="Value" id="test" status="">
                  <ww:property/>
                </ww:iterator>
                <br/>
              </td>
            </tr>
          </ww:iterator>
        </table>
      </td>
    </tr>
  </ww:iterator>
</table>

</body>
</html>
