<p>
<ww:if test="hasActionErrors()">
  <b style="color: red;" >Errors:</b>
  <ww:iterator value="actionErrors">
    <li style="color: red;"><ww:property/></li>
  </ww:iterator>
</ww:if>
</p>