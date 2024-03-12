package searchengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageDto {

    private int id;
    private int siteId;
    private String path;
    private int code;
    private String content;
    private int depth;
    private float relevance;
    private float absRelevance;
    private int counter = 0;
}
