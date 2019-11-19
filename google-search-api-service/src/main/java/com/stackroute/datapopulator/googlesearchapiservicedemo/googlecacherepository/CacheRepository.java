package com.stackroute.datapopulator.googlesearchapiservicedemo.googlecacherepository;

import com.stackroute.datapopulator.googlesearchapiservicedemo.domain.Cache;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheRepository extends MongoRepository<Cache, Integer> {

}
