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
/**
 * <p>This is the V2 REST API of Archiva. It uses JAX-RS annotations for defining the endpoints.
 * The API is documented with OpenApi annotations.</p>
 *
 * <h3>Some design principles of the API and classes:</h3>
 * <ul>
 *     <li>All services use V2 model classes. Internal models are always converted to V2 classes.</li>
 *     <li>Schema attributes use the snake case syntax (lower case with '_' as divider)</li>
 *     <li>Return code <code>200</code> and <code>201</code> (POST) is used for successful execution.</li>
 *     <li>Return code <code>403</code> is used, if the user has not the permission for the action.</li>
 *     <li>Return code <code>422</code> is used for input that has invalid data.</li>
 * </ul>
 *
 * <h4>Querying entity lists</h4>
 * <p>The main entities of a given path are retrieved on the base path.
 * Further sub entities or entries may be retrieved via subpaths.
 * A single entity is returned by the "{id}" path. Be careful with technical paths that are parallel to the
 * id path. Avoid naming conflicts with the id and technical paths.
 * Entity attributes may be retrieved by "{id}/{attribute}" path or if there are lists or collections by
 * "{id}/mycollection/{subentryid}"</p>
 *
 * <ul>
 *  <li><code>GET</code> method is used for retrieving entities on the base path ""</li>
 *  <li>The query for base entities should always return a paged result and be filterable and sortable</li>
 *  <li>Query parameters for filtering, ordering and limits should be optional and proper defaults must be set</li>
 *  <li>Return code <code>200</code> is used for successful retrieval</li>
 *  <li>This action is idempotent</li>
 * </ul>
 *
 * <h4>Querying single entities</h4>
 * <p>Single entities are retrieved on the path "{id}"</p>
 * <ul>
 *  <li><code>GET</code> method is used for retrieving a single entity. The id is always a path parameter.</li>
 *  <li>Return code <code>200</code> is used for successful retrieval</li>
 *  <li>Return code <code>404</code> is used if the entity with the given id does not exist</li>
 *  <li>This action is idempotent</li>
 * </ul>
 *
 * <h4>Creating entities</h4>
 * <p>The main entities are created on the base path "".</p>
 * <ul>
 *     <li><code>POST</code> is used for creating new entities</li>
 *     <li>The <code>POST</code> body must always have a complete definition of the entity.</li>
 *     <li>A unique <code>id</code> or <code>name</code> attribute is required for entities. If the id is generated during POST,
 *     it must be returned by response body.</li>
 *     <li>A successful <code>POST</code> request should always return the entity definition as it would be returned by the GET request.</li>
 *     <li>Return code <code>201</code> is used for successful creation of the new entity.</li>
 *     <li>A successful response has a <code>Location</code> header with the URL for retrieving the single created entity.</li>
 *     <li>Return code <code>303</code> is used, if the entity exists already</li>
 *     <li>This action is not idempotent</li>
 * </ul>
 *
 * <h4>Updating entities</h4>
 * <p>The path for entity update must contain the '{id}' of the entity. The path should be the same as for the GET operation.</p>
 * <ul>
 *     <li><code>PUT</code> is used for updating existing entities</li>
 *     <li>The body contains a JSON object. Only existing attributes are updated.</li>
 *     <li>A successful PUT request should return the complete entity definition as it would be returned by the GET request.</li>
 *     <li>Return code <code>200</code> is used for successful update of the new entity. Even if nothing changed.</li>
 *     <li>This action is idempotent</li>
 * </ul>
 *
 * <h4>Deleting entities</h4>
 * <p>The path for entity deletion must contain the '{id}' of the entity. The path should be the same as
 * for the GET operation.</p>
 * <ul>
 *     <li><code>DELETE</code> is used for deleting existing entities</li>
 *     <li>The successful operation has no request and no response body</li>
 *     <li>Return code <code>200</code> is used for successful deletion of the new entity.</li>
 *     <li>This action is not idempotent</li>
 * </ul>
 *
 * <h4>Errors</h4>
 * <ul>
 *     <li>A error uses a return code <code>>=400</code> </li>
 *     <li>All errors use the same result object ({@link org.apache.archiva.rest.api.v2.svc.ArchivaRestError}</li>
 *     <li>Error messages are returned as keys. Translation is part of the client application.</li>
 * </ul>
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
package org.apache.archiva.rest.api.v2.svc;