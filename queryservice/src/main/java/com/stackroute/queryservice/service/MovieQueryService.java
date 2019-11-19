package com.stackroute.queryservice.service;


import com.stackroute.queryservice.domain.Output;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LevenshteinDistance;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.RAMDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovieQueryService {
    //Initialize arraylists for each node in neo4j
    ArrayList<String> conceptList = new ArrayList<>();
    ArrayList<String> movieList = new ArrayList<>();
    ArrayList<String> boxOfficeList = new ArrayList<>();
    ArrayList<String> productionHouseList = new ArrayList<>();
    ArrayList<String> actorList = new ArrayList<>();
    ArrayList<String> directorList = new ArrayList<>();
    ArrayList<String> releaseYearList = new ArrayList<>();
    ArrayList<String> countryList = new ArrayList<>();
    ArrayList<String> languageList = new ArrayList<>();
    //Constructor fetches all nodes from Redis and stores in arraylists.
    public MovieQueryService() throws IOException, ParseException {
        JSONObject obj = fetchRedis();
        if(obj.containsKey("labels"))
        {
            JSONObject concepts = (JSONObject) obj.get("labels");
            JSONArray concept = (JSONArray) concepts.get("value");
            for(int i=0;i<concept.size();i++)
            {
                conceptList.add((String) concept.get(i));
            }
        }
        if(obj.containsKey("movie"))
        {
            JSONObject movies = (JSONObject) obj.get("movie");
            JSONArray movie = (JSONArray) movies.get("value");
            for(int i=0;i<movie.size();i++)
            {
                movieList.add((String) movie.get(i));
            }
        }
        if(obj.containsKey("boxoffice"))
        {
            JSONObject collections = (JSONObject) obj.get("boxoffice");
            JSONArray collection = (JSONArray) collections.get("value");
            for(int i=0;i<collection.size();i++)
            {
                boxOfficeList.add((String) collection.get(i));
            }
        }
        if(obj.containsKey("producer"))
        {
            JSONObject productions = (JSONObject) obj.get("producer");
            JSONArray production = (JSONArray) productions.get("value");
            for(int i=0;i<production.size();i++)
            {
                productionHouseList.add((String) production.get(i));
            }
        }
        if(obj.containsKey("artist"))
        {
            JSONObject actors = (JSONObject) obj.get("artist");
            JSONArray actor = (JSONArray) actors.get("value");
            for(int i=0;i<actor.size();i++)
            {
                actorList.add((String) actor.get(i));
            }
        }
        if(obj.containsKey("director"))
        {
            JSONObject directors = (JSONObject) obj.get("director");
            JSONArray director = (JSONArray) directors.get("value");
            for(int i=0;i<director.size();i++)
            {
                directorList.add((String) director.get(i));
            }
        }
        if(obj.containsKey("release date"))
        {
            JSONObject releases = (JSONObject) obj.get("release date");
            JSONArray release = (JSONArray) releases.get("value");
            for(int i=0;i<release.size();i++)
            {
                releaseYearList.add((String) release.get(i));
            }
        }
        if(obj.containsKey("country"))
        {
            JSONObject countries = (JSONObject) obj.get("country");
            JSONArray country = (JSONArray) countries.get("value");
            for(int i=0;i<country.size();i++)
            {
                countryList.add((String) country.get(i));
            }
        }
        if(obj.containsKey("language"))
        {
            JSONObject languages = (JSONObject) obj.get("language");
            JSONArray language = (JSONArray) languages.get("value");
            for(int i=0;i<language.size();i++)
            {
                languageList.add((String) language.get(i));
            }
        }
    }
    //Setting properties for NLP
    private static Properties properties;
    private static String propertiesName = "tokenize, ssplit, pos, lemma";
    private static StanfordCoreNLP stanfordCoreNLP;
    static {
        properties = new Properties();
        properties.setProperty("annotators", propertiesName);
    }
    public static StanfordCoreNLP getPipeline() {
        if (stanfordCoreNLP == null) {
            stanfordCoreNLP = new StanfordCoreNLP(properties);
        }
        return stanfordCoreNLP;
    }

    public JSONObject fetchRedis() throws IOException, org.json.simple.parser.ParseException {
        String link = "http://13.127.108.14:8134/api";
//        String link = "http://104.154.175.62:8134/api";
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int resp = connection.getResponseCode();
        if(resp!=200)
        {
            throw new RuntimeException("HTTP Response Code :" + resp);
        }
        else {
            //Reading the obtained values from API Call
            Scanner sc = new Scanner(url.openStream());
            String text = "";
            while (sc.hasNext()) {
                text += sc.nextLine();
            }
            sc.close();
            JSONParser parser = new JSONParser();
            JSONObject jobj = (JSONObject)parser.parse(text);
            return jobj;
        }
    }
    //Removes unwanted characters from query
    public String getTrimmedQuery(String query) {
        String trimmedQuery = query.trim();
        trimmedQuery = trimmedQuery.replaceAll("\\s+", " ");
        trimmedQuery = trimmedQuery.replaceAll("\\t", " ");
        trimmedQuery = trimmedQuery.replaceAll("[?.!,]","");
        return trimmedQuery;
    }
    //Method uses NLP to tokenize and lemmatize the query

    public List<String> getLemmatizedList(String query) {

        List<String> lemmatizedWordsList = new ArrayList<String>();
        String trimmedQuery = getTrimmedQuery(query);
        Annotation document = new Annotation(trimmedQuery);
        StanfordCoreNLP stanfordCoreNLP = getPipeline();
        // run all Annotators on this text
        stanfordCoreNLP.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmatizedWordsList.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return lemmatizedWordsList;
    }

    //Method checks the lemmatized query for each term in the arraylists and finds the relevant terms
    public Output RedisMatcher(String lemmatizedString) throws IOException {
        Output output = new Output();
        List<Map> constraints = new ArrayList<>();

        for(int i=0; i < movieList.size(); i++ ){
            Map<String,String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(movieList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("movie", "");
                map.put("key","movie");
                map.put("value", movieList.get(i));
                constraints.add(map);
            }
        }
        //Checks query for productionHouseList from redis
        for(int i=0; i < productionHouseList.size(); i++ ){
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(productionHouseList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("production house", "");
                map.put("key","producer");
                map.put("value", productionHouseList.get(i));
                constraints.add(map);
            }
        }
        //Checks query for ActorList from redis

        for(int i=0; i<actorList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(actorList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("act", "");
                lemmatizedString = lemmatizedString.replaceAll("artist", "");
                map.put("key","artist");
                map.put("value",actorList.get(i));
                constraints.add(map);
            }
        }

        //Checks query for directorList from redis

        for(int i=0; i<directorList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(directorList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("direct", "");
                map.put("key","director");
                map.put("value",directorList.get(i));
                constraints.add(map);
            }
        }

        //Checks query for countryList from redis

        for(int i=0; i<countryList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(countryList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("located", "");
                lemmatizedString = lemmatizedString.replaceAll("set", "");
                map.put("key","country");
                map.put("value",countryList.get(i));
                constraints.add(map);
            }
        }

        //Checks query for languageList from redis

        for(int i=0; i<languageList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(languageList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("language", "");
                map.put("key", "language");
                map.put("value",languageList.get(i));
                constraints.add(map);
            }
        }

        //Checks query for releaseYearList from redis

        for(int i=0; i < releaseYearList.size(); i++ )
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(releaseYearList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("release", "");
                map.put("key","release date");
                map.put("value",releaseYearList.get(i));
                constraints.add(map);
            }
        }

        //Checks query for BoxOfficeList from redis

        for(int i=0; i < boxOfficeList.size(); i++ ){
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(boxOfficeList.get(i));
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("box office collection", "");
                lemmatizedString = lemmatizedString.replaceAll("collect", "");
                lemmatizedString = lemmatizedString.replaceAll("gross", "");
                map.put("key","boxoffice");
                map.put("value",boxOfficeList.get(i));
                constraints.add(map);
            }
        }
        output.setConstraint(constraints);

        String corrLemmatizedString = null;
        RAMDirectory dir = new RAMDirectory();
        SpellChecker spellChecker = new SpellChecker(dir);
        IndexWriterConfig config = new IndexWriterConfig(null);
        spellChecker.indexDictionary(new PlainTextDictionary(Paths.get("resources/words_alpha.txt")),config,false);
        spellChecker.setStringDistance(new LevenshteinDistance());
        String[] strings = lemmatizedString.split(" ");
        for(String a:strings)
        {
            if(!(spellChecker.exist(a.trim())))
            {
                if(a.trim().length()>=3)
                {
                    String[] suggestions = spellChecker.suggestSimilar(a.trim(),1);
                    corrLemmatizedString = lemmatizedString.replace(a.trim(),suggestions[0]);
                }
            }
        }
        dir.close();
        spellChecker.close();
        if(corrLemmatizedString==null)
        {
            corrLemmatizedString = lemmatizedString;
        }

        List<String> concepts = new ArrayList<>();
        for (int i=0; i<conceptList.size();i++)
        {
            corrLemmatizedString = corrLemmatizedString.replaceAll("film", "movie");
            corrLemmatizedString = corrLemmatizedString.replaceAll("\\bact\\b", "artist");
            corrLemmatizedString = corrLemmatizedString.replaceAll("\\bproduce\\b", "producer");
            corrLemmatizedString = corrLemmatizedString.replaceAll("\\bdirect\\b", "director");
            corrLemmatizedString = corrLemmatizedString.replaceAll("release", "release date");
            Pattern pattern = Pattern.compile(conceptList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(corrLemmatizedString);
            if(matcher.find())
            {
                concepts.add(conceptList.get(i));
            }
        }
        String[] conceptsArray = new String[concepts.size()];
        int i=0;
        for(String a:concepts)
        {
            conceptsArray[i] = a;
            i++;
        }
        output.setQueryResult(conceptsArray);
        output.setStrForDict(corrLemmatizedString);
        output.setQuery(lemmatizedString);
        return output;
    }
    public String checkDict(Output output) throws IOException, ParseException, URISyntaxException {
        String status = "continue";
        if(output.getConstraints().size()==0||output.getQueryResult().length==0)
            status = "notFound";
        return status;
    }
}

