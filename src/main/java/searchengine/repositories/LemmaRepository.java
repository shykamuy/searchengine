package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "SELECT * FROM search_engine.lemma where lemma = :word", nativeQuery = true)
    List<Lemma> findLemmaByWord(@Param("word") String word);
    @Query(value = "select count(*) from search_engine.lemma", nativeQuery = true)
    int countLemmas();
    @Query(value = "select count(*) from search_engine.lemma where site_id = ?1", nativeQuery = true)
    int countLemmasBySiteId(int id);

    @Transactional
    @Modifying
    @Query(value = "insert into search_engine.lemma (frequency, lemma, site_id) values (:frequency, :lemma, :siteId) " +
            "on duplicate key update frequency = frequency + 1", nativeQuery = true)
    int insertOrUpdateLemma(@Param("frequency") int frequency, @Param("lemma") String lemma, @Param("siteId") int siteId);

}
