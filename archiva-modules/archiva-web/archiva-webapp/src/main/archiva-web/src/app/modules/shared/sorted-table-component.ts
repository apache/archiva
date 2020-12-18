import {EntityService} from "@app/model/entity-service";
import {TranslateService} from "@ngx-translate/core";

export class SortedTableComponent<T> {

    sortField = ["id"];
    sortOrder = "asc";

    constructor(public translator : TranslateService, public service: EntityService<T>) {
    }
}
