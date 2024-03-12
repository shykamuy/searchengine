package searchengine.dto;

import lombok.Data;
import searchengine.model.Page;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.List;


@Data
public class SiteDto {

    private int id;
    private Status status;
    private LocalDateTime statusTime;
    private String url;
    private String name;
    private List<PageDto> pageList;

}
