import {PaginationInfo} from "./pagination-info";

export class PagedResult<T> {
    pagination : PaginationInfo;
    data : Array<T>;
}
