import {Component, OnInit} from '@angular/core';
import {SortedTableComponent} from "@app/modules/shared/sorted-table-component";
import {PropertyEntry} from '@app/model/property-entry';
import {TranslateService} from "@ngx-translate/core";
import {Observable} from "rxjs";
import {PagedResult} from "@app/model/paged-result";
import {SecurityService} from "@app/services/security.service";
import {ToastService} from "@app/services/toast.service";
import {ErrorResult} from "@app/model/error-result";

@Component({
    selector: 'app-security-properties',
    templateUrl: './security-properties.component.html',
    styleUrls: ['./security-properties.component.scss']
})
export class SecurityPropertiesComponent extends SortedTableComponent<PropertyEntry> implements OnInit {

    editProperty:string='';
    propertyValue:string='';

    constructor(translator: TranslateService, private securityService: SecurityService, private toastService: ToastService) {
        super(translator, function (searchTerm: string, offset: number, limit: number, orderBy: string[], order: string): Observable<PagedResult<PropertyEntry>> {
            // console.log("Retrieving data " + searchTerm + "," + offset + "," + limit + "," + orderBy + "," + order);
            return securityService.queryProperties(searchTerm, offset, limit, orderBy, order);
        });
        super.sortField=['key']
    }

    ngOnInit(): void {
    }

    isEdit(key:string) : boolean {
        return this.editProperty == key;
    }

    updateProperty(key:string, value:string) {
        console.log("Updating "+key+"="+value)
        this.securityService.updateProperty(key, value).subscribe(
            ()=>{
                this.toastService.showSuccessByKey('security-properties', 'security.config.properties.edit_success')
            },
            (error: ErrorResult) => {
                this.toastService.showErrorByKey('security-properties', 'security.config.properties.edit_failure', {error:error.firstMessageString()})
            }
        );
    }

    toggleEditProperty(propertyEntry:PropertyEntry) : void {
        if (this.editProperty==propertyEntry.key) {
            propertyEntry.value=this.propertyValue
            this.editProperty='';
            this.updateProperty(propertyEntry.key, this.propertyValue);
            this.propertyValue = '';
        } else {
            this.editProperty = propertyEntry.key;
            this.propertyValue = propertyEntry.value;
        }
    }
}
