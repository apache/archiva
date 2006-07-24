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

<%@ taglib prefix="ww" uri="/webwork" %>

<html>
<head>
  <title>Administration</title>
  <ww:head />
</head>

<body>

<h1>Administration</h1>

<div id="contentArea">
  <h2>Configuration</h2>
  <table>
    <tr>
      <th>Repository Directory</th>
      <td>
        <ww:property value="repositoryDirectory" />
      </td>
      <td></td>
    </tr>
    <tr>
      <th>Indexing Schedule</th>
      <td>
        <ww:property value="indexerCronExpression" />
      </td>
      <%-- TODO: a "run now without timestamp checking" operation should be here too, to pick up any stragglers (in the event of a bug) --%>
      <%-- TODO: a "delete index and run now" operation should be here too (really clean, remove deletions that didn't get picked up) --%>
      <td><a href="<ww:url action="runIndexer" />">Run Now</a></td>
    </tr>
    <tr>
      <td><a href="<ww:url action="configure" />">Edit Configuration</a></td>
      <td></td>
      <td></td>
    </tr>
  </table>
</div>

</body>
</html>