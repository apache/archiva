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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {PropertyMap} from "@app/model/property-map";

export class LdapConfiguration {
    host_name : string = "";
    port : number = 389;
    ssl_enabled : boolean  = false;
    context_factory: string = "";
    base_dn : string = "";
    groups_base_dn : string = "";
    bind_dn : string = "";
    bind_password : string = "";
    authentication_method : string = "";
    bind_authenticator_enabled : boolean = true;
    use_role_name_as_group : boolean = false;
    properties : PropertyMap;
    writable : boolean = false;
    available_context_factories : string[];


    constructor(initObj:any=null) {
        if (initObj) {
            this.host_name = initObj.host_name
            this.port = initObj.port
            this.ssl_enabled = initObj.ssl_enabled
            this.context_factory = initObj.context_factory
            this.base_dn = initObj.base_dn
            this.groups_base_dn = initObj.groups_base_dn
            this.bind_dn = initObj.bind_dn
            this.bind_password = initObj.bind_password
            this.authentication_method = initObj.authentication_method
            this.bind_authenticator_enabled = initObj.bind_authenticator_enabled
            this.use_role_name_as_group = initObj.use_role_name_as_group
            this.properties = new PropertyMap(Object.entries(initObj.properties))
            this.writable = initObj.writable
            this.available_context_factories = initObj.available_context_factories
        }
    }

}
