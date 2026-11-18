package com.cms.servlet;

import com.cms.entity.Course;
import com.cms.service.CourseService;
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
 * GET /courses, GET /courses/{id}, POST /courses, PUT /courses/{id}, DELETE /courses/{id}.
 * Status codes: 200 ok, 400 bad input, 500 anything else.
 */
@WebServlet("/courses/*")
public class CourseServlet extends HttpServlet {

    private interface Action {
        Object run() throws Exception;
    }

    @EJB
    private CourseService service;

    private final Jsonb jsonb = JsonbBuilder.create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        handle(resp, () -> path == null || path.equals("/")
            ? service.findAll()
            : service.findById(id(req)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handle(resp, () -> service.create(body(req)));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handle(resp, () -> service.update(id(req), body(req)));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handle(resp, () -> {
            UUID id = id(req);
            service.delete(id);
            return Map.of("deleted", id);
        });
    }

    private UUID id(HttpServletRequest req) {
    String path = req.getPathInfo();
    if (path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
    }
    return UUID.fromString(path.substring(1));
}

    private Course body(HttpServletRequest req) throws IOException {
        req.setCharacterEncoding("UTF-8");
        return jsonb.fromJson(req.getReader(), Course.class);
    }

    private void handle(HttpServletResponse resp, CourseServlet.Action action) throws IOException {
        try {
            send(resp, 200, action.run());

        } catch (EJBException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                send(resp, 400, Map.of("error", String.valueOf(cause.getMessage())));
            } else {
                getServletContext().log("Course request failed", e);
                send(resp, 500, Map.of("error", "Internal server error"));
            }
        }
        catch (IllegalArgumentException e) {
            send(resp, 400, Map.of("error", String.valueOf(e.getMessage())));

        }
        catch (Exception e) {
            getServletContext().log("Course request failed", e);

            send(resp, 500,
                    Map.of("error", "Internal server error"));
        }
    }

    private void send(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(jsonb.toJson(body));
    }
}
