package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ViewScopeDaoImplTest {

	@Autowired
	ViewScopeDao viewScopeDao;
	@Autowired
	DBOBasicDao basicDao;
	
	@AfterEach
	public void after(){
		viewScopeDao.truncateAll();
	}
	
	@Test
	public void testSetViewScopeGetViewScoped(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		// find the intersection
		Set<Long> fetched = viewScopeDao.getViewScope(viewId1);
		assertEquals(containers, fetched);
		assertEquals(new Long(ViewTypeMask.File.getMask()), viewScopeDao.getViewTypeMask(viewId1));
	}
	
	
	@Test
	public void testSetViewTypeMaskNotFound(){
		long viewId1 = 123L;
		assertThrows(NotFoundException.class, ()->{
			// call under test
			viewScopeDao.getViewTypeMask(viewId1);
		});
	}
	
	@Test
	public void testSetViewScopeUpdateEtagChange(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		
		// check the value in the database.
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("viewId", viewId1);
		DBOViewType dboType = basicDao.getObjectByPrimaryKey(DBOViewType.class, param);
		assertNotNull(dboType);
		assertEquals(new Long(viewId1), dboType.getViewId());
		assertNotNull(dboType.getEtag());
		assertEquals(null, dboType.getViewType());
		assertEquals(new Long(ViewTypeMask.File.getMask()), dboType.getViewTypeMask());
		String startEtag = dboType.getEtag();
		
		// update one
		containers = Sets.newHashSet(444L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		// check the etag
		dboType = basicDao.getObjectByPrimaryKey(DBOViewType.class, param);
		assertNotNull(dboType.getEtag());
		assertNotEquals(startEtag, dboType.getEtag());
	}
	
	@Test
	public void testSetViewScopeAndFind(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		// find the intersection
		Set<Long> intersection = viewScopeDao.findViewScopeIntersectionWithPath(containers.stream().collect(Collectors.toList()));
		assertNotNull(intersection);
		assertEquals(1, intersection.size());
		assertTrue(intersection.contains(viewId1));		
	}
	
	@Test
	public void testViewScopeUpdate(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		// change the values
		containers = Sets.newHashSet(555L,777L);
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		// The 444 container should no longer intersect with view 123
		Set<Long> intersection = viewScopeDao.findViewScopeIntersectionWithPath(Lists.newArrayList(444L));
		assertNotNull(intersection);
		assertEquals(0, intersection.size());
		// view 123 should intersect with 555
		intersection = viewScopeDao.findViewScopeIntersectionWithPath(Lists.newArrayList(555L));
		assertNotNull(intersection);
		assertEquals(1, intersection.size());
		assertTrue(intersection.contains(viewId1));
		// view 123 should intersect with 777
		intersection = viewScopeDao.findViewScopeIntersectionWithPath(Lists.newArrayList(777L));
		assertNotNull(intersection);
		assertEquals(1, intersection.size());
		assertTrue(intersection.contains(viewId1));		
	}
	
	@Test
	public void testViewScopeMultipleOverlap(){
		long viewId1 = 123L;
		long viewId2 = 456L;
		// one
		viewScopeDao.setViewScopeAndType(viewId1, Sets.newHashSet(444L,555L), ViewTypeMask.File.getMask());
		// two
		viewScopeDao.setViewScopeAndType(viewId2, Sets.newHashSet(555L,888L), ViewTypeMask.File.getMask());
		// 555 should intersect with views 123 and 456
		Set<Long> results = viewScopeDao.findViewScopeIntersectionWithPath(Lists.newArrayList(555L));
		assertEquals(Sets.newHashSet(viewId1, viewId2), results);
		// 444 should intersect with view 123
		results = viewScopeDao.findViewScopeIntersectionWithPath(Lists.newArrayList(444L));
		assertEquals(Sets.newHashSet(viewId1), results);
		// 888 should intersect with view 456
		results = viewScopeDao.findViewScopeIntersectionWithPath(Lists.newArrayList(888L));
		assertEquals(Sets.newHashSet(viewId2), results);
		
	}
	
	@Test
	public void testSetViewScopeNull(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		// find the intersection
		Set<Long> intersection = viewScopeDao.findViewScopeIntersectionWithPath(containers.stream().collect(Collectors.toList()));
		assertNotNull(intersection);
		assertEquals(1, intersection.size());
		assertTrue(intersection.contains(viewId1));
		
		// set the scope null
		containers = null;
		viewScopeDao.setViewScopeAndType(viewId1, containers, ViewTypeMask.File.getMask());
		
		// No intersection should be found
		intersection = viewScopeDao.findViewScopeIntersectionWithPath(Lists.newArrayList(444L,555L));
		assertNotNull(intersection);
		assertEquals(0, intersection.size());
	}
}
