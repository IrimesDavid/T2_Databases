import com.cms.service.StudentService;
import com.cms.dao.StudentDAO;
import com.cms.service.EnrollmentService;


import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.entity.Student;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentServiceTest {

    private StudentDAO dao;
    private EnrollmentService enrollmentService;
    private StudentService service;

    @BeforeEach
    void setUp() {
        dao = mock(StudentDAO.class);
        enrollmentService = mock(EnrollmentService.class);
        service = new StudentService(dao, enrollmentService);
    }

    @Test
    void createClearsIdAndSaves() {
        Student s = new Student("Ana", "Pop", "ana@example.com");
        s.setId(UUID.randomUUID());
        when(dao.save(any())).thenAnswer(i -> i.getArgument(0));

        Student saved = service.create(s);

        assertNull(saved.getId());
        verify(dao).save(s);
    }

    @Test
    void createConvertsValidationErrorToIllegalArgument() {
        when(dao.save(any())).thenThrow(new ConstraintViolationException("invalid", Set.of()));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(new Student("", "Pop", "bad")));
    }

    @Test
    void updateRejectsUnknownId() {
        UUID id = UUID.randomUUID();
        when(dao.findById(id)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.update(id, new Student("Ana", "Pop", "ana@example.com")));
    }

    @Test
    void deleteRemovesExistingStudent() {
        UUID id = UUID.randomUUID();
        Student s = new Student("Ana", "Pop", "ana@example.com");
        when(dao.findById(id)).thenReturn(s);

        service.delete(id);

        verify(enrollmentService).deleteByStudentId(id);
        verify(dao).delete(s);
    }
}