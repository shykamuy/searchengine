package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.LemmaDto;
import searchengine.model.Lemma;

@Service
public class LemmaService {

    public static Lemma mapToLemmaEntity (LemmaDto lemmaDto) {
        Lemma lemma = new Lemma();
        lemma.setId(lemmaDto.getId());
        lemma.setFrequency(lemmaDto.getFrequency());
        lemma.setId(lemmaDto.getId());
        return lemma;
    }

}
