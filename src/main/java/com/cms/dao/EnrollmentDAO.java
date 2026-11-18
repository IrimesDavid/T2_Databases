package com.cms.dao;
import com.cms.entity.Enrollment;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@Stateless
public class EnrollmentDAO {
    
    @PersistenceContext(unitName = "SqlServerPU")
    private EntityManager em;

    public List<Enrollment> findAll() {
        return em.createQuery(
                        "SELECT e FROM Enrollment e ORDER BY e.enrollmentDate DESC", Enrollment.class)
                .getResultList();
    }

    public Enrollment findById(UUID id) {
        return em.find(Enrollment.class, id);
    }

    public List<Enrollment> findByStudentId(UUID studentId) {
        return em.createQuery(
                        "SELECT e FROM Enrollment e WHERE e.studentId = :studentId ORDER BY e.enrollmentDate DESC",
                        Enrollment.class)
                .setParameter("studentId", studentId)
                .getResultList();
    }

    public List<Enrollment> findByCourseId(UUID courseId) {
        return em.createQuery(
                "SELECT e FROM Enrollment e WHERE e.courseId = :courseId ORDER BY e.enrollmentDate DESC",
                Enrollment.class)
                .setParameter("courseId", courseId)
                .getResultList();
    }

    public Enrollment save(Enrollment enrollment) {
        em.persist(enrollment);
        return enrollment;
    }

    public void delete(Enrollment enrollment) {
        em.remove(em.contains(enrollment) ? enrollment : em.merge(enrollment));
    }

    public void deleteByStudentId(UUID studentId) {
        em.createQuery(
                        "DELETE FROM Enrollment e WHERE e.studentId = :studentId"
                )
                .setParameter("studentId", studentId)
                .executeUpdate();
    }

    public void deleteByCourseId(UUID courseId) {
        em.createQuery(
                        "DELETE FROM Enrollment e WHERE e.courseId = :courseId"
                )
                .setParameter("courseId", courseId)
                .executeUpdate();
    }
}
