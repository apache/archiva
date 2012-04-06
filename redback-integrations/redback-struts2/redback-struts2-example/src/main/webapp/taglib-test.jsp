<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="/redback/taglib-1.0" prefix="redback" %>
<html>
    <head>
        <title>Redback Security Example Webapp</title>
    </head>
    <body>
      <p>
        jsp tag test page
      </p>
      <hr/>
      <p>
        test of the jsp tag pss:ifAuthorized 1:<br/>
        <br/>
        you should see an X right here -&gt;
        <redback:ifAuthorized permission="foo">
          X
        </redback:ifAuthorized>
      </p>
      <hr/>
      <p>
        test of the jsp tag redback:ifAuthorized 2:<br/>
        <br/>
        you should NOT see an X right here -&gt;
        <redback:ifAuthorized permission="bar">
          X
        </redback:ifAuthorized>
      </p>
      <hr/>
      <p>
        test of the jsp tag redback:ifAnyAuthorized 3:<br/>
        <br/>
        you should see an X right here -&gt;
        <redback:ifAnyAuthorized permissions="foo,bar">
          X
        </redback:ifAnyAuthorized>
      </p>
      <hr/>
      <p>
        test of the jsp tag redback:ifAnyAuthorized 4:<br/>
        <br/>
        you should NOT see an X right here -&gt;
        <redback:ifAnyAuthorized permissions="bar,dor">
          X
        </redback:ifAnyAuthorized>
      </p>
      <hr/>
    </body>
</html>
