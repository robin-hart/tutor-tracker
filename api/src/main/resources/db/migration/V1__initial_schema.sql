CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(80) NOT NULL,
    institution VARCHAR(200) NOT NULL,
    target_month_hours DOUBLE NOT NULL,
    total_hours DOUBLE NOT NULL,
    month_hours DOUBLE NOT NULL,
    completion_percent INT NOT NULL,
    created_at DATE,
    PRIMARY KEY (id),
    CONSTRAINT uk_projects_slug UNIQUE (slug)
);

CREATE TABLE project_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    project_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_groups_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_project_group_name UNIQUE (project_id, name)
);

CREATE TABLE students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_key VARCHAR(100) NOT NULL,
    name VARCHAR(160) NOT NULL,
    notes VARCHAR(1000) NOT NULL,
    group_name VARCHAR(100),
    project_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_student_key UNIQUE (student_key),
    CONSTRAINT fk_students_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE timeslots (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    duration_minutes INT NOT NULL,
    date DATE NOT NULL,
    start_time TIME,
    project_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_timeslots_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    month VARCHAR(80) NOT NULL,
    month_key VARCHAR(7),
    project_name VARCHAR(200) NOT NULL,
    project_id BIGINT,
    total_hours DOUBLE NOT NULL,
    sessions INT NOT NULL,
    gross_amount DOUBLE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reports_project_month UNIQUE (project_id, month_key),
    CONSTRAINT fk_reports_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE today_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    time VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    project_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_today_slots_project FOREIGN KEY (project_id) REFERENCES projects (id)
);
