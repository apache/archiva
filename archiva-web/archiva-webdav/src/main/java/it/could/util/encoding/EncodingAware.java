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

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

/**
 * <p>The {@link EncodingAware} interface describes an {@link Object} aware
 * of multiple encodings existing withing the platform.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public interface EncodingAware {

    /** <p>The default encoding is specified as being <code>UTF-8</code>.</p> */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /** <p>The platform encoding is evaluated at runtime from the JVM.</p> */
    public static final String PLATFORM_ENCODING =
            new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();

}
