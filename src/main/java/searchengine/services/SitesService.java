package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.PageDto;
import searchengine.dto.Response;
import searchengine.dto.SiteDto;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.Lemma2PageRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.repositories.PageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Service
public class SitesService implements CRUDService<SiteDto> {


    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private Lemma2PageRepository l2pRepository;

    @Autowired
    private PageService pageService;

    private ForkJoinPool forkJoinPool = new ForkJoinPool();

    private SitesListConfig sitesList;
    private boolean isIndexing = false;
    private boolean isWorking = false;

    public SitesService(SitesListConfig sitesList) {
        this.sitesList = sitesList;
    }


    public void addSites(SitesListConfig sitesList) {
        System.out.println(siteRepository.count());
        for (SiteConfig site : sitesList.getSites()) {
            try {
                Site modelSite = new Site();
                modelSite.setUrl(site.getUrl());
                modelSite.setName(site.getName());
                Page page = new Page();
                page.setPath(site.getUrl());
                page.setCode(300L);
                page.setContent("UNINDEXED");
                page.setSiteId(modelSite);
                List<Page> pageList = new ArrayList<>();
                pageList.add(page);
                modelSite.setPageList(pageList);
                siteRepository.save(modelSite);
            } catch (Exception e) {
                //Лог добавился имеющийся сайт
                System.out.println(e.getMessage());
            }
        }
    }


    public Response stopIndexing() {
        Response response = new Response();
        if (forkJoinPool.getActiveThreadCount() == 0) {
            response.setError("Индексация не запущена");
            return response;
        }
        forkJoinPool.shutdown();
        forkJoinPool.shutdownNow();
        while (forkJoinPool.isTerminating()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Потоки остановлены");
        List<Site> unCompletedIndexingSiteList = siteRepository.unIndexedUrlList(Status.INDEXING.toString());
        unCompletedIndexingSiteList.forEach(site -> {
            site.setStatus(Status.FAILED);
            site.setLastError("Indexing stopped by user");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });
        response.setResult(true);
        return response;
    }

    public Long getSiteIdByUrl(String url) {
        url = url.split("/[^w/]")[0] + "/";
        if (siteRepository.siteIdByUrl(url) != 0) {
            return siteRepository.siteIdByUrl(url);
        }
        return 0L;
    }


    public List<Site> getUnIndexedSites() {
        System.out.println(Status.INDEXING.toString());
        List<Site> unIndexedSiteList = siteRepository.unIndexedUrlList(Status.INDEXING.toString());
        return unIndexedSiteList;
    }

    public boolean siteStatus(SitesListConfig sitesList) {
        if (siteRepository.count() != 0) {
            Iterable<Site> sites = siteRepository.findAll();

            sites.forEach(site -> {
                System.out.println(site.getPageList().size());
                if (site.getStatus().equals(Status.INDEXING)) {
                    isIndexing = true;
                }
            });
            return isIndexing;
        }
        return false;
    }

    public Response indexSites(List<Site> unIndexedSiteList) {
        Response response = new Response();
        response.setError("Индексация уже идет");
        unIndexedSiteList.forEach(site -> {
            if (pageRepository.countIndexedPages(site.getId()) == 1) {
                List<PageDto> pageDtoList = new ArrayList<>();
                site.getPageList().forEach(page -> {
                    pageDtoList.add(PageService.mapToPageDto(page));
                });
                forkJoinPool.submit(new PageIndexService(pageDtoList, siteRepository, pageRepository, lemmaRepository, l2pRepository, pageService));
                response.setResult(true);
                response.setError(null);
            }
        });
        return response;
    }


    public static SiteDto mapToSiteDto(Site site) {
        SiteDto dto = new SiteDto();
        dto.setId(site.getId());
        dto.setName(site.getName());
        dto.setUrl(site.getUrl());
        dto.setStatus(site.getStatus());
        dto.setStatusTime(site.getStatusTime());
        dto.setPageList(site.getPageList().stream().map(PageService::mapToPageDto).toList());
        return dto;

    }

    public static Site mapToEntity(SiteDto siteDto) {
        Site site = new Site();
        site.setId(siteDto.getId());
        site.setName(siteDto.getName());
        site.setUrl(siteDto.getUrl());
        site.setStatus(siteDto.getStatus());
        site.setStatusTime(siteDto.getStatusTime());
        site.setPageList(siteDto.getPageList().stream().map(PageService::mapToPageEntity).toList());
        return site;
    }

    @Override
    public List<SiteDto> getAll() {
        return null;
    }

    @Override
    public Site get(int id) {
        return siteRepository.findById(id).orElse(null);
    }

    @Override
    public Response delete(int id) {
        Response response = new Response();
        Optional<Site> optionalSite = siteRepository.findById(id);
        if (optionalSite.isPresent()) {
            siteRepository.deleteById(id);
            response.setResult(true);
            return response;
        }
        response.setError("Указанная страница не найдена");
        return response;
    }

    @Override
    public void create(SiteDto obj) {

    }

    @Override
    public void update(SiteDto obj) {

    }

}
