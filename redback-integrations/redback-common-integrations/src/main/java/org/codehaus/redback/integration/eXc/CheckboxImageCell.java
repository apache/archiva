package org.codehaus.redback.integration.eXc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.extremecomponents.table.bean.Column;
import org.extremecomponents.table.cell.AbstractCell;
import org.extremecomponents.table.core.TableModel;
import org.extremecomponents.table.view.html.BuilderUtils;

/**
 * CheckboxImageCell
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CheckboxImageCell
    extends AbstractCell
{

    private static final String CHECKBOX_TRUE = "icon_success_sml";

    private static final String CHECKBOX_FALSE = "checkbox-false";

    protected String getCellValue( TableModel model, Column column )
    {
        Object value = column.getPropertyValue();
        if ( value == null )
        {
            return "";
        }

        Boolean bool = (Boolean) value;

        String cellValue = "<img src=\"";

        if ( bool.booleanValue() )
        {
            cellValue = cellValue + BuilderUtils.getImage( model, CHECKBOX_TRUE );
        }
        else
        {
            cellValue = cellValue + BuilderUtils.getImage( model, CHECKBOX_FALSE );
        }

        cellValue = cellValue + "\" alt=\"" + bool.toString() + "\"/>";

        return cellValue;
    }
}
