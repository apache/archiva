import { Injectable } from '@angular/core';
import {ArchivaRequestService} from "@app/services/archiva-request.service";
import {RoleTemplate} from "@app/model/role-template";
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RoleService {

  constructor(private rest: ArchivaRequestService) { }

  public getTemplates() : Observable<RoleTemplate[]> {
    return this.rest.executeRestCall("get", "redback", "roles/templates", null);
  }

}
