import { Component, OnInit } from '@angular/core';
import { AnalyticsService } from '../analytics-service/analytics.service';
import { Router } from '@angular/router';
import { analytics } from 'src/analytics';
import { searchQuery } from 'src/searchQuery';

@Component({
  selector: 'app-expert-analytics-medical',
  templateUrl: './expert-analytics-medical.component.html',
  styleUrls: ['./expert-analytics-medical.component.css']
})
export class ExpertAnalyticsMedicalComponent implements OnInit {
  public responses =  [];
  public errorMsg;
  public displayedColumns: string[] = ['domain','query','result','posResponse','negResponse','actions'];
  constructor(private _analytics1: AnalyticsService,private router:Router) { }

  ngOnInit() {
    this._analytics1.changeURL("http://13.127.108.14:8099/api/v1/display/medical");
    this._analytics1.getResponses()
        .subscribe(data => this.responses=data,
                   error => this.errorMsg = error);
  }
  editQuery(term:analytics) {
    var query:searchQuery = {"domain":term.key.domain, "searchTerm":term.key.query,"sessionId":""};
    this._analytics1.postQuery(query)
        .subscribe(data => {
                              console.log(data);
                              this.router.navigate(['/expert-validate']);
                           },
                   error => console.log(error));
  }

}
