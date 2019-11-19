import { Component, OnInit } from '@angular/core';
import { AnalyticsService } from '../analytics-service/analytics.service';
import { analytics } from 'src/analytics';
import { searchQuery } from 'src/searchQuery';
import { Router } from '@angular/router';


@Component({
  selector: 'app-expert-analytics',
  templateUrl: './expert-analytics.component.html',
  styleUrls: ['./expert-analytics.component.css']
})
export class ExpertAnalyticsComponent implements OnInit {

  public responses =  [];
  public errorMsg;
  public displayedColumns: string[] = ['domain','query','result','posResponse','negResponse','actions'];
  constructor(private _analytics: AnalyticsService,private router:Router) { }

  ngOnInit() {
    this._analytics.changeURL("http://34.93.245.170:8099/api/v1/display");
    this._analytics.getResponses()
        .subscribe(data => this.responses=data,
                   error => this.errorMsg = error);
  }
  editQuery(term:analytics) {
    this.router.navigate(['/expert-validate']);

    var query:searchQuery = {"domain":term.key.domain, "searchTerm":term.key.query,"sessionId":""};
    this._analytics.postQuery(query)
        .subscribe(data => {
                              console.log(data);
                           },
                   error => console.log(error));
  }
  logg() {
    console.log(this.errorMsg);
  }
}
