<%@ taglib uri="/struts-tags" prefix="s"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
  <head>
    <title><s:text name="backupRestore.page.title"/></title>
  </head>
  <body>
    <div id="h3">
      <h3><s:text name="backupRestore.section.backup.title"/></h3>

      <c:if test="${!empty actionErrors}">
        <div class="errormessage">
          <s:iterator value="actionErrors">
            <p><s:text name="<s:property/>" /></p>
          </s:iterator>          
        </div>
      </c:if>

      <p>
        You can back up the application data for this installation to prevent data loss in the case of system failures. 
        The  application will be inaccessible while the backup takes place.
      </p>
      <p>
        A backup will be stored on the server in a dated subdirectory of the backup directory: 
        <code> 
          <s:property value="backupDirectory" /> 
        </code> 
      </p>

      <s:form action="backup" method="post" >
        <s:submit value="Create Backup" theme="simple"/>
      </s:form>  
    </div>
 
    <div id="h3">  
      <h3><s:text name="backupRestore.section.restore.title"/></h3>
      <p>
        You can reset the system to a previous state by using the
        restore function, or use it to import data from another version of this application.
      </p>
      <p>
        You can specify the directory where the backup files are located, or select from one of the recent backups in the configured
        backup directory.
      </p>

      <s:form action="restore" method="post" validate="true">
        <table>
          <s:textfield name="restoreDirectory" label="Backup directory"
            size="70" required="true" />
          <s:submit value="Restore Backup" theme="simple" />
        </table>         
      </s:form>
    </div>
    
    <div id="h3">
      <h4><s:text name="backupRestore.section.recent.backup"/></h4>

      <s:set name="previousBackups" value="previousBackups" /> 
      <c:choose>
        <c:when test="${empty(previousBackups)}">
          <div class="warningmessage">
            No previous backups found in the default backup directory.
          </div>
        </c:when>
        <c:otherwise>
          <table>
            <c:forEach var="backup" items="${previousBackups}">
              <tr>
                <td>
                  <fmt:formatDate value="${backup.date}" pattern="EEE MMM dd, yyyy 'at' HH:mm:ss" />
                </td>
                <td>
                  <c:if test="${backup.userDatabase}">
                    <c:set var="url">
                      <s:url action="restore">
                        <s:param name="restoreDirectory">${backup.directory}</s:param>
                        <s:param name="userDatabase" value="true" />
                      </s:url>
                    </c:set>
                    <a href="${url}">Restore Users</a>
                  </c:if>                                         
                  </td>
                </tr>
              </c:forEach>
            </table>
          </c:otherwise>
        </c:choose>
      </div>
  </body>
</s:i18n>
</html>

