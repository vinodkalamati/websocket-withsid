import { Component, OnInit } from '@angular/core';
import { AnalyticsService } from '../analytics-service/analytics.service';
import { Router } from '@angular/router';
import { analytics } from 'src/analytics';
import { searchQuery } from 'src/searchQuery';

@Component({
  selector: 'app-expert-analytics-movie',
  templateUrl: './expert-analytics-movie.component.html',
  styleUrls: ['./expert-analytics-movie.component.css']
})
export class ExpertAnalyticsMovieComponent implements OnInit {
  public responses =  [];
  public errorMsg;
  public displayedColumns: string[] = ['domain','query','result','posResponse','negResponse','actions'];
  constructor(private _analytics2: AnalyticsService,private router:Router) { }

  ngOnInit() {
    this._analytics2.changeURL("http://34.93.245.170:8099/api/v1/display/movie");
    this._analytics2.getResponses()
        .subscribe(data => this.responses=data,
                   error => this.errorMsg = error);
  }
  editQuery(term:analytics) {
    var query:searchQuery = {"domain":term.key.domain, "searchTerm":term.key.query,"sessionId":""};
    this._analytics2.postQuery(query)
        .subscribe(data => {
                              console.log(data);
                              this.router.navigate(['/expert-validate']);
                           },
                   error => console.log(error));
  }

}
