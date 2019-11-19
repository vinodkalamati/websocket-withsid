import { Component, OnInit, Input } from '@angular/core';
import {UserResponseService} from '../user-response-service/user-response.service';
import { MedicalSearchService } from '../medical_search_service/medical-search.service';
import { Router } from '@angular/router';
import { WebSocketService } from '../websocket-service/websocket.service';
import Speech from 'speak-tts' ;


@Component({
  selector: 'app-search-result',
  templateUrl: './search-result.component.html',
  styleUrls: ['./search-result.component.css']
})
export class SearchResultComponent implements OnInit {

  constructor(private userResponseService:UserResponseService, private medicalSearchService:MedicalSearchService, private route:Router, private webSocketService: WebSocketService ) { }
  resultString: string;
  result:string[];
  query: string;
  flag:boolean = true;
  suggestionString:string;
  suggestion:string[];

  likeFlag:boolean;
  notifications: any;

  analyticsString:string;
  userFlag:string;
  reportFlag:boolean =true;
  domain:string;
  status:string;
  resultFlag=true;
  ngOnInit() {
    const speech = new Speech()
    this.likeFlag=true;
    this.resultString = localStorage.getItem('result');
    this.result = this.resultString.split(":");
    this.query = localStorage.getItem('query');
    this.status = localStorage.getItem('status');
    if(this.status == 'noresult'){
      this.resultFlag= false;
      this.query = localStorage.getItem('query');
      this.result.shift();
      this.result[0]= "no such data exists"
      speech.speak({
        text: 'Sorry!! No Such Data Exist',
        }).then(() => {
            console.log("Success !")
        }).catch(e => {
            console.error("An error occurred :", e)
        })
    }else{
      if(this.result.length < 2){
        this.query = "No Result Found";
        this.flag =false
        speech.speak({
          text: 'Sorry!! We did not find any result in our database',
          }).then(() => {
              console.log("Success !")
          }).catch(e => {
              console.error("An error occurred :", e)
          })
      }else{
        this.query = this.result[0];
        this.result.shift();
        this.suggestionString = localStorage.getItem('suggestion');
        console.log(this.suggestionString);
        this.suggestion = this.suggestionString.split(':');
        speech.speak({
         text: 'For Your Query We found the Result'+ this.result,
         }).then(() => {
             console.log("Success !")
         }).catch(e => {
             console.error("An error occurred :", e)
         })
      }
    }
    

  }
  userLike(query, result){
    this.likeFlag = false;
    console.log(query);
    this.analyticsString = result.join(",");
    console.log(this.analyticsString);
    this.userFlag= "accurate";
    this.userResponseService.userLike(query,this.analyticsString,this.userFlag).subscribe(
      (response) => {
        console.log("response", response);
      },
      (error: any) => {
        console.log("error", error)
      })
  }
  userReport(query, result){
    this.reportFlag = false
    console.log(query);
    this.analyticsString = result.join(",");
    console.log(this.analyticsString);
    this.userFlag= "inaccurate";
    this.userResponseService.userReport(query,this.analyticsString,this.userFlag).subscribe(
      (response) => {
        console.log("response", response);
      },
      (error: any) => {
        console.log("error", error)
      })
  }
  suggestionSearch(searchQuery){
    localStorage.clear();
    let stompClient =this.webSocketService.connect();
    console.log(searchQuery);
    this.medicalSearchService.suggestionSearchService(searchQuery)
                         .subscribe(data=>{
                               console.log(data);
                               let stompClient =this.webSocketService.connect();
    // tslint:disable-next-line: align
    stompClient.connect({},frame =>{
                                  
                                 stompClient.subscribe('/topic/notification',notifications=>{
                                   this.notifications=JSON.parse(notifications.body);
                                   localStorage.setItem('query',this.notifications.query);
                                   localStorage.setItem('status',this.notifications.status);
                                   localStorage.setItem('result',this.notifications.result.join(':'));
                                   if(this.notifications.suggestions!=null){
                                    localStorage.setItem('suggestion',this.notifications.suggestions.join(':'));
                                   }
                                  //  this.route.navigateByUrl('').then(e=>{
                                  //    this.route.navigateByUrl('/search-result');  
                                  //    window.location.reload();                                
                                  //   })
                                 })
                               });
                              
                              },error=>{
                                console.log(error);
                                this.route.navigateByUrl('/medical-domain');
                              });
  }

}
