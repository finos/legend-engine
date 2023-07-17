package org.finos.legend.engine.application.query.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QuerySearchSpecification;
import org.finos.legend.engine.application.query.model.QueryStoreStats;

import java.util.List;

public interface QueryStoreManager {

    List<Query> searchQueries(QuerySearchSpecification searchSpecification, String currentUser);
    List<Query> getQueries(List<String> queryIds);
    Query getQuery(String queryId);
    QueryStoreStats getQueryStoreStats() throws JsonProcessingException;
    Query createQuery(Query query, String currentUser) throws JsonProcessingException;
    Query updateQuery(String queryId, Query query, String currentUser) throws JsonProcessingException;
    void deleteQuery(String queryId, String currentUser) throws JsonProcessingException;
    List<QueryEvent> getQueryEvents(String queryId, QueryEvent.QueryEventType eventType, Long since, Long until, Integer limit);

}
