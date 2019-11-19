import { Component, OnInit } from '@angular/core';
import {SpeechService} from '../speech-service/speech.service';
import {MedicalSearchService} from '../medical_search_service/medical-search.service';
import { Router } from '@angular/router';
import {WebSocketService} from '../websocket-service/websocket.service';
import Speech from 'speak-tts' ;


@Component({
  selector: 'app-medical-domain-search',
  templateUrl: './medical-domain-search.component.html',
  styleUrls: ['./medical-domain-search.component.css']
})
export class MedicalDomainSearchComponent implements OnInit {

  //SpeechRecognition variables
    startListenButton: boolean;
    stopListeningButton: boolean;
    speechData: string;

    notifications:any;
    results:String;
    name: string;
   // tslint:disable-next-line: max-line-length
   constructor(private speechService: SpeechService,private webSocketService: WebSocketService,  private medicalSearchService: MedicalSearchService, private route:Router ) {
     this.startListenButton = true;
     this.stopListeningButton = false;
     this.speechData = '';
   }
   formValue: String;
   ngOnInit() {
    const speech = new Speech() // will throw an exception if not browser supported
    if(speech.hasBrowserSupport()) { // returns a boolean
        console.log("speech synthesis supported")
    }
     localStorage.clear();
     let stompClient =this.webSocketService.connect();
                               stompClient.connect({},frame =>{
                                 stompClient.subscribe('/topic/notification',notifications=>{
                                   this.notifications=JSON.parse(notifications.body);
                                   localStorage.setItem('query',this.notifications.query);
                                   localStorage.setItem('status',this.notifications.status);
                                   localStorage.setItem('result',this.notifications.result.join(':'));
                                   if(this.notifications.suggestions!=null){
                                     console.log(this.notifications.suggestions)
                                    localStorage.setItem('suggestion',this.notifications.suggestions.join(':'));
                                   }
                                   this.results="results";
                                   this.route.navigateByUrl('/search-result');
                                 })
                               });
   }

   ngOnDestroy() {
     this.speechService.DestroySpeechObject();
   }
   flag:boolean =false;
   userSearch(searchQuery){
    const speech = new Speech();
      speech.speak({
        text: 'Please wait!! We are searching appropriate result for you',
        }).then(() => {
            console.log("Success !")
        }).catch(e => {
            console.error("An error occurred :", e)
        })
        this.flag =true;

     localStorage.setItem('domain', "medical");
      this.medicalSearchService.userSearchService(searchQuery)
                         .subscribe(data=>{
                               console.log(data);
                               

                              },error=>{
                                console.log(error);
                                this.route.navigateByUrl('/medical-domain');
                              });


    }
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
   // let stompClient =this.webSocketService._connect();
   // stompClient._connect({},frame =>{
   //   stompClient.subscribe('/topic/notification',notifications=>{
   //     this.notifications=JSON.parse(notifications.body);
   //     localStorage.setItem('result', this.notifications.join(':'));
   //     console.log(this.notifications);
   //     this.results="results";
   //     this.route.navigateByUrl('/search-result');
   //   })
   // });
   
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
     speech.speak({
      text: 'Please wait!! We are searching appropriate result for you',
      }).then(() => {
          console.log("Success !")
      }).catch(e => {
          console.error("An error occurred :", e)
      })
      this.medicalSearchService.suggestionSearchService(this.formValue)
                         .subscribe(data=>{
                               console.log(data);
                              },error=>{
                                console.log(error);
                                this.route.navigateByUrl('/medical-domain');
                              });
   }

}
