import com.cms.dao.CourseDAO;
import com.cms.entity.Course;
import com.cms.service.CourseService;
import com.cms.service.EnrollmentService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseServiceTest {

    @Test
    void deleteRemovesEnrollmentsBeforeDeletingCourse() {
        CourseDAO dao = mock(CourseDAO.class);
        EnrollmentService enrollmentService = mock(EnrollmentService.class);
        CourseService service = new CourseService(dao, enrollmentService);
        UUID id = UUID.randomUUID();
        Course course = new Course("Databases", "DB", null, 5);
        when(dao.findById(id)).thenReturn(course);

        service.delete(id);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(enrollmentService, dao);
        inOrder.verify(enrollmentService).deleteByCourseId(id);
        inOrder.verify(dao).delete(course);
    }
}
