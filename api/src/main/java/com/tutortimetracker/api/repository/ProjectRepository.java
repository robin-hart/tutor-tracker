package com.tutortimetracker.api.repository;

import com.tutortimetracker.api.entity.ProjectEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence operations for projects. */
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

  /**
   * @param slug stable project slug
   * @return matching project or empty
   */
  Optional<ProjectEntity> findBySlug(String slug);
}
