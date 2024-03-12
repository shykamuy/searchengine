package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "SELECT count(*) FROM search_engine.page where site_id = ?1", nativeQuery = true)
    int countIndexedPages(int id);
    @Query(value = "select * from search_engine.page where path = ?1", nativeQuery = true)
    int getPageIdByUrl(String url);
    @Query(value = "select count(*) from search_engine.page", nativeQuery = true)
    int countPages();
    @Query(value = "select count(*) from search_engine.page where site_id = ?1", nativeQuery = true)
    int countPagesBySiteId(int siteId);

    @Query(value = "select * from search_engine.page where path = ?1", nativeQuery = true)
    Page getPageByUrl(String url);

    @Transactional
    @Modifying
    @Query(value = "delete from search_engine.page where id = ?1", nativeQuery = true)
    void deletePage(int id);

    @Query(value = "select * from search_engine.page where id in (select page_id from search_engine.l2p where lemma_id = ?1)", nativeQuery = true)
    List<Page> getPageListByLemmaId(int lemmaId);

}
