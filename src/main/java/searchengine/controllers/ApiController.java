package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.PageDto;
import searchengine.dto.Response;
import searchengine.dto.search.SearchDto;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.repositories.Lemma2PageRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.PageService;
import searchengine.services.SearchService;
import searchengine.services.SitesService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;

    @Autowired
    private SitesService service;
    @Autowired
    private PageService pageService;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private Lemma2PageRepository l2pRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SearchService searchService;


    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deleteSite(@PathVariable int id) {
        Response response = service.delete(id);
        return response.getError() == null ?
                new ResponseEntity(response, HttpStatus.OK) :
                new ResponseEntity(response, HttpStatus.NOT_FOUND);
    }



    @GetMapping
    public ResponseEntity AddSites() {
        if (statisticsService.getSitesList().getSites().size() == 0) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        service.addSites(statisticsService.getSitesList());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        Response response = service.stopIndexing();
        return response.getError() == null ?
                new ResponseEntity(response, HttpStatus.OK) :
                new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }



    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing(){
        Response response = new Response();
        if (!service.siteStatus(statisticsService.getSitesList())) {
            response.setError("В конфигурационном файле не задан список сайтов");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }
        List<Site> siteList = service.getUnIndexedSites();
        response = service.indexSites(siteList);
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(String url) throws IOException {
        Response response = new Response();
        PageDto pageDto = new PageDto();
        pageDto.setPath(url);
        pageDto.setSiteId(service.getSiteIdByUrl(url));
        if (pageDto.getSiteId() == 0) {
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }
        response.setResult(true);
        pageDto.setCode(300);
        pageDto.setContent("UNINDEXED");
        pageService.deletePageIfExist(pageDto, pageRepository, lemmaRepository, l2pRepository);
        pageService.createLemmas(pageDto, siteRepository, pageRepository, lemmaRepository, l2pRepository);
        return new ResponseEntity(response, HttpStatus.CREATED);
    }

    @GetMapping("/search{query}{offset}{limit}{site}")
    public ResponseEntity search(@RequestParam String query, @RequestParam int offset, @RequestParam int limit, @RequestParam(required = false) String site) throws IOException {
        if (query.equals("")) {
            SearchResponse sr = new SearchResponse();
            sr.setError("Пустая строка");
            return new ResponseEntity(sr, HttpStatus.BAD_REQUEST);
        }
        SearchDto dto = searchService.createLemmasFromRequest(query);
        dto.setQuery(query);
        dto.setSite(site);
        dto.setOffset(offset);
        dto.setLimit(limit);
        dto.setLemmaDtoList(searchService.findLemmasInDB(dto));
        System.out.println(dto.getLemmaDtoList());
        dto.setPageDtoList(searchService.findPagesByLemmas(dto));
        dto.setPageDtoList(searchService.countRelevance(dto));
        return new ResponseEntity(searchService.createSearchResponse(dto), HttpStatus.OK);
    }
}
