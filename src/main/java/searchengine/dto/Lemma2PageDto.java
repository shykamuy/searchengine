package searchengine.dto;

import lombok.Data;

@Data
public class Lemma2PageDto {
    private int id;
    private int pageId;
    private int lemmaId;
    private float quantity;
}
