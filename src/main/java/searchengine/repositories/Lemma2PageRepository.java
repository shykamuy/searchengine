package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma2Page;

import java.util.List;

@Repository
public interface Lemma2PageRepository extends JpaRepository<Lemma2Page, Integer> {

    @Transactional
    @Modifying
    @Query(value =
            "insert into search_engine.l2p (quantity, lemma_id, page_id) values " +
            "(:quantity, (select id from search_engine.lemma where lemma = :lemma and site_id = :siteId), :pageId)",
            nativeQuery = true)
    void insertL2P(@Param("quantity")float quantity, @Param("lemma") String lemma, @Param("siteId") int siteId, @Param("pageId") int pageId);

    @Query(value = "select * from search_engine.l2p where page_id = ?1", nativeQuery = true)
    List<Lemma2Page> getL2PByPageId(int pageId);

    @Query(value = "select quantity from search_engine.l2p where page_id = :pageId and lemma_id = :lemmaId", nativeQuery = true)
    float getRank(@Param("pageId") int pageId, @Param("lemmaId") int lemmaId);

}
