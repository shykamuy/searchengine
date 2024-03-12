package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.Lemma2PageDto;
import searchengine.model.Lemma2Page;

@Service
public class Lemma2PageService {
    public static Lemma2Page mapToLemma2PageEntity(Lemma2PageDto l2pDto) {
        Lemma2Page l2p = new Lemma2Page();
        l2p.setId(l2pDto.getId());
        l2p.setQuantity(l2pDto.getQuantity());
        return l2p;
    }
}
