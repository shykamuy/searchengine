package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import searchengine.dto.PageDto;


import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.Lemma2PageRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;


@Component
public class PageIndexService extends RecursiveTask<PageDto> {



    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private PageService pageService = new PageService();
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private Lemma2PageRepository l2pRepository;
    private PageDto currentPage;
    private List<PageDto> list;
    private static final int MAX_DEPTH = 3;

    public PageIndexService(List<PageDto> list, SiteRepository siteRepository, PageRepository pageRepository,
                            LemmaRepository lemmaRepository, Lemma2PageRepository l2pRepository) {
        this.list = list;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.l2pRepository = l2pRepository;
    }

    @Override
    protected PageDto compute() {
        List<PageIndexService> pageIndexServiceList = new ArrayList<>();
        currentPage = list.get(0);

        Document document = pageService.connectDocument(currentPage);

        currentPage.setCode(200);
        currentPage.setContent(document.toString());
        currentPage.setSiteId(currentPage.getSiteId());


        try {
            HashMap<String, Integer> lemmaMap = createLemmas(currentPage);
            pageService.update(currentPage, pageRepository, siteRepository);
            pageService.setLemmasAndIndexes(currentPage, siteRepository, pageRepository, lemmaRepository, l2pRepository, lemmaMap);
        } catch (Exception e) {
            return null;
        }

        Elements links = document.select("a[href]");

        forkToChildPages(links, pageIndexServiceList);

        pageIndexServiceList.forEach(ForkJoinTask::join);
        if (currentPage.getDepth() == 0) {
            Site indexedSite = siteRepository.findById(currentPage.getSiteId()).orElseThrow();
            indexedSite.setStatus(Status.INDEXED);
            indexedSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(indexedSite);
        }
        return null;
    }

    private void forkToChildPages(Elements links, List<PageIndexService> pageIndexServiceList) {
        links.forEach(link -> {
            String absLink = link.absUrl("href");
            if (!absLink.contains("https://www.")) {
                absLink = absLink.replace("https://", "https://www.");
            }
            if (currentPage.getPath().contains("http:")) {
                absLink = absLink.replace("https:", "http:");
            }
            if (absLink.contains(currentPage.getPath())
                    && !absLink.contains("/#") && !absLink.contains("/?")) {
                if (absLink.charAt(absLink.length() - 1) != '/')
                    absLink += "/";
                List<PageDto> childPageDtoList = new ArrayList<>();
                PageDto newPageDto = new PageDto();
                newPageDto.setPath(absLink);
                newPageDto.setSiteId(currentPage.getSiteId());
                newPageDto.setDepth(currentPage.getDepth() + 1);
                childPageDtoList.add(newPageDto);
                PageIndexService pageIndexService = new PageIndexService(childPageDtoList, siteRepository,
                        pageRepository, lemmaRepository, l2pRepository);
                pageIndexService.fork();
                pageIndexServiceList.add(pageIndexService);

            }
        });

    }

    private HashMap<String, Integer> createLemmas(PageDto currentPage) throws IOException {
        String text = currentPage.getContent();
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        String regex = "[^а-яА-Я\s]";
        text = text.replaceAll(regex, "").replaceAll(" +", " ").toLowerCase().trim();
        if (text.equals("")) {
            return null;
        }
        List<String> wordList = new ArrayList<>(List.of(text.split("\s")));
        wordList.forEach(word -> {
            List<String> wordInfo = List.of(luceneMorphology.getMorphInfo(word).get(0).split("\s"));
            if (!(wordInfo.get(1).equals("МЕЖД") || wordInfo.get(1).equals("СОЮЗ") ||
                    wordInfo.get(1).equals("ПРЕДЛ") || wordInfo.get(1).equals("ЧАСТ") || wordInfo.get(1).contains("МС"))) {
                word = luceneMorphology.getNormalForms(word).get(0);
                if(lemmaMap.containsKey(word)) {
                    lemmaMap.put(word, lemmaMap.get(word) + 1);
                }else {
                    lemmaMap.put(word, 1);
                }
            }
        });
        return lemmaMap;
    }
}
