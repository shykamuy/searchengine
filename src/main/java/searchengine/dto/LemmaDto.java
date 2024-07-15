package searchengine.dto;

import lombok.Data;

@Data
public class LemmaDto implements Comparable<LemmaDto> {

    private Long id;
    private Long siteId;
    private String lemma;
    private Long frequency;

    @Override
    public int compareTo(LemmaDto lemmaDto) {
        return Long.valueOf(lemmaDto.frequency - this.frequency).intValue();
    }
}
