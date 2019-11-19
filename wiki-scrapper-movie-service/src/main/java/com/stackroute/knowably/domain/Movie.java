package com.stackroute.knowably.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Movie {
    private String id;
    private String userId;
    private String query;
    private String domain;
    private String concept;
    private String url;
    private String sessionId;
}
