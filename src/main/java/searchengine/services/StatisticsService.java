package searchengine.services;

import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.StatisticsResponse;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    SitesListConfig getSitesList();
}
