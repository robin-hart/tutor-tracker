package com.tutortimetracker.api.repository;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ReportRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for report rows.
 */
public interface ReportRepository extends JpaRepository<ReportRowEntity, Long> {

	/**
	 * @param project owning project
	 * @return reports for selected project
	 */
	List<ReportRowEntity> findByProject(ProjectEntity project);

	/**
	 * @param project owning project
	 * @param monthKey month key in yyyy-MM format
	 * @return report for month if it exists
	 */
	Optional<ReportRowEntity> findByProjectAndMonthKey(ProjectEntity project, String monthKey);
}
