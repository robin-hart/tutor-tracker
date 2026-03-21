package com.tutortimetracker.api.repository;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for students.
 */
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    /**
     * @param project owning project
     * @return students for project
     */
    List<StudentEntity> findByProject(ProjectEntity project);

    /**
     * @param project owning project
     * @param groupName target group name
     * @return students assigned to a specific group
     */
    List<StudentEntity> findByProjectAndGroupName(ProjectEntity project, String groupName);

    /**
     * @param studentKey stable student key
     * @return matching student or empty
     */
    Optional<StudentEntity> findByStudentKey(String studentKey);
}
