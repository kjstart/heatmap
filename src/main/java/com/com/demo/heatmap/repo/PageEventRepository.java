package com.com.demo.heatmap.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.com.demo.heatmap.entity.PageEvent;

@Repository
public interface PageEventRepository extends JpaRepository<PageEvent, Long> {
	
	PageEvent save(PageEvent pageEvent);

	List<PageEvent> findByUrlHashAndEventType(String urlHash, String eventType);

	@Query(nativeQuery = true, value = "select page_section, sum(stay_time) stay_time from page_events where URL_HASH=? and event_type='position' group by page_section order by page_section")
	List<Object[]> findStayTimeByUrlHashAndEventTypeGroupByPosition(String urlHash);

	@Query(nativeQuery = true, value = "select cursor_x, cursor_y, count(*) sum from page_events where url_hash=? and event_type='click' group by cursor_x, cursor_y")
	List<Object[]> findByUrlHashGroupByCursor(String urlHash);
}