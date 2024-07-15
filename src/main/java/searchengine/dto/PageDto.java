package searchengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageDto {

    private Long id;
    private Long siteId;
    private String path;
    private Long code;
    private String content;
    private int depth;
    private Float relevance;
    private Float absRelevance;
    private int counter = 0;
}
