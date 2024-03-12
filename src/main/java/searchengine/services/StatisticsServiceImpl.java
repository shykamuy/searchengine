package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.Lemma2PageRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesListConfig sites;

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private Lemma2PageRepository l2pRepository;




    @Override
    public SitesListConfig getSitesList() {
        return sites;
    }

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        List<Site> siteList = siteRepository.findAll();
        total.setSites(siteList.size());
        total.setPages(pageRepository.countPages());
        total.setLemmas(lemmaRepository.countLemmas());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        siteList.forEach(site -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setPages(pageRepository.countPagesBySiteId(site.getId()));
            item.setLemmas(lemmaRepository.countLemmasBySiteId(site.getId()));
            item.setStatus(site.getStatus().toString());
                item.setError(site.getLastError());
            ZonedDateTime zdt = ZonedDateTime.of(site.getStatusTime(), ZoneId.systemDefault());
            item.setStatusTime(zdt.toInstant().toEpochMilli());
            detailed.add(item);
        });
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }


}
