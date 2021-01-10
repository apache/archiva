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

import {ComponentFixture, TestBed} from '@angular/core/testing';

import {LdapSecurityComponent} from './ldap-security.component';
import {FormBuilder} from "@angular/forms";
import {RouterTestingModule} from '@angular/router/testing';
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {HttpClientTestingModule} from "@angular/common/http/testing";
import {TranslateModule} from "@ngx-translate/core";

describe('LdapSecurityComponent', () => {
  let component: LdapSecurityComponent;
  let fixture: ComponentFixture<LdapSecurityComponent>;
  let router;
  let route;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LdapSecurityComponent ],
      providers: [FormBuilder],
      schemas:[NO_ERRORS_SCHEMA],
      imports:[
        TranslateModule.forRoot(),
        RouterTestingModule.withRoutes([]),
          HttpClientTestingModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LdapSecurityComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    router = TestBed.get(Router);
    route = TestBed.get(ActivatedRoute)
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Test Custom DN Validator', () => {
    it('valid 1', () => {
      const ctrl = component.userForm.controls['base_dn']
      ctrl.setValue('cn=abc');
      expect(ctrl.valid).toBeTruthy();
    })
    it('valid 2', () => {
      const ctrl = component.userForm.controls['base_dn']
      ctrl.setValue('cn=abc,dc=abc');
      expect(ctrl.valid).toBeTruthy()
    })
    it('valid with space', () => {
      const ctrl = component.userForm.controls['base_dn']
      ctrl.setValue('cn=abc , dc=abc');
      expect(ctrl.valid).toBeTruthy()
    })
    it('invalid postfix', () => {
      const ctrl = component.userForm.controls['base_dn']
      ctrl.setValue('cn=abc,');
      expect(ctrl.invalid).toBeTruthy()
    })
    it('invalid RDN', () => {
      const ctrl = component.userForm.controls['base_dn']
      ctrl.setValue('cn=abc,dc,dc=abc');
      expect(ctrl.invalid).toBeTruthy()
    })
  });
});
