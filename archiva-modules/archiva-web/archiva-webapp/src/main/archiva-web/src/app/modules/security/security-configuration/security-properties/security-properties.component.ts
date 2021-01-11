import {Component, OnInit} from '@angular/core';
import {SortedTableComponent} from "@app/modules/shared/sorted-table-component";
import {PropertyEntry} from '@app/model/property-entry';
import {TranslateService} from "@ngx-translate/core";
import {Observable} from "rxjs";
import {PagedResult} from "@app/model/paged-result";
import {SecurityService} from "@app/services/security.service";

@Component({
    selector: 'app-security-properties',
    templateUrl: './security-properties.component.html',
    styleUrls: ['./security-properties.component.scss']
})
export class SecurityPropertiesComponent extends SortedTableComponent<PropertyEntry> implements OnInit {

    constructor(translator: TranslateService, securityService: SecurityService) {
        super(translator, function (searchTerm: string, offset: number, limit: number, orderBy: string[], order: string): Observable<PagedResult<PropertyEntry>> {
            // console.log("Retrieving data " + searchTerm + "," + offset + "," + limit + "," + orderBy + "," + order);
            return securityService.queryProperties(searchTerm, offset, limit, orderBy, order);
        });
        super.sortField=['key']
    }

    ngOnInit(): void {
    }

}
