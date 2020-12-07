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

import { TestBed } from '@angular/core/testing';

import { AuthenticationService } from './authentication.service';
import {of, throwError} from 'rxjs';
import {ArchivaRequestService} from "./archiva-request.service";
import { ErrorMessage } from '../model/error-message';
import {HttpErrorResponse} from "@angular/common/http";

describe('AuthenticationService', () => {
  let service: AuthenticationService;
  let archivaRequestServiceSpy: jasmine.SpyObj<ArchivaRequestService>;

  beforeEach(() => {
    const spy = jasmine.createSpyObj('ArchivaRequestService', ['executeRestCall']);

    TestBed.configureTestingModule(    { providers: [
      AuthenticationService,
      { provide: ArchivaRequestService, useValue: spy }
    ]});
    service = TestBed.inject(AuthenticationService);
    archivaRequestServiceSpy = TestBed.inject(ArchivaRequestService) as jasmine.SpyObj<ArchivaRequestService>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('#login should return success and set token', () => {
    const stubValue = {'access_token':'abcdefg','refresh_token':'hijklmnop','expires_in':1000};
    archivaRequestServiceSpy.executeRestCall.and.returnValue(of(stubValue));
    // resultHandler: (n: string, err?: ErrorMessage[]) => void
    let result:string;
    let handler = (n:string, err?:ErrorMessage[]) => {
      result = n;
    };
    service.login('admin','pass123', handler);
    expect(result).toEqual("OK");
    expect(archivaRequestServiceSpy.executeRestCall.calls.count()).toBe(1, 'one call');
    expect(localStorage.getItem('access_token')).toEqual("abcdefg");
    expect(localStorage.getItem('refresh_token')).toEqual("hijklmnop");

  });

  it('#login fails', () => {
    const stubValue = {'access_token':'abcdefg','refresh_token':'hijklmnop','expires_in':1000};
    archivaRequestServiceSpy.executeRestCall.and.returnValue(throwError(new HttpErrorResponse({'status':404, error:{'errorMessages':[
        new ErrorMessage()
        ]}})));
    // resultHandler: (n: string, err?: ErrorMessage[]) => void
    let result : string;
    let messages: ErrorMessage[];
    let handler = (n:string, err?:ErrorMessage[]) => {
      result = n;
      messages = err;
    };
    service.login('admin', 'test', handler);
    expect(result).toEqual("ERROR");
    expect(messages).toBeTruthy();
    expect(messages.length).toEqual(1);
  });
});
