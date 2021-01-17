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

import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {concat, merge, Observable, of, Subject} from "rxjs";
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    multicast,
    refCount,
    startWith,
    switchMap,
    tap
} from "rxjs/operators";
import {EntityService} from "@app/model/entity-service";
import {FieldToggle} from "@app/model/field-toggle";
import {PageQuery} from "../model/page-query";
import {LoadingValue} from '../model/loading-value';
import {PagedResult} from "@app/model/paged-result";
import {PaginationInfo} from "@app/model/pagination-info";


/**
 * This component has a search field and pagination section. Entering data in the search field, or
 * a button click on the pagination triggers a call to a service method, that returns the entity data.
 * The service must implement the {@link EntityService} interface.
 *
 * The content is displayed between the search input and the pagination section. To use the data, you should
 * add an identifier and refer to the item$ variable:
 * ```
 * <app-paginated-entities #parent>
 *   <table>
 *       <tr ngFor="let entity in parent.item$ | async" >
 *           <td>{{entity.id}}</td>
 *       </tr>
 *   </table>
 * </app-paginated-entities>
 * ```
 *
 * @typeparam T The type of the retrieved entity elements.
 */
@Component({
    selector: 'app-paginated-entities',
    templateUrl: './paginated-entities.component.html',
    styleUrls: ['./paginated-entities.component.scss']
})
export class PaginatedEntitiesComponent<T> implements OnInit, FieldToggle, AfterViewInit {

    @Input() id: string;

    /**
     * This must be set, if you use the component. This service retrieves the entity data.
     */
    @Input() service: EntityService<T>;

    /**
     * The number of elements per page retrieved
     */
    @Input() pageSize = 10;

    /**
     * Two-Way-Binding attribute for sorting field
     */
    @Input() sortField = [];
    /**
     * Two-Way Binding attribute for sort order
     */
    @Input() sortOrder = "asc";

    /**
     * Pagination controls
     */
    @Input() pagination = {
        maxSize: 5,
        rotate: true,
        boundaryLinks: true,
        ellipses: false
    }

    /**
     * If true, all controls are displayed, if the total count is 0
     */
    @Input()
    displayIfEmpty:boolean=true;
    /**
     * Sets the translation key, for the text to display, if displayIfEmpty=false and the total count is 0.
     */
    @Input()
    displayKeyIfEmpty:string='form.emptyContent';

    /**
     * If set to true, all controls are displayed, even if there is only one page to display.
     * Otherwise the controls are not displayed, if there is only a single page of results.
     */
    @Input()
    displayControlsIfSinglePage:boolean=true;



    /**
     * The current page that is selected
     */
    page = 1;
    /**
     * The current search term entered in the search field
     */
    searchTerm: string;

    /**
     * Event thrown, if the page value changes
     */
    @Output() pageChange: EventEmitter<number> = new EventEmitter<number>();
    /**
     * Event thrown, if the search term changes
     */
    @Output() searchTermChange: EventEmitter<string> = new EventEmitter<string>();

    @Output() sortFieldChange: EventEmitter<string[]> = new EventEmitter<string[]>();

    @Output() sortOrderChange: EventEmitter<string> = new EventEmitter<string>();

    /**
     * The total number of elements available for the given search term
     */
    public paginationInfo$: Observable<PaginationInfo>;
    /**
     * The entity items retrieved from the service
     */
    public items$: Observable<LoadingValue<PagedResult<T>>>;

    private pageStream: Subject<number> = new Subject<number>();
    private searchTermStream: Subject<string> = new Subject<string>();

    constructor() {
        // console.log("Construct " + this.id);
        this.items$=null;
        this.paginationInfo$=null;
    }

    ngOnInit(): void {
        console.log("Pag Init " + this.id);
        // We combine the sources for the page and the search input field to a observable 'source'
        const pageSource = this.pageStream.pipe(map(pageNumber => {
            return new PageQuery(this.searchTerm, pageNumber);
        }));
        const searchSource = this.searchTermStream.pipe(
            debounceTime(1000),
            distinctUntilChanged(),
            map(searchTerm => {
                this.searchTerm = searchTerm;
                return new PageQuery(searchTerm, 1)
            }));
        const source = merge(pageSource, searchSource).pipe(
            startWith(new PageQuery(this.searchTerm, this.page)),
            switchMap((params: PageQuery) =>
                concat(
                    of(LoadingValue.start<PagedResult<T>>()),
                    this.service(params.search, (params.page - 1) * this.pageSize, this.pageSize, this.sortField, this.sortOrder)
                        .pipe(map(pagedResult=>LoadingValue.finish<PagedResult<T>>(pagedResult)))
                )
            ),
            // This is to avoid multiple REST calls, without each subscriber would
            // cause a REST call.
            multicast(()=>new Subject<LoadingValue<PagedResult<T>>>()),
            refCount()
            );
        this.paginationInfo$ = source.pipe(filter(val => val.hasValue())
            , map(val => PaginationInfo.of(val.value.pagination.total_count, val.value.pagination.offset, val.value.pagination.limit)),
            tap((el) => this.page = el.page()));
        this.items$ = source;
    }

    search(terms: string) {
        // console.log("Keystroke " + terms);
        this.searchTermChange.emit(terms);
        this.searchTermStream.next(terms)
    }

    public changePage(pageNumber: number) {
        // console.log("Page change " +pageNumber);
        this.pageChange.emit(pageNumber);
        this.pageStream.next(pageNumber);
    }

    private compareArrays(a1: string[], a2: string[]) {
        let i = a1.length;
        while (i--) {
            if (a1[i] !== a2[i]) return false;
        }
        return true
    }

    toggleSortField(fieldName: string) {
        this.toggleField([fieldName]);
    }

    toggleField(fieldArray: string[]) {
        // console.log("Changing sort field " + fieldArray);
        let sortOrderChanged: boolean = false;
        let sortFieldChanged: boolean = false;
        if (!this.compareArrays(this.sortField, fieldArray)) {
          // console.log("Fields differ: " + this.sortField + " - " + fieldArray);
            this.sortField = fieldArray;
            if (this.sortOrder != 'asc') {
                this.sortOrder = 'asc';
                sortOrderChanged = true;
            }
            sortFieldChanged = true;
        } else {
            if (this.sortOrder == "asc") {
                this.sortOrder = "desc";
            } else {
                this.sortOrder = "asc";
            }
          // console.log("Toggled sort order: " + this.sortOrder);
            sortOrderChanged = true;
        }
        if (sortOrderChanged) {
          //console.log("Sort order changed: "+this.sortOrder)
            this.sortOrderChange.emit(this.sortOrder);
        }
        if (sortFieldChanged) {
            this.sortFieldChange.emit(this.sortField);
        }
        if (sortFieldChanged || sortOrderChanged) {
            this.page = 1;
            this.changePage(this.page);
        }
    }

    ngAfterViewInit(): void {
        // console.log("Pag afterViewInit " + this.id);
        // We emit the current value to push them to the containing reading components
        this.sortOrderChange.emit(this.sortOrder);
        this.sortFieldChange.emit(this.sortField);
    }


    public changeService(newService : EntityService<T>): void {
        this.service = newService;
        this.changePage(1);
    }

}
