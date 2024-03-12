package searchengine.dto.search;

import lombok.Data;
import searchengine.dto.LemmaDto;
import searchengine.dto.PageDto;

import java.util.HashMap;
import java.util.List;

@Data
public class SearchDto {
    private String query;
    private int offset;
    private int limit;
    private String site;
    private HashMap<String, Integer> lemmaMap;
    private int siteId;
    private List<LemmaDto> lemmaDtoList;
    private List<PageDto> pageDtoList;

}
