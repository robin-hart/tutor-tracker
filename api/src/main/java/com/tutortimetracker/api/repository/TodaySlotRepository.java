package com.tutortimetracker.api.repository;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.TodaySlotEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence operations for today's slot cards. */
public interface TodaySlotRepository extends JpaRepository<TodaySlotEntity, Long> {

  /**
   * @param project owning project
   * @return slots for project
   */
  List<TodaySlotEntity> findByProject(ProjectEntity project);
}
