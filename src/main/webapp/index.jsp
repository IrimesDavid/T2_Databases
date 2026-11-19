<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Course Management</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
    <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body>
<div class="app-shell" id="app" data-context-path="${pageContext.request.contextPath}">
    <header class="topbar">
        <a class="brand" href="${pageContext.request.contextPath}/" aria-label="Course Management home">
        <span class="brand-mark">C</span><span class="brand-mark">M</span><span class="brand-mark">C</span>
        </a>
    </header>

    <main>
        <section class="page-intro">
            <div>
                <p class="eyebrow">ADMINISTRATION & OVERVIEW</p>
                <h1>Course Management Catalogue</h1>
            </div>
        </section>

        <section class="enrollment-panel" aria-labelledby="enrollment-title">
            <div class="panel-heading">
                <div class="panel-icon" aria-hidden="true">+</div>
                <div>
                    <h2>Enrollments</h2>
                </div>
            </div>
            <form id="enrollment-form" class="enrollment-form">
                <label class="field">
                    <span>Student</span>
                    <input id="enrollment-student" name="student" list="student-options" placeholder="Search student" autocomplete="off" required>
                    <datalist id="student-options"></datalist>
                </label>
                <label class="field">
                    <span>Course</span>
                    <input id="enrollment-course" name="course" list="course-options" placeholder="Search course" autocomplete="off" required>
                    <datalist id="course-options"></datalist>
                </label>
                <button class="button button-primary enrollment-submit" type="submit">Add enrollment <span aria-hidden="true">→</span></button>
            </form>
        </section>

        <section class="records-section">
            <div class="section-heading">
                <div>
                    <h2>Records</h2>
                </div>
                <div class="create-actions">
                    <button class="button button-outline" id="add-student" type="button"><span aria-hidden="true">+</span> Add student</button>
                    <button class="button button-primary" id="add-course" type="button"><span aria-hidden="true">+</span> Add course</button>
                </div>
            </div>

            <div class="tabs" role="tablist" aria-label="Record type">
                <button class="tab active" id="students-tab" type="button" role="tab" aria-selected="true" aria-controls="students-panel">
                    Students <span class="tab-count" id="student-count">0</span>
                </button>
                <button class="tab" id="courses-tab" type="button" role="tab" aria-selected="false" aria-controls="courses-panel" tabindex="-1">
                    Courses <span class="tab-count" id="course-count">0</span>
                </button>
            </div>

            <div class="list-toolbar">
                <label class="search-box">
                    <span class="search-icon" aria-hidden="true"></span>
                    <span class="visually-hidden">Search records</span>
                    <input id="record-search" type="search" placeholder="Search student" autocomplete="off">
                </label>
                <span class="result-label" id="result-count"></span>
            </div>

            <div class="loading-state" id="loading-state">
                <span class="spinner" aria-hidden="true"></span>
                <span>Loading students and courses…</span>
            </div>
            <div class="error-banner" id="load-error" role="alert" hidden></div>
            <div class="card-grid" id="record-list" role="tabpanel" aria-labelledby="students-tab" aria-live="polite"></div>
        </section>
    </main>

    <footer class="footer">
        <span>CMC</span>
        <span>Student and course records</span>
    </footer>
</div>

<dialog class="editor-dialog" id="editor-dialog" aria-labelledby="editor-title"></dialog>
<div class="toast-region" id="toast-region" aria-live="polite" aria-atomic="true"></div>
</body>
</html>
