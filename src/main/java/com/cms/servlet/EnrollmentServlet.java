package com.cms.servlet;

import com.cms.entity.Enrollment;
import com.cms.service.EnrollmentService;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * GET /enrollments, GET /enrollments/for_course/{course_id}, GET /enrollments/for_student/{student_id},
 * POST /enrollments,
 * DELETE /enrollments/{id},
 * Status codes: 200 ok, 400 bad input, 404 unknown path, 500 anything else.
 */
@WebServlet("/enrollments/*")
public class EnrollmentServlet extends HttpServlet {

    private interface Action {
        Object run() throws Exception;
    }

    @EJB
    private EnrollmentService service;

    private final Jsonb jsonb = JsonbBuilder.create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            handle(resp, service::findAll);
            return;
        }

        UUID courseId = pathId(path, "/for_course/");
        UUID studentId = pathId(path, "/for_student/");
        if (courseId != null) {
            handle(resp, () -> service.findByCourseId(courseId));
        } else if (studentId != null) {
            handle(resp, () -> service.findByStudentId(studentId));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            handle(resp, () -> service.create(body(req)));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        UUID enrollmentId = pathId(path, "/");

        if (enrollmentId != null) {
            handle(resp, () -> {
                service.delete(enrollmentId);
                return Map.of("deleted", enrollmentId);
            });
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private UUID pathId(String path, String prefix) {
        if (path == null) {
            return null;
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.startsWith(prefix)) {
            return null;
        }

        String value = path.substring(prefix.length());
        if (!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return null;
        }
        return UUID.fromString(value);
    }

    private Enrollment body(HttpServletRequest req) throws IOException {
        req.setCharacterEncoding("UTF-8");
        return jsonb.fromJson(req.getReader(), Enrollment.class);
    }

    private void handle(HttpServletResponse resp, Action action) throws IOException {
        try {
            send(resp, 200, action.run());
        } catch (EJBException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                send(resp, 400, Map.of("error", String.valueOf(cause.getMessage())));
            } else {
                getServletContext().log("Enrollment request failed", e);
                send(resp, 500, Map.of("error", "Internal server error"));
            }
        } catch (IllegalArgumentException e) {
            send(resp, 400, Map.of("error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            getServletContext().log("Enrollment request failed", e);
            send(resp, 500, Map.of("error", "Internal server error"));
        }
    }

    private void send(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(jsonb.toJson(body));
    }
}
