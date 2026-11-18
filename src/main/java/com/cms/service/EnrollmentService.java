package com.cms.service;
import com.cms.dao.EnrollmentDAO;
import com.cms.dao.CourseDAO;
import com.cms.dao.StudentDAO;
import com.cms.entity.Enrollment;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Stateless
// Each persistence unit uses a non-XA datasource, so coordinate calls without one cross-database transaction.
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class EnrollmentService {
    @EJB
    private EnrollmentDAO dao;

    @EJB
    private StudentDAO studentDAO;

    @EJB
    private CourseDAO courseDAO;

    public EnrollmentService(){}

    //for tests
    public EnrollmentService(EnrollmentDAO dao, StudentDAO studentDAO, CourseDAO courseDAO){
        this.dao = dao;
        this.studentDAO = studentDAO;
        this.courseDAO = courseDAO;
    }

    public List<Enrollment> findAll() {
        return dao.findAll();
    }

    public List<Enrollment> findByCourseId(UUID courseId){
        requireCourse(courseId);
        return dao.findByCourseId(courseId);
    }

    public List<Enrollment> findByStudentId(UUID studentId){
        requireStudent(studentId);
        return dao.findByStudentId(studentId);
    }

    public Enrollment create(Enrollment enrollment) {
        if (enrollment == null) {
            throw new IllegalArgumentException(
                    "Enrollment cannot be null"
            );
        }
        if (enrollment.getStudentId() == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        if (enrollment.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        requireStudent(enrollment.getStudentId());
        requireCourse(enrollment.getCourseId());

        // set null to make sure the UUID is generated internally when the entity is persisted
        enrollment.setId(null);
        // set date now
        enrollment.setEnrollmentDate(LocalDate.now());

        try {
            return dao.save(enrollment);
        } catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(
                    getValidationMessage(e), e
            );
        }
    }

    public void deleteByCourseId(UUID courseId) {
        requireCourse(courseId);
        dao.deleteByCourseId(courseId);
    }

    public void deleteByStudentId(UUID studentId) {
        requireStudent(studentId);
        dao.deleteByStudentId(studentId);
    }

    public void delete(UUID enrollmentId) {
        if (enrollmentId == null) {
            throw new IllegalArgumentException("Enrollment ID cannot be null");
        }
        Enrollment enrollment = dao.findById(enrollmentId);
        if (enrollment == null) {
            throw new IllegalArgumentException("Enrollment not found: " + enrollmentId);
        }
        dao.delete(enrollment);
    }

    private void requireStudent(UUID studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        if (studentDAO.findById(studentId) == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
    }

    private void requireCourse(UUID courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (courseDAO.findById(courseId) == null) {
            throw new IllegalArgumentException("Course not found: " + courseId);
        }
    }

    private String getValidationMessage(
            ConstraintViolationException e) {

        return e.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid enrollment data");
    }
}
