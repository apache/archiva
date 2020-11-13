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
  Component,
  OnInit,
  Input,
  ViewContainerRef,
  ViewChild,
  TemplateRef,
  ChangeDetectorRef,
  AfterViewChecked, EventEmitter, Output
} from '@angular/core';
import {FieldToggle} from "../../../model/field-toggle";
import { ChangeDetectionStrategy } from '@angular/core';

@Component({
  host: { style: 'display:none'  },
  selector: 'app-th-sorted',
  templateUrl: './sorted-table-header.component.html',
  styleUrls: ['./sorted-table-header.component.scss']
})
export class SortedTableHeaderComponent implements OnInit, AfterViewChecked {

  @Input() fieldArray: string[];
  currentFieldArray: string[];
  sortOrder: string;
  toggleObserver: FieldToggle;
  @Input() contentText:string;

  @ViewChild('content', { static: true }) content: TemplateRef<{}>;


  constructor(private readonly viewContainer: ViewContainerRef) { }

  ngOnInit(): void {
    this.viewContainer.createEmbeddedView(this.content);
  }
  ngAfterViewChecked() {
  }

  toggleSortField() {
    // console.log("Toggling sort field " + this.fieldArray);
    this.toggleObserver.toggleField(this.fieldArray);
  }

  private compareArrays(a1: string[], a2: string[]) {
    if (a1==null || a2==null) {
      return false;
    }
    let i = a1.length;
    while (i--) {
      if (a1[i] !== a2[i]) return false;
    }
    return true
  }

  sortCheck() {
    return this.compareArrays(this.fieldArray, this.currentFieldArray);
  }

  isAscending() :boolean {
    // console.log("header: Is ascending: " + this.sortOrder);
    return this.sortOrder == 'asc';
  }

  registerSortOrderEmitter(emitter : EventEmitter<string>) {
    emitter.subscribe((field) => this.sortOrder = field);
  }

  registerSortFieldEmitter(emitter : EventEmitter<string[]>) {
    emitter.subscribe((field) => {
      // console.log("header: Change sort field "+field)
      this.currentFieldArray = field
    });
  }

}
