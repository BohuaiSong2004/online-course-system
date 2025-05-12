package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Catalog queries. Results are held in MyBatis' second-level (namespace) cache, which is
 * flushed automatically by any insert/update/delete in this namespace or in ChapterMapper
 * (which shares it via cache-ref).
 */
@Mapper
public interface CourseMapper {

    List<Course> searchPublished(@Param("q") String q, @Param("category") String category,
                                 @Param("level") String level, @Param("offset") int offset,
                                 @Param("limit") int limit);

    long countPublished(@Param("q") String q, @Param("category") String category, @Param("level") String level);

    List<String> listPublishedCategories();

    List<Course> listPublishedForSitemap();

    Course findById(@Param("id") long id);

    Course findBySlug(@Param("slug") String slug);

    List<Course> findByInstructor(@Param("instructorId") long instructorId);

    List<Course> findAllForAdmin();

    boolean slugExists(@Param("slug") String slug);

    int insert(Course course);

    int update(Course course);

    int updateStatus(@Param("id") long id, @Param("status") String status);

    int delete(@Param("id") long id);
}
