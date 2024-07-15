package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.PageDto;
import searchengine.dto.Response;
import searchengine.model.Lemma2Page;
import searchengine.model.Page;
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
import java.util.Map;
import java.util.Optional;

@Service
public class PageService implements CRUDService<PageDto> {

    @Autowired
    private static PageRepository pageRepository;
    @Autowired
    private static SiteRepository siteRepository;
    @Autowired
    private static LemmaRepository lemmaRepository;
    @Autowired
    private static Lemma2PageRepository lemma2PageRepository;


    public static PageDto mapToPageDto(Page page) {
        PageDto dto = new PageDto();
        dto.setId(page.getId());
        dto.setCode(page.getCode());
        dto.setPath(page.getPath());
        dto.setSiteId(page.getSiteId().getId());
        dto.setContent(page.getContent());
        return dto;

    }


    public static Page mapToPageEntity(PageDto dto) {
        Page page = new Page();
        page.setId(dto.getId());
        page.setCode(dto.getCode());
        page.setPath(dto.getPath());
        page.setContent(dto.getContent());
        return page;
    }


    @Override
    public List getAll() {
        return pageRepository.findAll().stream().map(PageService::mapToPageDto).toList();
    }

    @Override
    public Object get(int id) {
        return pageRepository.findById(id);
    }

    @Override
    public Response delete(int id) {
        Response response = new Response();
        Optional<Page> optionalPage = pageRepository.findById(id);
        if (optionalPage.isPresent()) {
            pageRepository.deleteById(id);
            return response;
        }
        return response;
    }

    @Override
    public void create(PageDto pageDto) {
        Page page = mapToPageEntity(pageDto);
        Long siteId = pageDto.getSiteId();
        Site site = siteRepository.findById(siteId.intValue()).orElseThrow();
        page.setSiteId(site);
        pageRepository.save(page);
    }

    @Override
    public void update(PageDto pageDto) {
        Page page = mapToPageEntity(pageDto);
        Long siteId = pageDto.getSiteId();
        Site site = siteRepository.findById(siteId.intValue()).orElseThrow();
        page.setSiteId(site);
        pageRepository.save(page);
    }

    public void update(PageDto currentPage, PageRepository pageRepository, SiteRepository siteRepository) {
        Page page = mapToPageEntity(currentPage);
        Long siteId = currentPage.getSiteId();
        Site site = siteRepository.findById(siteId.intValue()).orElseThrow();
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        page.setSiteId(site);
        pageRepository.save(page);
        siteRepository.save(site);
    }

    public Document connectDocument(PageDto currentPage) {
        Document document;
        try {
            Thread.sleep(500);
            document = Jsoup.connect(currentPage.getPath()).ignoreHttpErrors(true).get();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    public void setLemmasAndIndexes(PageDto currentPage, SiteRepository siteRepository, PageRepository pageRepository,
                                    LemmaRepository lemmaRepository, Lemma2PageRepository l2pRepository, HashMap<String, Integer> lemmaMap) {

        Long pageId = pageRepository.getPageIdByUrl(currentPage.getPath());
        Page page = pageRepository.findById(pageId.intValue()).orElseThrow();
        Long siteId = page.getSiteId().getId();
        Site site = siteRepository.findById(siteId.intValue()).orElseThrow();

        for (Map.Entry<String, Integer> entry : lemmaMap.entrySet()) {
            lemmaRepository.insertOrUpdateLemma(1, entry.getKey(), site.getId());
            l2pRepository.insertL2P(entry.getValue(), entry.getKey(), site.getId(), page.getId());
        }
    }

    Page page = new Page();

    public void deletePageIfExist(PageDto currentPage, PageRepository pageRepository, LemmaRepository lemmaRepository, Lemma2PageRepository l2pRepository) {
        try {
            Page page1 = pageRepository.getPageByUrl(currentPage.getPath());
            List<Lemma2Page> l2pList = l2pRepository.getL2PByPageId(page1.getId());
            l2pList.forEach(l2p -> {
                if (l2p.getLemmaId().getFrequency() > 1) {
                    l2p.getLemmaId().setFrequency(l2p.getLemmaId().getFrequency() - 1);
                    l2pRepository.save(l2p);
                } else {
                    lemmaRepository.deleteById(l2p.getLemmaId().getId().intValue());
                }
            });
            pageRepository.deletePage(page1.getId());
        } catch (Exception e) {
        }
    }


    public void createLemmas(PageDto pageDto, SiteRepository siteRepository, PageRepository pageRepository,
                             LemmaRepository lemmaRepository, Lemma2PageRepository l2pRepository) throws IOException {
        Document document = connectDocument(pageDto);
        pageDto.setContent(document.toString());
        Page page = PageService.mapToPageEntity(pageDto);
        page.setSiteId(siteRepository.findById(pageDto.getSiteId().intValue()).orElseThrow());
        pageRepository.save(page);
        String text = document.text();
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        String regex = "[^а-яА-Я\s]";
        text = text.replaceAll(regex, "").replaceAll(" +", " ").toLowerCase().trim();
        if (text.equals("")) {
            return;
        }
        List<String> wordList = new ArrayList<>(List.of(text.split("\s")));
        wordList.forEach(word -> {
            List<String> wordInfo = List.of(luceneMorphology.getMorphInfo(word).get(0).split("\s"));
            if (!(wordInfo.get(1).equals("МЕЖД") || wordInfo.get(1).equals("СОЮЗ") ||
                    wordInfo.get(1).equals("ПРЕДЛ") || wordInfo.get(1).equals("ЧАСТ") || wordInfo.get(1).contains("МС"))) {
                word = luceneMorphology.getNormalForms(word).get(0);
                if (lemmaMap.containsKey(word)) {
                    lemmaMap.put(word, lemmaMap.get(word) + 1);
                } else {
                    lemmaMap.put(word, 1);
                }
            }
        });
        pageDto.setCode(200L);
        setLemmasAndIndexes(pageDto, siteRepository, pageRepository, lemmaRepository, l2pRepository, lemmaMap);
    }
}
