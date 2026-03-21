package com.tutortimetracker.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Persisted monthly report row. */
@Entity
@Table(
    name = "reports",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_reports_project_month",
          columnNames = {"project_id", "month_key"})
    })
public class ReportRowEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String month;

  @Column(name = "month_key", length = 7)
  private String monthKey;

  @Column(nullable = false, length = 200)
  private String projectName;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "project_id")
  private ProjectEntity project;

  @Column(nullable = false)
  private double totalHours;

  @Column(nullable = false)
  private int sessions;

  @Column(nullable = false)
  private double grossAmount;

  public Long getId() {
    return id;
  }

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getMonthKey() {
    return monthKey;
  }

  public void setMonthKey(String monthKey) {
    this.monthKey = monthKey;
  }

  public ProjectEntity getProject() {
    return project;
  }

  public void setProject(ProjectEntity project) {
    this.project = project;
  }

  public double getTotalHours() {
    return totalHours;
  }

  public void setTotalHours(double totalHours) {
    this.totalHours = totalHours;
  }

  public int getSessions() {
    return sessions;
  }

  public void setSessions(int sessions) {
    this.sessions = sessions;
  }

  public double getGrossAmount() {
    return grossAmount;
  }

  public void setGrossAmount(double grossAmount) {
    this.grossAmount = grossAmount;
  }
}
