/* ========================================================================== *
 *         Copyright (C) 2004-2006, Pier Fumagalli <http://could.it/>         *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */
package it.could.util.encoding;

import java.io.UnsupportedEncodingException;

/**
 * <p>The {@link Encodable} interface describes an {@link Object} whose
 * {@link String} representation can vary depending on the encoding used.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public interface Encodable extends EncodingAware {
    
    /**
     * <p>Return the {@link String} representation of this instance.</p>
     * 
     * <p>This method is equivalent to a call to
     * {@link #toString(String) toString}({@link EncodingAware#DEFAULT_ENCODING
     * DEFAULT_ENCODING})</p>
     */
    public String toString();

    /**
     * <p>Return the {@link String} representation of this instance given
     * a specific character encoding.</p>
     * 
     * @throws UnsupportedEncodingException if the specified encoding is not
     *                                      supported by the platform.
     */
    public String toString(String encoding)
    throws UnsupportedEncodingException;

}
