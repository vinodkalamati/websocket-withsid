import { Component, OnInit } from '@angular/core';
import {MovieSearchService} from '../movie_search_service/movie-search.service';
import { Router } from '@angular/router';
import {WebSocketService} from '../websocket-service/websocket.service';
import { SpeechService } from '../speech-service/speech.service';
import Speech from 'speak-tts' ;

@Component({
  selector: 'app-movie-domain-search',
  templateUrl: './movie-domain-search.component.html',
  styleUrls: ['./movie-domain-search.component.css']
})
export class MovieDomainSearchComponent implements OnInit {

  //SpeechRecognition variables
  startListenButton: boolean;
  stopListeningButton: boolean;
  speechData: string;
  sessionId:any;

  constructor( private movieSearchService: MovieSearchService, private route:Router,private speechService: SpeechService,private webSocketService: WebSocketService) { }
  notifications:any;
  results:String;
  ngOnInit() {
    localStorage.clear();

    this.sessionId=new Date().valueOf();
    localStorage.setItem("sessionId",this.sessionId);
    let sessionId=localStorage.getItem("sessionId");
    console.log(sessionId+"^^^^^^^^^^");
    console.log(this.sessionId);

    const speech = new Speech() // will throw an exception if not browser supported
    if(speech.hasBrowserSupport()) { // returns a boolean
        console.log("speech synthesis supported")
    }
    let stompClient =this.webSocketService.connect();
    stompClient.connect({},frame =>{
                                 stompClient.subscribe('/topic/notification/'+sessionId,notifications=>{
                                   this.notifications=JSON.parse(notifications.body);
                                   localStorage.setItem('query',this.notifications.query);
                                   localStorage.setItem('status',this.notifications.status);
                                   localStorage.setItem('result',this.notifications.result.join(':'));
                                   if(this.notifications.suggestions!=null){
                                    localStorage.setItem('suggestion',this.notifications.suggestions.join(':'));
                                   }
                                   this.results="results";
                                   this.route.navigateByUrl('/search-result');
                                 })
                               });
  }
  flag:boolean=false;
  userSearch(searchQuery){
    const speech = new Speech();
    speech.speak({
      text: 'Please wait!! We are searching appropriate result for you',
      }).then(() => {
          console.log("Success !")
      }).catch(e => {
          console.error("An error occurred :", e)
      })
    console.log(searchQuery);
    this.flag=true;
    localStorage.setItem('domain', "movie");
    this.movieSearchService.userSearchService(searchQuery)
     .subscribe(data=>{
      console.log(data);
     },error=>{
       console.log(error);
       this.route.navigateByUrl('/movie-domain');
     });
  }

  formValue: String;
  activateSpeechSearch(): void {
    const speech = new Speech()
     speech.speak({
      text: 'Speak!!! We are listening',
  }).then(() => {
      console.log("Success !")
  }).catch(e => {
      console.error("An error occurred :", e)
  })
    this.startListenButton = false;

    this.speechService.record()
        .subscribe(
        //listener
        (value) => {
            this.speechData = value;
            this.formValue = value;
            console.log('listener.speechData:', value);
        },
        //error
        (err) => {
            console.log(err);
            if (err.error == "no-speech") {
                console.log("--restarting service--");
                this.activateSpeechSearch();
            }
        },
        //completion
        () => {
            this.startListenButton = true;
            console.log("--complete--");
            this.sendMessageFromSpeechRecognition();
            console.log('this.stopListeningButton', this.stopListeningButton);
            // if (!this.stopListeningButton) {
            //   this.activateSpeechSearch();
            // }

        });
  }

  deActivateSpeechSearch(): void {
    console.log("stop listening")
    this.startListenButton = true;
    this.stopListeningButton = true;
    this.speechService.DestroySpeechObject();
  }
  
  sendMessageFromSpeechRecognition(): void {
    
    this.speechService.DestroySpeechObject();
    console.log("fjds"+this.formValue);
    const speech = new Speech()
    speech.speak({
     text: 'Did you say?'+ this.formValue,
     }).then(() => {
         console.log("Success !")
     }).catch(e => {
         console.error("An error occurred :", e)
     })
    this.flag =true;
    localStorage.setItem('domain', "medical");
     this.movieSearchService.suggestionSearchService(this.formValue)
                        .subscribe(data=>{
                              console.log(data);
                              

                             },error=>{
                               console.log(error);
                               this.route.navigateByUrl('/medical-domain');
                             });
  }



}
