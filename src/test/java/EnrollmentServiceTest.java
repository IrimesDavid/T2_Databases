import com.cms.dao.CourseDAO;
import com.cms.dao.EnrollmentDAO;
import com.cms.dao.StudentDAO;
import com.cms.entity.Course;
import com.cms.entity.Enrollment;
import com.cms.entity.Student;
import com.cms.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentServiceTest {
    private EnrollmentDAO enrollmentDAO;
    private StudentDAO studentDAO;
    private CourseDAO courseDAO;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        enrollmentDAO = mock(EnrollmentDAO.class);
        studentDAO = mock(StudentDAO.class);
        courseDAO = mock(CourseDAO.class);
        service = new EnrollmentService(enrollmentDAO, studentDAO, courseDAO);
    }

    @Test
    void createChecksReferencesAndLetsDatabaseGenerateIdAndDate() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Enrollment enrollment = new Enrollment(studentId, courseId, LocalDate.MIN);
        enrollment.setId(UUID.randomUUID());
        when(studentDAO.findById(studentId)).thenReturn(new Student());
        when(courseDAO.findById(courseId)).thenReturn(new Course());
        when(enrollmentDAO.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment saved = service.create(enrollment);

        assertNull(saved.getId());
        assertEquals(LocalDate.now(), saved.getEnrollmentDate());
        verify(studentDAO).findById(studentId);
        verify(courseDAO).findById(courseId);
        verify(enrollmentDAO).save(enrollment);
    }

    @Test
    void createRejectsUnknownStudentWithoutSaving() {
        UUID studentId = UUID.randomUUID();
        Enrollment enrollment = new Enrollment(studentId, UUID.randomUUID(), null);
        when(studentDAO.findById(studentId)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(enrollment));
        verify(enrollmentDAO, never()).save(any());
        verify(courseDAO, never()).findById(any());
    }

    @Test
    void createRejectsUnknownCourseWithoutSaving() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Enrollment enrollment = new Enrollment(studentId, courseId, null);
        when(studentDAO.findById(studentId)).thenReturn(new Student());
        when(courseDAO.findById(courseId)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(enrollment));
        verify(enrollmentDAO, never()).save(any());
    }

    @Test
    void deleteRejectsUnknownEnrollment() {
        UUID id = UUID.randomUUID();
        when(enrollmentDAO.findById(id)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.delete(id));
        verify(enrollmentDAO, never()).delete(any());
    }
}
