import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SecurityPropertiesComponent } from './security-properties.component';

describe('SecurityPropertiesComponent', () => {
  let component: SecurityPropertiesComponent;
  let fixture: ComponentFixture<SecurityPropertiesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SecurityPropertiesComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SecurityPropertiesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
