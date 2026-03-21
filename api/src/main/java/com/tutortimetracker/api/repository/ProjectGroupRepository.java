package com.tutortimetracker.api.repository;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ProjectGroupEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence operations for project groups. */
public interface ProjectGroupRepository extends JpaRepository<ProjectGroupEntity, Long> {

  /**
   * @param project owning project
   * @return groups in project
   */
  List<ProjectGroupEntity> findByProject(ProjectEntity project);

  /**
   * @param project owning project
   * @param name group name
   * @return group when existing
   */
  Optional<ProjectGroupEntity> findByProjectAndName(ProjectEntity project, String name);
}
