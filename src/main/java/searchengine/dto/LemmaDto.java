package searchengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class LemmaDto implements Comparable<LemmaDto> {

    private int id;
    private int siteId;
    private String lemma;
    private int frequency;

    @Override
    public int compareTo(LemmaDto lemmaDto) {
        return lemmaDto.frequency - this.frequency;
    }
}
