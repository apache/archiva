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

import {Directive, ElementRef, HostListener, Renderer2} from '@angular/core';

/**
 * Used to mark a div element as navigation subgroup. If a navigation element is clicked,
 * it searches the siblings of the div element for activated children and deactivates them.
 * This is a workaround for a bootstrap issue, when nav-items are collected in div elements.
 */
@Directive({
  selector: '[appNavSubgroup]'
})
export class NavSubgroupDirective {

  constructor(private renderer : Renderer2, private el : ElementRef) { }

  @HostListener('click')
  onClick() {
    let actionEl = this.el.nativeElement;
    let divElements = actionEl.parentElement.querySelectorAll("div[class~='nav']");
    if (divElements != null) {
      for (let divEl of divElements) {
        if (divEl != actionEl) {
          let actionElements = divEl.querySelectorAll("a[class~='active']");
          if (actionElements != null) {
            for (let activeEl of actionElements) {
              this.renderer.removeClass(activeEl, "active");
            }
          }
        }
      }
    }
  }


}
