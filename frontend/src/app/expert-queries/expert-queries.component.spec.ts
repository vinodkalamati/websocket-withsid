import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ExpertQueriesComponent } from './expert-queries.component';

describe('ExpertQueriesComponent', () => {
  let component: ExpertQueriesComponent;
  let fixture: ComponentFixture<ExpertQueriesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ExpertQueriesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExpertQueriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
