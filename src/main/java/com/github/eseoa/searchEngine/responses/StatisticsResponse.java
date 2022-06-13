package com.github.eseoa.searchEngine.responses;

import com.github.eseoa.searchEngine.responses.statistics.Statistics;
import lombok.Data;
import org.hibernate.Session;

@Data
public final class StatisticsResponse {
    private boolean result;
    private Statistics statistics;

    public StatisticsResponse(Session session, Boolean result) {
        this.result = result;
        statistics = new Statistics(session);
    }

}