import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManageUsersRolesComponent } from './manage-users-roles.component';

describe('ManageUsersRolesComponent', () => {
  let component: ManageUsersRolesComponent;
  let fixture: ComponentFixture<ManageUsersRolesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ManageUsersRolesComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ManageUsersRolesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
