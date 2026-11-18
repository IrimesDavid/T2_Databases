package com.cms.dao;
import com.cms.entity.Course;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@Stateless
public class CourseDAO {
    @PersistenceContext(unitName = "OraclePU")
    private EntityManager em;

    public List<Course> findAll() {
        return em.createQuery(
                        "SELECT c FROM Course c ORDER BY c.name", Course.class)
                .getResultList();
    }

    public Course findById(UUID id) {
        return em.find(Course.class, id);
    }

    public Course save(Course course) {
        em.persist(course);
        return course;
    }

    public Course update(Course course) {
        return em.merge(course);
    }

    public void delete(Course course) {
        em.remove(em.contains(course) ? course : em.merge(course));
    }
}
