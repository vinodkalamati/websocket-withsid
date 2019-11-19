import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { analytics } from 'src/analytics';
import { catchError } from 'rxjs/operators';
import { searchQuery } from 'src/searchQuery';
import { throwError } from 'rxjs';
import { responses } from 'src/responses';
import { expertDTO } from 'src/expertDTO';

const httpOptions = {
  headers: new HttpHeaders({'Content-Type':'application/json'})
};

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private _url: string = "http://34.93.245.170:8099/api/v1/display";
  private _url2: string = "http://34.93.245.170:8080/queryservice/api/v1/response";
  constructor(private http: HttpClient) { }
  changeURL(url: string) {
    this._url = url;
  }
  getResponses(): Observable<analytics[]> {
    return this.http.get<analytics[]>(this._url).pipe(
      catchError((error: HttpErrorResponse) => {
        return throwError(error.message || "Server Error");
      })
    )
  }
  postQuery(query:searchQuery) {
    return this.http.post("http://34.93.245.170:8087//api/v1/analyticsquery",query,httpOptions);
  }
  postConcept(concept:expertDTO) {
    return this.http.post("http://34.93.245.170:8080/google-search/api/v1/domain",concept,httpOptions);
  }
  getQueries(): Observable<responses[]> {
    return this.http.get<responses[]>(this._url2).pipe(
      catchError((error: HttpErrorResponse) => {
        return throwError(error.message || "Server Error");
      })
    )
  }
}
