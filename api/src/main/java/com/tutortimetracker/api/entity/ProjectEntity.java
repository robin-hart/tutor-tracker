package com.tutortimetracker.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Project entity persisted in MariaDB. */
@Entity
@Table(name = "projects")
public class ProjectEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String slug;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 80)
  private String category;

  @Column(nullable = false)
  private double totalHours;

  @Column(nullable = false)
  private double monthHours;

  @Column(nullable = false)
  private int completionPercent;

  @Column(name = "created_at")
  private LocalDate createdAt;

  public Long getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public double getTotalHours() {
    return totalHours;
  }

  public void setTotalHours(double totalHours) {
    this.totalHours = totalHours;
  }

  public double getMonthHours() {
    return monthHours;
  }

  public void setMonthHours(double monthHours) {
    this.monthHours = monthHours;
  }

  public int getCompletionPercent() {
    return completionPercent;
  }

  public void setCompletionPercent(int completionPercent) {
    this.completionPercent = completionPercent;
  }

  public LocalDate getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDate createdAt) {
    this.createdAt = createdAt;
  }

  @PrePersist
  void initializeCreatedAt() {
    if (createdAt == null) {
      createdAt = LocalDate.now();
    }
  }
}
