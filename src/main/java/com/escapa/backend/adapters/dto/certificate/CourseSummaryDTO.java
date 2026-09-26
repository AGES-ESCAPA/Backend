package com.escapa.backend.adapters.dto.certificate;

import java.util.UUID;

public class CourseSummaryDTO {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private String level;
    private String thumbnailUrl;
    private Integer durationTime;
    private Integer lessonsCount;
    private double rating;
    private Integer reviewsCount;
    private String instructor;
    private double price;

    public CourseSummaryDTO(
            String courseId,
            String courseTitle,
            String courseDescription,
            String courseCategory,
            String courseLevel,
            String courseImageUrl,
            Integer courseDurationTime,
            Integer courseLessonsCount,
            double courseRating,
            Integer courseReviewsCount,
            String courseInstructorName,
            double coursePrice
    ) {
        this.id = courseId == null ? null : UUID.fromString(courseId);
        this.title = courseTitle;
        this.description = courseDescription;
        this.category = courseCategory;
        this.level = courseLevel;
        this.thumbnailUrl = courseImageUrl;
        this.durationTime = courseDurationTime;
        this.lessonsCount = courseLessonsCount;
        this.rating = courseRating;
        this.reviewsCount = courseReviewsCount;
        this.instructor = courseInstructorName;
        this.price = coursePrice;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(Integer durationTime) {
        this.durationTime = durationTime;
    }

    public Integer getLessonsCount() {
        return lessonsCount;
    }

    public void setLessonsCount(Integer lessonsCount) {
        this.lessonsCount = lessonsCount;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public Integer getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(Integer reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
