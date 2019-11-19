package com.stackroute.queryservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackroute.queryservice.domain.*;
import com.stackroute.queryservice.service.MedicineQueryService;
import com.stackroute.queryservice.service.MovieQueryService;
import com.stackroute.queryservice.service.ResponseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Api(value = "QuerySearch Rest API")
@RestController
@CrossOrigin("*")
@RequestMapping("api/v1")
public class QueryServiceController {
    private ResponseEntity responseEntity;
    private ResponseService responseService;
    private String topic="QueryResult";

    KafkaTemplate<String, String> kafkaTemplate;
    //Autowiring Kafka and services
    @Autowired
    public QueryServiceController(KafkaTemplate<String, String> kafkaTemplate, ResponseService responseService) {
        this.kafkaTemplate = kafkaTemplate;
        this.responseService = responseService;
    }

    @ApiOperation(value = "Perform NLP on User Query")
    @PostMapping("query")
    public ResponseEntity<?> processQuery(@RequestBody QueryInput queryInput) throws JsonProcessingException {
        System.out.println("sessionId*****************"+queryInput.getSessionId());
	    System.out.println("queryServiceCaslled");
	    //checks query for domain
        if (queryInput.getDomain().equalsIgnoreCase("movie")) {
            try {
                MovieQueryService movieQueryNlp = new MovieQueryService();
                List<String> lemmatizedList = movieQueryNlp.getLemmatizedList(queryInput.getSearchTerm());
                String lemmatizedQuery = "";
                for (int i = 0; i < lemmatizedList.size(); i++) {
                    lemmatizedQuery = lemmatizedQuery + lemmatizedList.get(i) + " ";
                }
                Output output = movieQueryNlp.RedisMatcher(lemmatizedQuery.trim().toLowerCase());
                output.setSessionId(queryInput.getSessionId());
                String status = movieQueryNlp.checkDict(output);
                /*QueryService finds the queryResult and constraints. If both are present, status is
                  continue and result is sent to Result Fetcher. If both are not there, status is not Found and
                  result is sent to Notification Service.
                * */
                if(status.equals("continue"))
                {
                    QueryOutput queryOutput = new QueryOutput();
                    queryOutput.setDomain("movie");
                    queryOutput.setQuery(output.getStrForDict());
                    queryOutput.setConstraints(output.getConstraints());
                    queryOutput.setSessionId(queryInput.getSessionId());
                    String[] queryResults = output.getQueryResult();
                    List<String> finalQueryResult = new ArrayList<>();
                    for (String queryresult : queryResults) {
                        finalQueryResult.add(queryresult);
                    }
                    queryOutput.setQueryresult(finalQueryResult);
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(queryOutput);
                    kafkaTemplate.send(topic, json);
                    responseEntity = new ResponseEntity<String>(json, HttpStatus.OK);
                } else if(status.equals("notFound"))
                {
                    NotificationOutput output1 = new NotificationOutput();
                    output1.setStatus("notFound");
                    output1.setQuery(output.getStrForDict());
                    output1.setSessionId(queryInput.getSessionId());
                    NotFoundResponse response = new NotFoundResponse();
                    response.setDomain("movie");
                    response.setQuery(queryInput.getSearchTerm());
                    responseService.saveResponse(response);
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(output1);
                    kafkaTemplate.send("FinalResult",json);
                    responseEntity = new ResponseEntity<String>(json, HttpStatus.OK);
                }
            } catch (Exception exception) {
                responseEntity = new ResponseEntity<String>(exception.getMessage(), HttpStatus.CONFLICT);
            }
        } else if (queryInput.getDomain().equalsIgnoreCase("medical")) {
            try {
                MedicineQueryService medicineQueryNlp = new MedicineQueryService();
                List<String> lemmatizedList = medicineQueryNlp.getLemmatizedList(queryInput.getSearchTerm());
                String lemmatizedString = "";
                for (int i = 0; i < lemmatizedList.size(); i++) {
                    lemmatizedString = lemmatizedString + lemmatizedList.get(i) + " ";
                }
                Output output = medicineQueryNlp.RedisMatcher(lemmatizedString.trim().toLowerCase());
                output.setSessionId(queryInput.getSessionId());
                String status = medicineQueryNlp.checkDict(output);
                /*QueryService finds the queryResult and constraints. If both are present, status is
                  continue and result is sent to Result Fetcher. If both are not there, status is not Found and
                  result is sent to Notification Service.
                * */
                if(status.equals("continue"))
                {
                    QueryOutput queryOutput = new QueryOutput();
                    queryOutput.setDomain("medical");
                    queryOutput.setQuery(output.getStrForDict());
                    queryOutput.setConstraints(output.getConstraints());
                    queryOutput.setSessionId(queryInput.getSessionId());
                    String[] queryResult = output.getQueryResult();
                    List<String> finalQueryResult = new ArrayList<>();
                    for (String a : queryResult) {
                        finalQueryResult.add(a);
                    }
                    queryOutput.setQueryresult(finalQueryResult);
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(queryOutput);
                    kafkaTemplate.send(topic, json);
                    responseEntity = new ResponseEntity<String>(json, HttpStatus.OK);
                } else if (status.equals("wait")) {
                    responseEntity = new ResponseEntity<String>("Waiting for internal pipeline trigger",HttpStatus.PROCESSING);
                } else if (status.equals("notFound")) {
                    NotificationOutput output1 = new NotificationOutput();
                    output1.setStatus("notFound");
                    output1.setQuery(output.getStrForDict());
                    output1.setSessionId(queryInput.getSessionId());
                    System.out.println(queryInput.getSessionId());
                    NotFoundResponse response = new NotFoundResponse();
                    response.setDomain("medical");
                    response.setQuery(queryInput.getSearchTerm());
                    responseService.saveResponse(response);
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(output1);
                    kafkaTemplate.send("FinalResult",json);
                    responseEntity = new ResponseEntity<String>(json, HttpStatus.OK);
                }
            } catch (Exception e) {
                responseEntity = new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
                e.printStackTrace();
            }
        }
        return responseEntity;
    }
    //This method is called by the expert analytics page for the domain expert to edit the neo4j data.
    @PostMapping("analyticsquery")
    public ResponseEntity<?> processAnalyticsQuery(@RequestBody QueryInput input) throws IOException, ParseException {
        if(input.getDomain().equalsIgnoreCase("movie"))
        {
            MovieQueryService movieQueryNlp = new MovieQueryService();
            List<String> lemmatizedList = movieQueryNlp.getLemmatizedList(input.getSearchTerm());
            String lemmatizedQuery = "";
            for (int i = 0; i < lemmatizedList.size(); i++) {
                lemmatizedQuery = lemmatizedQuery + lemmatizedList.get(i) + " ";
            }
            Output output = movieQueryNlp.RedisMatcher(lemmatizedQuery.trim().toLowerCase());
            QueryOutput queryOutput = new QueryOutput();
            queryOutput.setDomain("movie");
            queryOutput.setQuery(output.getStrForDict());
            queryOutput.setConstraints(output.getConstraints());
            String[] queryResult = output.getQueryResult();
            List<String> finalQueryResult = new ArrayList<>();
            for (String a : queryResult) {
                finalQueryResult.add(a);
            }
            queryOutput.setQueryresult(finalQueryResult);
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(queryOutput);
            kafkaTemplate.send("FrontEndQuery", json);
            responseEntity = new ResponseEntity<String>(json, HttpStatus.OK);
        } else if (input.getDomain().equalsIgnoreCase("medical"))
        {
            MedicineQueryService medicineQueryNlp = new MedicineQueryService();
            List<String> lemmatizedList = medicineQueryNlp.getLemmatizedList(input.getSearchTerm());
            String lemmatizedString = "";
            for (int i = 0; i < lemmatizedList.size(); i++) {
                lemmatizedString = lemmatizedString + lemmatizedList.get(i) + " ";
            }
            Output output = medicineQueryNlp.RedisMatcher(lemmatizedString.trim().toLowerCase());
            QueryOutput queryOutput = new QueryOutput();
            queryOutput.setDomain("medical");
            queryOutput.setQuery(output.getStrForDict());
            queryOutput.setConstraints(output.getConstraints());
            String[] queryResult = output.getQueryResult();
            List<String> finalQueryResult = new ArrayList<>();
            for (String a : queryResult) {
                finalQueryResult.add(a);
            }
            queryOutput.setQueryresult(finalQueryResult);
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(queryOutput);
            kafkaTemplate.send("FrontEndQuery", json);
            responseEntity = new ResponseEntity<String>(json, HttpStatus.OK);
        }
        return responseEntity;
    }
    //This method returns all untagged queries (Result Not Found) in the mongo database for the domain expert to view.
    @GetMapping("response")
    public ResponseEntity<?> getResponses() {
        try {
            List<NotFoundResponse> list = responseService.getResponses();
            NotFoundResponse[] responses = new NotFoundResponse[list.size()];
            int i = 0;
            for(NotFoundResponse response:list)
            {
                responses[i] = response;
                i++;
            }
            responseEntity = new ResponseEntity<NotFoundResponse[]>(responses,HttpStatus.OK);
        } catch (Exception e) {
            responseEntity = new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
}
