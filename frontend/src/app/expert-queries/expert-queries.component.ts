import { Component, OnInit, Inject } from '@angular/core';
import { AnalyticsService } from '../analytics-service/analytics.service';
import {MatDialog, MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { expertDTO } from 'src/expertDTO';
import { FormControl, FormGroup } from '@angular/forms';

@Component({
  selector: 'app-expert-queries',
  templateUrl: './expert-queries.component.html',
  styleUrls: ['./expert-queries.component.css']
})
export class ExpertQueriesComponent implements OnInit {
  public responses =  [];
  public errorMsg;
  public userId: any;
  public displayedColumns: string[] = ['number','domain','query','actions'];
  constructor(private _analytics:AnalyticsService, private dialog:MatDialog) { }

  ngOnInit() {
    this.userId = localStorage.getItem('user');
    this._analytics.changeURL("http://34.93.245.170:8080/queryservice/api/v1/response");
    this._analytics.getQueries()
        .subscribe(data => this.responses=data,
                   error => this.errorMsg = error);
  }
  openDialog(): void {
    const dialogRef = this.dialog.open(ExpertQueriesDialog, {
      width: '290px',
      data: this.userId
    });
    dialogRef.afterClosed().subscribe(result => {
      console.log('The dialog was closed');
    });
  }
  logg() {
    console.log(this.errorMsg);
  }
}
@Component({
  selector: 'expert-queries-dialog',
  templateUrl: 'expert-queries-dialog.html',
  styleUrls: ['expert-queries-dialog.css']
})
export class ExpertQueriesDialog {
  conceptForm = new FormGroup({
    domain: new FormControl(''),
    concept: new FormControl(''),
  });
  constructor(public dialogRef: MatDialogRef<ExpertQueriesDialog>, @Inject(MAT_DIALOG_DATA) public data: any, private _analytics:AnalyticsService) {}

    onClickSubmit(expertdata:any){
      console.log(expertdata);
        var query: expertDTO = { "userId":"internal", "domain":expertdata.domain, "concept":expertdata.concept};
        console.log(JSON.stringify(query));
        this._analytics.postConcept(query)
                       .subscribe(Response => console.log(Response),
                                  error => console.log(error));
        this.onNoClick();
  }
  onNoClick(): void {
    this.dialogRef.close();
  }
}
