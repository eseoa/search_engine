package main.responses;

import lombok.Data;
import main.responses.statistics.Statistics;

@Data
public final class StatisticsResponse {
    private boolean result;
    private Statistics statistics;

    public StatisticsResponse() {
        result = true;
        statistics = new Statistics();
    }

}
