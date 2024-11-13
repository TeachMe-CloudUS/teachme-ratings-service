// package com.mongodb.starter.rating;

// import com.mongodb.client.MongoCollection;

// import jakarta.persistence.EntityNotFoundException;

// import org.bson.Document;
// import org.bson.conversions.Bson;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.stereotype.Service;

// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.List;

// import static com.mongodb.client.model.Aggregates.*;
// import static com.mongodb.client.model.Projections.*;
// import static com.mongodb.client.model.search.SearchOperator.text;
// import static com.mongodb.client.model.search.SearchOptions.searchOptions;
// import static com.mongodb.client.model.search.SearchPath.fieldPath;

// @Service
// public class RatingServiceImpl implements RatingService {

//     // private final static Logger LOGGER = LoggerFactory.getLogger(RatingServiceImpl.class);
//     // private final MongoCollection<Document> collection;
//     // @Value("${spring.data.mongodb.atlas.search.index}")
//     // private String index;

//     // public RatingServiceImpl(MongoTemplate mongoTemplate) {
//     //     this.collection = mongoTemplate.getCollection("movies");
//     // }

//     // public Collection<Document> findAll(String keywords, int limit) {

//     //     Bson searchStage = search(text(fieldPath("fullplot"), keywords), searchOptions().index(index));
//     //     Bson projectStage = project(fields(excludeId(), include("title", "year", "fullplot", "imdb.rating")));
//     //     Bson limitStage = limit(limit);
//     //     List<Bson> pipeline = List.of(searchStage, projectStage, limitStage);
//     //     List<Document> docs = collection.aggregate(pipeline).into(new ArrayList<>());
//     //     if (docs.isEmpty()) {
//     //         throw new EntityNotFoundException("moviesByKeywords", keywords);
//     //     }
//     //     return docs;
//     // }

// }