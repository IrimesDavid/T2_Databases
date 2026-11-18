# Course Management System

## 1. Project overview

A small Java web application for managing students, courses, and student enrollments. It demonstrates CRUD operations across **three separate database systems**: PostgreSQL, Microsoft SQL Server, and Oracle Database.

The application is a **single monolithic WAR**, not three microservices. The Java application combines data from the three databases when it needs to display enrollment relationships.

## 2. Database structure

Each database owns a different table:

| Database | Table | Fields |
|---|---|---|
| PostgreSQL | `STUDENTS` | `id` (PK), `first_name`, `last_name`, `email` |
| Oracle | `COURSES` | `id` (PK), `name`, `abbreviation`, `description`, `credits` |
| Microsoft SQL Server | `ENROLLMENTS` | `id` (PK), `student_id`, `course_id`, `enrollment_date` |

**Relationships:** `ENROLLMENT.student_id` refers to `STUDENT.id`; `ENROLLMENT.course_id` refers to `COURSE.id`. These are application-managed references, not ordinary cross-database foreign keys. Before creating an enrollment, the backend checks that the student and course exist in their respective databases. The overview and card dropdowns combine records in the service layer.

- Student email and course abbreviation should be unique.
- `enrollment_date` is assigned by the backend to the current date and cannot be edited.
- Enrollments support **Create** and **Delete** only. Students and courses support full CRUD.

## 3. Architecture

The application follows a layered MVC architecture:

```text
Browser
  │
  ▼
JSP + JSTL + JavaScript                 View
  │ HTTP requests / JSON responses
  ▼
Servlets                                Controller
  │
  ▼
EJBs (@Stateless services)              Business logic
  │
  ▼
DAOs                                    Persistence access
  │  JPA EntityManager injected per DAO
  ▼
PostgreSQL       Oracle       SQL Server
  STUDENTS        COURSES       ENROLLMENTS
```

- **Model:** JPA entities (`Student`, `Course`, `Enrollment`).
- **DAO layer:** one DAO per database/table. Each DAO uses an `EntityManager` injected with its own persistence-unit name.
- **Service layer:** stateless EJBs contain the application logic and coordinate DAOs, including lookups across databases.
- **Controller:** Servlets handle page requests and CRUD/detail endpoints.
- **View:** JSP with JSTL renders the initial page; JavaScript handles tab interaction, client-side search, autocomplete, and asynchronous requests.

Uses three JPA persistence units and three WildFly datasources: `PostgresPU`, `OraclePU`, and `SqlServerPU`. Cross-database operations are not one atomic JPA transaction; keeping the workflow simple (validates both referenced records, then inserts the enrollment).

## 4. Frontend

The application has **one page** with two tabs:

### Students tab
- Display all students as cards, sorted alphabetically by last name and then first name.
- Provide a client-side name search.
- Each card has an expandable section that makes a separate GET request to show the courses attended by that student. (backend gets ids from Enrollments table and uses them to fetch courses)
- Provide forms/actions to create, edit, and delete students.

### Courses tab
- Display all courses as cards, sorted alphabetically by course name.
- Provide a client-side name search.
- Each card has an expandable section that makes a separate GET request to show the students enrolled in that course.(backend gets ids from Enrollments table and uses them to fetch students)
- Provide forms/actions to create, edit, and delete courses.

### Enrollment form
- Provide autocomplete inputs for student and course.
- Student suggestions contain full names in alphabetical order. Course suggestions match the course name or abbreviation.
- Suggestions are based on the data already loaded in the browser. Keep the selected record's **ID** as the form value. If the user types a value manually, resolve it to an existing record; reject unknown or ambiguous matches. The backend must validate both IDs again before creating the enrollment.
- Set the enrollment date automatically on the backend. Users cannot edit it.
- Allow enrollment creation and deletion, but no enrollment update.

### Loading and requests
- The initial page GET loads **all students and courses**; there is no pagination or server-side search for now.
- Search and autocomplete use the in-memory data in the browser.
- Card expansions use separate GET requests for the related records.
- CRUD operations and enrollment creation/deletion use Servlet endpoints; JavaScript updates the page without navigating to another page.
- Pagination and server-side filtering can be future improvements.

## 5. Docker infrastructure

Docker Compose runs four services:

1. PostgreSQL
2. Microsoft SQL Server
3. OracleDB
4. WildFly hosting the application (v41.0.1)

Use a named Docker volume for each database so data survives container recreation. Configure the three JDBC drivers and datasources in WildFly. Inside the Compose network, the application connects to databases using their **service names and container ports**.

Use a multi-stage `Dockerfile`:

1. **Build stage:** use a Maven/JDK image to compile and package the project as a WAR (`mvn package`).
2. **Runtime stage:** use a WildFly image and copy the generated WAR into WildFly's `standalone\deployments` directory.

This makes `docker compose up --build` build the WAR, build the application image, start the databases, and deploy the application. Configure database credentials through environment variables (`.env` file automatically read by `docker-compose.yml`).

Useful commands:

```bash
docker compose up --build -d   # Build and start the complete setup
docker compose logs -f wildfly # Follow application logs
docker compose down            # Stop/remove containers; keep named volumes
docker compose down -v         # Also delete database volumes and their data
```

*Notes*:
1. The `enrollment-db-init` one-shot service waits for SQL Server to pass its health check, then creates the enrollment database and application login. WildFly waits for this initialization to complete successfully and connects with `SQLSERVER_USER` and `SQLSERVER_PASSWORD`; the `sa` credentials are used only by the initialization service.
2. The docker network is called `docker-net`

## 6. Project structure

```text
## 6. Project structure

```text
course-management-system/
│
├── src/
│   └── main/
│       ├── java/com/example/cms/
│       │   ├── entity/
│       │   │   ├── Student.java
│       │   │   ├── Course.java
│       │   │   └── Enrollment.java
│       │   │
│       │   ├── dao/
│       │   │   ├── StudentDAO.java
│       │   │   ├── CourseDAO.java
│       │   │   └── EnrollmentDAO.java
│       │   │
│       │   ├── service/
│       │   │   ├── StudentService.java
│       │   │   ├── CourseService.java
│       │   │   ├── EnrollmentService.java
│       │   │
│       │   └── servlet/
│       │       ├── StudentServlet.java
│       │       ├── CourseServlet.java
│       │       └── EnrollmentServlet.java
│       │
│       ├── resources/
│       │   └── META-INF/
│       │       └── persistence.xml
│       │
│       └── webapp/
│           ├── index.jsp
│           ├── WEB-INF/
│           │   └── web.xml
│           └── assets/
│               ├── css/
│               └── js/
│
├── docker/
│   ├── sqlserver/
│   │   └── init-sqlserver.sh
│   │
│   └── wildfly/
│       ├── drivers/
│       │   ├── postgresql.jar (v42.7.4)
│       │   ├── mssql-jdbc.jar (v12.8.1.jre11)
│       │   └── ojdbc.jar (v23.5.0.24.07)
│       │
│       └── configure-datasources.cli
│
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .env
├── .env.example
└── .gitignore
```

**Configuration responsibilities:**

- `Dockerfile` – builds the WAR using Maven, prepares the WildFly image, installs the JDBC drivers, executes the CLI configuration script, and deploys the WAR.
- `docker/wildfly/drivers/` – contains the JDBC driver JAR files for PostgreSQL, SQL Server, and OracleDB.
- `docker/wildfly/configure-datasources.cli` – installs the three JDBC driver modules and configures `PostgresDS`, `SqlServerDS`, and `OracleDS` under `java:jboss/datasources/` using environment expressions. `persistence.xml` should reference those JNDI names.
- `docker/sqlserver/init-sqlserver.sh` – creates the SQL Server enrollment database and app login/user before WildFly starts.
- `persistence.xml` – defines the three JPA persistence units and references their WildFly JNDI datasources.
- `docker-compose.yml` – starts the WildFly application and the three database services, with named volumes for persistent data.
- `.env` – contains database credentials and other environment-specific configuration.

The WildFly configuration is applied automatically when Docker builds the application image. There is no need to install or configure WildFly manually on the host machine. The version chosen for this project is `v41.0.1.Final-jdk21`.

## 7. Implementation checklist

1. Create the Maven WAR project and choose a Java/WildFly-compatible Jakarta EE version. ✔
2. Create the Compose services, named volumes, environment configuration, and database health checks. ✔
3. Build the multi-stage Dockerfile and verify that WildFly deploys a simple WAR. ✔
4. Configure JDBC drivers, WildFly datasources, and the three JPA persistence units. ✔
5. Create the entities, DAOs, and stateless EJB services; test CRUD against each database.
6. Implement enrollment validation and the cross-database lookup logic.
7. Implement the Servlets and the single JSP/JSTL page; add JavaScript for tabs, searches, autocomplete, dropdown GET requests, and asynchronous CRUD.
8. Test the complete workflow from a clean Docker startup and prepare the architecture diagram and project documentation.
