package com.cms.dao;

import com.cms.entity.Student;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@Stateless
public class StudentDAO {

    @PersistenceContext(unitName = "PostgresPU")
    private EntityManager em;

    public List<Student> findAll() {
        return em.createQuery(
                        "SELECT s FROM Student s ORDER BY s.lastName, s.firstName", Student.class)
                .getResultList();
    }

    public Student findById(UUID id) {
        return em.find(Student.class, id);
    }

    public Student findByEmail(String email) {
        List<Student> result = em.createQuery(
                        "SELECT s FROM Student s WHERE s.email = :email", Student.class)
                .setParameter("email", email)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public Student save(Student student) {
        em.persist(student);
        return student;
    }

    public Student update(Student student) {
        return em.merge(student);
    }

    public void delete(Student student) {
        em.remove(em.contains(student) ? student : em.merge(student));
    }
}