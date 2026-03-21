package com.tutortimetracker.api.repository;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for timeslots.
 */
public interface TimeslotRepository extends JpaRepository<TimeslotEntity, String> {

	/**
	 * @param project owning project
	 * @param from start date inclusive
	 * @param to end date exclusive
	 * @return timeslots in date range
	 */
	List<TimeslotEntity> findByProjectAndDateGreaterThanEqualAndDateLessThan(ProjectEntity project, LocalDate from, LocalDate to);

	/**
	 * @param project owning project
	 * @param date date filter
	 * @return slots for date
	 */
	List<TimeslotEntity> findByProjectAndDate(ProjectEntity project, LocalDate date);

	/**
	 * @param project owning project
	 * @param id timeslot id
	 * @return matching timeslot or empty
	 */
	Optional<TimeslotEntity> findByProjectAndId(ProjectEntity project, String id);

	/**
	 * @param project owning project
	 * @return all timeslots for project
	 */
	List<TimeslotEntity> findByProject(ProjectEntity project);
}
