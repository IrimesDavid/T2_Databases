package com.cms.service;

import com.cms.dao.CourseDAO;
import com.cms.entity.Course;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.UUID;

@Stateless
public class CourseService {
    @EJB
    private CourseDAO dao;

    @EJB
    private EnrollmentService enrollmentService;

    public CourseService() {}

    // used by test class
    public CourseService(CourseDAO dao, EnrollmentService enrollmentService) {
        this.dao = dao;
        this.enrollmentService = enrollmentService;
    }
    
    public List<Course> findAll() {
        return dao.findAll();
    }
    
    public Course findById(UUID id) {
        Course course = dao.findById(id);

        if (course == null) {
            throw new IllegalArgumentException(
                    "Course not found: " + id
            );
        }

        return course;
    }
    
    public Course create(Course course) {
        if (course == null) {
            throw new IllegalArgumentException(
                    "Course cannot be null"
            );
        }

        // set null to make sure the UUID is generated internally when the entity is persisted
        course.setId(null);

        try {
            return dao.save(course);
        } catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(
                    getValidationMessage(e), e
            );
        }
    }

    public Course update(UUID id, Course data) {
        if (data == null) {
            throw new IllegalArgumentException(
                    "Course data cannot be null"
            );
        }

        Course existing = findById(id);

        existing.setName(data.getName());
        existing.setAbbreviation(data.getAbbreviation());
        existing.setDescription(data.getDescription());
        existing.setCredits(data.getCredits());

        try {
            return dao.update(existing);
        } catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(
                    getValidationMessage(e), e
            );
        }
    }

    public void delete(UUID id) {
        Course existing = findById(id);
        enrollmentService.deleteByCourseId(id);
        dao.delete(existing);
    }

    private String getValidationMessage(
            ConstraintViolationException e) {

        return e.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid course data");
    }
}
