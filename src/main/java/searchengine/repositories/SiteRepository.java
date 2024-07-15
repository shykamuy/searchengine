package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {


    @Query(value = "SELECT * FROM search_engine.site WHERE site.status = ?1", nativeQuery = true)
    List<Site> unIndexedUrlList(String status);

    @Query(value = "select id from search_engine.site where url = ?1", nativeQuery = true)
    Long siteIdByUrl(String url);
}
