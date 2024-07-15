package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.aop.AopInvocationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.LemmaDto;
import searchengine.dto.PageDto;
import searchengine.dto.search.SearchDto;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchedPage;
import searchengine.model.Lemma;
import searchengine.repositories.Lemma2PageRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private Lemma2PageRepository l2pRepository;

    public SearchDto createLemmasFromRequest(String request) throws IOException {
        SearchDto searchResponse = new SearchDto();
        String regex = "[^а-яА-Я\s]";
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();

        String text = request.replaceAll(regex, "").replaceAll(" +", " ").toLowerCase().trim();

        List.of(text.split("\s")).forEach(word -> {
            List<String> wordInfo = List.of(luceneMorphology.getMorphInfo(word).get(0).split("\s"));
            if (!(wordInfo.get(1).equals("МЕЖД") ||
                    wordInfo.get(1).equals("СОЮЗ") ||
                    wordInfo.get(1).equals("ПРЕДЛ") ||
                    wordInfo.get(1).equals("ЧАСТ") ||
                    wordInfo.get(1).contains("МС"))) {
                word = luceneMorphology.getNormalForms(word).get(0);
                lemmaMap.put(word, 0);
            }
        });
        searchResponse.setLemmaMap(lemmaMap);
        return searchResponse;
    }

    public List<LemmaDto> findLemmasInDB(SearchDto dto) {
        int countPages;
        List<LemmaDto> lemmaDtoList = new ArrayList<>();
        countPages = pageRepository.countPagesBySiteId(dto.getSiteId());
        for (Map.Entry<String, Integer> entry : dto.getLemmaMap().entrySet()) {
            Long frequency;
            List<Lemma> lemmaList = lemmaRepository.findLemmaByWord(entry.getKey());
            for (int i = 0; i < lemmaList.size(); i++) {
                if (lemmaList.get(i) == null) {
                    continue;
                }
                frequency = lemmaList.get(i).getFrequency();
                countPages = pageRepository.countPagesBySiteId(lemmaList.get(i).getSiteId().getId());
                if ((countPages / frequency) > 10) {
                    LemmaDto lemmaDto = new LemmaDto();
                    lemmaDto.setId(lemmaList.get(i).getId());
                    lemmaDto.setFrequency(lemmaList.get(i).getFrequency());
                    lemmaDto.setLemma(lemmaList.get(i).getLemma());
                    lemmaDto.setSiteId(lemmaList.get(i).getSiteId().getId());
                    if (dto.getSite() != null) {
                        if (lemmaDto.getSiteId() == siteRepository.siteIdByUrl(dto.getSite())) {
                            lemmaDtoList.add(lemmaDto);
                        }
                    } else {
                        lemmaDtoList.add(lemmaDto);
                    }
                }
            }
        }
        lemmaDtoList.sort(Comparator.comparingLong(LemmaDto::getFrequency));
        dto.setLemmaDtoList(lemmaDtoList);
        return dto.getLemmaDtoList();
    }

    public List<PageDto> findPagesByLemmas(SearchDto dto) {

        List<PageDto> pageDtoList = new ArrayList<>();

        dto.getLemmaDtoList().forEach(lemmaDto -> {
            pageDtoList.addAll(pageRepository.getPageListByLemmaId(lemmaDto.getId().intValue()).stream().map(PageService::mapToPageDto).toList());
        });
        return pageDtoList;
    }

    private float absRelevance;
    private float maxAbsRelevance;

    public List<PageDto> countRelevance(SearchDto dto) {
        maxAbsRelevance = 0;
        dto.getPageDtoList().forEach(pageDto -> {
            absRelevance = 0;
            dto.getLemmaDtoList().forEach(lemmaDto -> {
                try {
                    absRelevance += l2pRepository.getRank(pageDto.getId(), lemmaDto.getId());
                } catch (AopInvocationException e) {
                }
            });
            pageDto.setAbsRelevance(absRelevance);
            if (absRelevance > maxAbsRelevance) maxAbsRelevance = absRelevance;
        });
        dto.getPageDtoList().forEach(pageDto -> {
            pageDto.setRelevance(pageDto.getAbsRelevance() / maxAbsRelevance);
        });
        dto.setPageDtoList(dto.getPageDtoList().stream().distinct().filter(pageDto -> pageDto.getRelevance() > 0.09).collect(Collectors.toList()));
        dto.getPageDtoList().sort(Comparator.comparingDouble(PageDto::getRelevance).reversed());
        dto.getPageDtoList().forEach(pageDto -> {
        });
        return dto.getPageDtoList();
    }

    String snippet;

    public SearchResponse createSearchResponse(SearchDto dto) {
        SearchResponse searchResponse = new SearchResponse();
        List<SearchedPage> data = new ArrayList<>();
        dto.getPageDtoList().forEach(pageDto -> {
            SearchedPage searchedPage = new SearchedPage();
            searchedPage.setSite(pageDto.getPath().split("/[^w/]")[0]);
            searchedPage.setSiteName(siteRepository.findById(pageDto.getSiteId().intValue()).get().getName());
            URL url = null;
            try {
                url = new URL(pageDto.getPath());
                searchedPage.setUri(url.toURI().getPath());
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
            Document document = Jsoup.parse(pageDto.getContent());
            Element element = document.select("title").get(0);
            searchedPage.setTitle(element.text());
            searchedPage.setSnippet(createSnippet(dto, document));
            searchedPage.setRelevance(pageDto.getRelevance());
            data.add(searchedPage);
        });
        searchResponse.setResult(true);
        searchResponse.setData(data);
        searchResponse.setCount(data.size());
        return searchResponse;
    }


    private String createSnippet(SearchDto dto, Document document) {
        String snippet = null;
        String highlightedQuery = "<b>" + dto.getQuery() + "</b>";
        List<String> queryWordList = List.of(dto.getQuery().split("\s"));
        String text = document.text();
        int startIdx = document.text().indexOf(dto.getQuery());
        int endIdx = document.text().indexOf(dto.getQuery()) + dto.getQuery().length();
        if (text.indexOf(dto.getQuery()) > 0) {
            try {
                snippet = text.substring(
                        text.lastIndexOf('.', startIdx) + 2,
                        text.indexOf('.', endIdx + 1) + 2
                ).trim();
            } catch (StringIndexOutOfBoundsException e) {
                snippet = text.substring(0, 100) + "...";
            }
            if (snippet.length() > 300) {
                List<String> snippetWords = List.of(snippet.split("\s"));
                List<String> queryWords = List.of(dto.getQuery().split("\s"));
                StringBuilder newSnippet = new StringBuilder();
                if (0 > snippetWords.indexOf(queryWords.get(0)) - 10
                        && snippetWords.size() < snippetWords.indexOf(queryWords.get(queryWords.size() - 1)) + 10
                ) {
                    for (int i = 0; i < snippetWords.size() - 1; i++) {
                        newSnippet.append(snippetWords.get(i)).append(" ");
                    }
                    newSnippet.append("...");
                } else if (snippetWords.size() < snippetWords.indexOf(queryWords.get(queryWords.size() - 1)) + 10) {
                    for (int i = snippetWords.indexOf(queryWords.get(0)) - 10; i < snippetWords.size() - 1; i++) {
                        newSnippet.append(snippetWords.get(i)).append(" ");
                    }
                    newSnippet.append("...");
                } else if (0 > snippetWords.indexOf(queryWords.get(0)) - 10) {
                    for (int i = 0; i < snippetWords.indexOf(queryWords.get(queryWords.size() - 1)); i++) {
                        newSnippet.append(snippetWords.get(i)).append(" ");
                    }
                } else {
                    for (int i = snippetWords.indexOf(queryWords.get(0)) - 10; i < snippetWords.indexOf(queryWords.get(queryWords.size() - 1)) + 10; i++) {
                        newSnippet.append(snippetWords.get(i)).append(" ");
                    }
                    newSnippet.append("...");
                }
                snippet = newSnippet.toString().trim();
            }
            snippet = snippet.replace(dto.getQuery(), highlightedQuery).trim();
        } else {
            String bodyText = document.body().text();
            snippet = bodyText.substring(0, 200) + "...";
            ;
        }
        return snippet;
    }
}
