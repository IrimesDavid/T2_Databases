
package com.cms.service;

import com.cms.dao.StudentDAO;
import com.cms.entity.Student;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.UUID;

@Stateless
public class StudentService {
    @EJB
    StudentDAO dao; // package-private so tests can set it through constructor only while Wildfly injects automatically

    @EJB
    private EnrollmentService enrollmentService;

    public StudentService() {
    }

    public StudentService(StudentDAO dao, EnrollmentService enrollmentService) {
        this.dao = dao;
        this.enrollmentService = enrollmentService;
    }

    public List<Student> findAll() {
        return dao.findAll();
    }

    public Student findById(UUID id) {
        Student student = dao.findById(id);

        if (student == null) {
            throw new IllegalArgumentException(
                    "Student not found: " + id
            );
        }

        return student;
    }

    public Student create(Student student) {
        if (student == null) {
            throw new IllegalArgumentException(
                    "Student cannot be null"
            );
        }

        // set null to make sure the UUID is generated internally when the entity is persisted
        student.setId(null);

        try {
            return dao.save(student);
        } catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(
                    getValidationMessage(e), e
            );
        }
    }

    public Student update(UUID id, Student data) {
        if (data == null) {
            throw new IllegalArgumentException(
                    "Student data cannot be null"
            );
        }

        Student existing = findById(id);

        existing.setFirstName(data.getFirstName());
        existing.setLastName(data.getLastName());
        existing.setEmail(data.getEmail());

        try {
            return dao.update(existing);
        } catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(
                    getValidationMessage(e), e
            );
        }
    }

    public void delete(UUID id) {
        Student existing = findById(id);
        enrollmentService.deleteByStudentId(id);
        dao.delete(existing);
    }

    private String getValidationMessage(
            ConstraintViolationException e) {

        return e.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid student data");
    }
}
