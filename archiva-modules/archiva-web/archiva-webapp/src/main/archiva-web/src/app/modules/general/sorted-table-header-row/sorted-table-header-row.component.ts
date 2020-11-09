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

import {
  AfterViewChecked, AfterViewInit,
  Component,
  Input,
  OnInit,
  QueryList,
  TemplateRef,
  ViewChild,
  ViewChildren,
  ViewContainerRef,
  ContentChildren, AfterContentInit, AfterContentChecked, ChangeDetectorRef, Output, EventEmitter
} from '@angular/core';
import {FieldToggle} from "../../../model/field-toggle";
import {SortedTableHeaderComponent} from "../sorted-table-header/sorted-table-header.component";
import { delay, startWith } from 'rxjs/operators';

@Component({
  selector: 'tr[sorted]',
  templateUrl: './sorted-table-header-row.component.html',
  styleUrls: ['./sorted-table-header-row.component.scss']
})
export class SortedTableHeaderRowComponent implements OnInit, AfterViewInit, AfterContentInit, AfterContentChecked {

  @Input() sortFieldEmitter: EventEmitter<string[]>;
  @Input() sortOrderEmitter: EventEmitter<string>;
  @Input() toggleObserver: FieldToggle;

  sortFields: string[];
  sortOrder: string;

  @ContentChildren(SortedTableHeaderComponent, { descendants: true }) contentChilds: QueryList<SortedTableHeaderComponent>;

  constructor(private readonly viewContainer: ViewContainerRef) {
  }

  ngAfterContentChecked(): void {


    }

  ngAfterContentInit(): void {
    this.contentChilds.changes.pipe(startWith(this.contentChilds), delay(0)).subscribe(() => {
      this.contentChilds.forEach((colComponent, index) => {
        // console.log("Children " + colComponent);
        colComponent.registerSortFieldEmitter(this.sortFieldEmitter);
        colComponent.registerSortOrderEmitter(this.sortOrderEmitter);

        colComponent.sortOrder = this.sortOrder;
        colComponent.currentFieldArray = this.sortFields;
        colComponent.toggleObserver = this.toggleObserver;
      });
    });

  }

  ngOnInit(): void {
    this.registerSortOrderEmitter(this.sortOrderEmitter);
    this.registerSortFieldEmitter(this.sortFieldEmitter);
  }

  ngAfterViewInit(): void {

  }

  registerSortOrderEmitter(emitter : EventEmitter<string>) {
    emitter.subscribe((order) => {
      // console.log("header-row: Changing sort order: " + order);
      this.sortOrder = order
    });
  }

  registerSortFieldEmitter(emitter: EventEmitter<string[]>) {
    emitter.subscribe((fields)=>{
      // console.log("header-row: Changing sort fields" + fields);
      this.sortFields = fields;
    })
  }

}
