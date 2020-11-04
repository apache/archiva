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

import {Directive, ElementRef, Input, OnChanges, OnInit, Renderer2, SimpleChanges} from '@angular/core';

/**
 * This directive can be used to render a element based on permissions. Sets the 'd-none' class for the
 * element, if the permission returns 'false'.
 */
@Directive({
    selector: '[appViewPermission]'
})
export class ViewPermissionDirective implements OnInit, OnChanges {
    @Input('appViewPermission') permission: boolean;

    constructor(private renderer: Renderer2, private el: ElementRef) {

    }

    ngOnInit(): void {
        // console.log("Init appViewPermission " + this.permission + " " + typeof (this.permission));
        // this.togglePermission();
    }

    private togglePermission() {
        if (this.permission) {
            this.removeClass("d-none");
        } else {
            this.addClass("d-none");
        }
    }

    addClass(className: string) {
        // make sure you declare classname in your main style.css
        this.renderer.addClass(this.el.nativeElement, className);
    }

    removeClass(className: string) {
        this.renderer.removeClass(this.el.nativeElement, className);
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.permission != null &&
            (changes.permission.firstChange || changes.permission.currentValue != changes.permission.previousValue)) {
            // console.debug("Changed " + JSON.stringify(changes));
            this.togglePermission();
        }
    }

}
