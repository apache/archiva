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

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LdapSecurityComponent, dnValidator } from './ldap-security.component';
import {ValidatorFn} from "@angular/forms";

describe('LdapSecurityComponent', () => {
  let component: LdapSecurityComponent;
  let fixture: ComponentFixture<LdapSecurityComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LdapSecurityComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LdapSecurityComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Test Custom DN Validator', () => {
    it('check valid 1', () => {
      const ctrl = component.userForm.controls['base_dn']
      ctrl.setValue('cn=abc');
      expect(ctrl.valid).toBeTruthy();
    })
    it('check invalid 1', () => {
      const ctrl = component.userForm.controls['base_dn']
      ctrl.setValue('cn=abc,');
      expect(ctrl.invalid).toBeTruthy()
    })
  });
});
