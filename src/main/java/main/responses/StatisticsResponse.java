package main.responses;

import lombok.Data;
import main.responses.statistics.Statistics;
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
