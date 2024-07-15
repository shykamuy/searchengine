package searchengine.dto;

import lombok.Data;

@Data
public class Lemma2PageDto {
    private Long id;
    private Long pageId;
    private Long lemmaId;
    private Float quantity;
}
