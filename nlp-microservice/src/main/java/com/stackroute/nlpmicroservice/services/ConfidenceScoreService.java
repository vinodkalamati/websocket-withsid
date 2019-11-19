package com.stackroute.nlpmicroservice.services;

import com.google.gson.Gson;
import com.stackroute.nlpmicroservice.domain.Medical;
import com.stackroute.nlpmicroservice.domain.Movie;
import com.stackroute.nlpmicroservice.domain.SitesContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class ConfidenceScoreService {
    private static final Logger LOGGER= LoggerFactory.getLogger(ConfidenceScoreService.class);
    private MovieDbService movieDbService;
    private MedicalDbService medicalDbService;
    private SitesContent sitesContent;

    private Map<String,Integer> linkedTreatmentMap=new LinkedHashMap<>();
    private Map<String,Integer> linkedCausesMap=new LinkedHashMap<>();
    private Map<String,Integer> linkedDcMap=new LinkedHashMap<>();
    private Map<String,Integer> linkedSymptomsMap=new LinkedHashMap<>();
    private Map<String,Integer> linkedMedicineMap=new LinkedHashMap<>();


    private Map<String,Integer> linkCountryMap=  new LinkedHashMap<>();
    private Map<String,Integer> linkPHouseMap=  new LinkedHashMap<>();
    private Map<String,Integer> linkCollectionMap=  new LinkedHashMap<>();
    private Map<String,Integer> linkActorsMap=  new LinkedHashMap<>();
    private Map<String,Integer> linkDirectorsMap=  new LinkedHashMap<>();
    private Map<String,Integer> linkProducersMap=  new LinkedHashMap<>();
    private Map<String,Integer> linkRDateMap=  new LinkedHashMap<>();

    private Movie movie=new Movie();
    private Medical medical=new Medical();

    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public ConfidenceScoreService(MovieDbService movieDbService,MedicalDbService medicalDbService,
                                  KafkaTemplate<String, String> kafkaTemplate,SitesContent sitesContent) {
        this.medicalDbService=medicalDbService;
        this.movieDbService=movieDbService;
        this.kafkaTemplate = kafkaTemplate;
        this.sitesContent=sitesContent;
    }

    // function to sort hash-map by values
    private HashMap<String, Integer> sortByValue(Map<String, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer> > list = new LinkedList<>(hm.entrySet());
        // Sort the list
        list.sort(Comparator.comparing(Map.Entry::getValue));

        // put data from sorted list to hash-map
        HashMap<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
    // This function is used to remove duplicates from list
    private List<String> removeDuplicates(List<String> list)
    {
        LinkedHashSet<String> hashSet1 = new LinkedHashSet<>(list);
        list = new ArrayList<>(hashSet1);
        return list;
    }


    // This function is used to process list of domains.
    public void processListOfDomains(String domain,String concept)
    {
        if(domain.equalsIgnoreCase("medical"))
        {
            List<Medical> medicalObjects=medicalDbService.getAllMedicals();
            for (int i = 0; i < medicalObjects.size(); i++) {
                addUniqueTreatments(medicalObjects.get(i).getTreatment());
                addUniqueCauses(medicalObjects.get(i).getCauses());
                addUniqueDcs(medicalObjects.get(i).getDeathCount());
                addUniqueSymptoms(medicalObjects.get(i).getSymptoms());
                addUniqueMedicines(medicalObjects.get(i).getMedicine());

            }
            Medical finalMedical=createMedical(concept);
            Gson gson = new Gson();
            String json = gson.toJson(finalMedical);
            kafkaTemplate.send("NlpResult",json);

        }
        else if(domain.equalsIgnoreCase("movie"))
        {
            List<Movie> movieObjects=movieDbService.getAllMovies();
            for (int i = 0; i < movieObjects.size(); i++) {
                addUniqueCountry(movieObjects.get(i).getCountry());
                addUniqueProductionHouse(movieObjects.get(i).getProductionHouse());
                addUniqueRDates(movieObjects.get(i).getReleaseDate());
                addUniqueDirectors(movieObjects.get(i).getDirectors());
                addUniqueCollections(movieObjects.get(i).getCollection());
                addUniqueActors(movieObjects.get(i).getCasts());
                addUniqueProducers(movieObjects.get(i).getProducers());
            }
            Movie finalMovie=createMovie(concept);
            Gson gson = new Gson();
            String json = gson.toJson(finalMovie);
            kafkaTemplate.send("NlpResult",json);
        }
        else
        {
            LOGGER.info("no object found in domain list of Confidence score service");
        }

    }
    // This function is used to create String
    private Medical createMedical(String concept) {
        medical.setDisease(concept);
        medical.setDomain("medical");
        medical.setId(sitesContent.getUserId());

        linkedCausesMap=sortByValue(linkedCausesMap);
        List<String> lCauses=new ArrayList<>(linkedCausesMap.keySet());
        if(!lCauses.isEmpty()) {
            medical.setCauses(lCauses);
        }

        linkedSymptomsMap=sortByValue(linkedSymptomsMap);
        List<String> lSymptoms=new ArrayList<>(linkedSymptomsMap.keySet());
        if(!lSymptoms.isEmpty()) {
            medical.setSymptoms(lSymptoms);
        }

        linkedTreatmentMap=sortByValue(linkedTreatmentMap);
        List<String> lTreatment=new ArrayList<>(linkedTreatmentMap.keySet());
        if(!lTreatment.isEmpty()) {
            medical.setTreatment(lTreatment);
        }

        linkedMedicineMap=sortByValue(linkedMedicineMap);
        List<String> lMedicine=new ArrayList<>(linkedMedicineMap.keySet());
        if(!lMedicine.isEmpty()) {
            medical.setMedicine(lMedicine);
        }

        List<String> lDcm=new ArrayList<>(linkedDcMap.keySet());
        String maxFound="";
        int maxCount=0;
        if(!lDcm.isEmpty()) {
            for (String s : lDcm) {
                if (linkedDcMap.get(s) > maxCount) {
                    maxCount = linkedDcMap.get(s);
                    maxFound = s;
                }
            }
            if(maxFound.length()>0) {
                lDcm.clear();
                lDcm.add(maxFound);
                medical.setDeathCount(lDcm);
            }
        }
        return medical;
    }

    // This function is used to movie String
    private Movie createMovie(String concept) {
        movie.setMovieName(concept);
        movie.setDomain("movie");
        medical.setId(sitesContent.getUserId());

        linkPHouseMap=sortByValue(linkPHouseMap);
        List<String> lph=new ArrayList<>(linkPHouseMap.keySet());
        if(!lph.isEmpty()) {
            movie.setProductionHouse(lph);
        }

        linkCountryMap=sortByValue(linkCountryMap);
        List<String> lc=new ArrayList<>(linkCountryMap.keySet());
        if(!lc.isEmpty()) {
            String country=lc.get(0);
            lc.clear();
            lc.add(country);
            movie.setCountry(lc);
        }

        List<String> lrd=new ArrayList<>(linkRDateMap.keySet());
        String mx="";
        if(!lrd.isEmpty()) {
            for (String s : lrd) {
                if (s.length() > mx.length()) {
                    mx = s;
                }
            }
            lrd.clear();
            lrd.add(mx);
            movie.setReleaseDate(lrd);
        }

        List<String> lcm=new ArrayList<>(linkCollectionMap.keySet());
        String maxFound="";
        int maxCount=0;
        if(!lcm.isEmpty()) {
            for (String s : lcm) {
                if (linkCollectionMap.get(s) > maxCount) {
                    maxCount = linkCollectionMap.get(s);
                    maxFound = s;
                }
            }
            if(maxFound.length()>0) {
                lcm.clear();
                lcm.add(maxFound);
                movie.setCollection(lcm);
            }
        }

        List<String> ld=new ArrayList<>(linkDirectorsMap.keySet());
        if(!ld.isEmpty()) {
            movie.setDirectors(ld);
        }
        else
        {
            ld.add("Not Found");
            movie.setDirectors(ld);
            ld.clear();
        }

        List<String> lp=new ArrayList<>(linkProducersMap.keySet());
        if(!lp.isEmpty()) {
            movie.setProducers(lp);
        }
        else
        {
            lp.add("Not Found");
            movie.setProducers(lp);
            lp.clear();
        }
        List<String> la=new ArrayList<>(linkActorsMap.keySet());
        if(!la.isEmpty()) {
            movie.setCasts(la);
        }
        else
        {
            la.add("Not Found");
            movie.setCasts(la);
            la.clear();
        }
        return movie;
    }

    private void addUniqueTreatments(List<String> treatment)
    {
        treatment= removeDuplicates(treatment);
        if(!treatment.isEmpty()) {
            for (String s : treatment) {
                if (linkedTreatmentMap.containsKey(s)) {
                    linkedTreatmentMap.put(s, linkedTreatmentMap.get(s) + 1);
                } else {
                    linkedTreatmentMap.put(s, 1);
                }
            }
        }
    }
    private void addUniqueMedicines(List<String> medicines)
    {
        medicines= removeDuplicates(medicines);
        if(!medicines.isEmpty()) {
            for (String medicine : medicines) {
                if (linkedMedicineMap.containsKey(medicine)) {
                    linkedMedicineMap.put(medicine, linkedMedicineMap.get(medicine) + 1);
                } else {
                    linkedMedicineMap.put(medicine, 1);
                }
            }
        }
    }
    private void addUniqueCauses(List<String> causes)
    {
        causes= removeDuplicates(causes);
        if(!causes.isEmpty()) {
            for (String cause : causes) {
                if (linkedCausesMap.containsKey(cause)) {
                    linkedCausesMap.put(cause, linkedCausesMap.get(cause) + 1);
                } else {
                    linkedCausesMap.put(cause, 1);
                }
            }
        }

    }
    private void addUniqueDcs(List<String> deathCounts)
    {
        deathCounts= removeDuplicates(deathCounts);
        if(!deathCounts.isEmpty()) {
            for (String deathCount : deathCounts) {
                if (linkedDcMap.containsKey(deathCount)) {
                    linkedDcMap.put(deathCount, linkedDcMap.get(deathCount) + 1);
                } else {
                    linkedDcMap.put(deathCount, 1);
                }
            }
        }


    }
    private void addUniqueSymptoms(List<String> symptoms)
    {
        symptoms= removeDuplicates(symptoms);
        if(!symptoms.isEmpty()) {
            for (String symptom : symptoms) {
                if (linkedSymptomsMap.containsKey(symptom)) {
                    linkedSymptomsMap.put(symptom, linkedSymptomsMap.get(symptom) + 1);
                } else {
                    linkedSymptomsMap.put(symptom, 1);
                }
            }
        }
    }
    private void addUniqueActors(List<String> actors)
    {
        actors= removeDuplicates(actors);
        if(!actors.isEmpty()) {
            for (String actor : actors) {
                if (linkActorsMap.containsKey(actor)) {
                    linkActorsMap.put(actor, linkActorsMap.get(actor) + 1);
                } else {
                    linkActorsMap.put(actor, 1);
                }
            }
        }
    }
    private void addUniqueProducers(List<String> producers)
    {
        producers= removeDuplicates(producers);
        if(!producers.isEmpty()) {
            for (String producer : producers) {
                if (linkProducersMap.containsKey(producer)) {
                    linkProducersMap.put(producer, linkProducersMap.get(producer) + 1);
                } else {
                    linkProducersMap.put(producer, 1);
                }
            }
        }
    }
    private void addUniqueDirectors(List<String> directors)
    {
        directors= removeDuplicates(directors);
        if(!directors.isEmpty()) {
            for (String director : directors) {
                if (linkDirectorsMap.containsKey(director)) {
                    linkDirectorsMap.put(director, linkDirectorsMap.get(director) + 1);
                } else {
                    linkDirectorsMap.put(director, 1);
                }
            }
        }
    }
    private void addUniqueCollections(List<String> collectionsList)
    {
        collectionsList= removeDuplicates(collectionsList);
        if(!collectionsList.isEmpty()) {
            for (String s : collectionsList) {
                if (linkCollectionMap.containsKey(s)) {
                    linkCollectionMap.put(s, linkCollectionMap.get(s) + 1);
                } else {
                    linkCollectionMap.put(s, 1);
                }
            }
        }
    }
    private void addUniqueRDates(List<String> rDates)
    {
        rDates= removeDuplicates(rDates);
        if(!rDates.isEmpty()) {
            for (String rDate : rDates) {
                if (linkRDateMap.containsKey(rDate)) {
                    linkRDateMap.put(rDate, linkRDateMap.get(rDate) + 1);
                } else {
                    linkRDateMap.put(rDate, 1);
                }
            }
        }
    }
    private void addUniqueCountry(List<String> countries)
    {
        countries= removeDuplicates(countries);
        if(!countries.isEmpty()) {
            for (String country : countries) {
                if (linkCountryMap.containsKey(country)) {
                    linkCountryMap.put(country, linkCountryMap.get(country) + 1);
                } else {
                    linkCountryMap.put(country, 1);
                }
            }
        }
    }

    private void addUniqueProductionHouse(List<String> pHouses)
    {
        pHouses= removeDuplicates(pHouses);
        if(!pHouses.isEmpty()) {
            for (String pHouse : pHouses) {
                if (linkPHouseMap.containsKey(pHouse)) {
                    linkPHouseMap.put(pHouse, linkPHouseMap.get(pHouse) + 1);
                } else {
                    linkPHouseMap.put(pHouse, 1);
                }
            }
        }
    }
}
