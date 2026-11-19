(() => {
    "use strict";

    const root = document.getElementById("app");
    const base = `${root.dataset.contextPath}`;
    const api = {
        students: `${base}/students`,
        courses: `${base}/courses`,
        enrollments: `${base}/enrollments`
    };

    const state = {
        students: new Map(),
        courses: new Map(),
        enrollmentsByStudent: new Map(),
        enrollmentsByCourse: new Map(),
        expandedStudents: new Set(),
        expandedCourses: new Set(),
        activeTab: "students"
    };

    const elements = {
        list: document.getElementById("record-list"),
        loading: document.getElementById("loading-state"),
        loadError: document.getElementById("load-error"),
        search: document.getElementById("record-search"),
        resultCount: document.getElementById("result-count"),
        studentCount: document.getElementById("student-count"),
        courseCount: document.getElementById("course-count"),
        recordCount: document.getElementById("record-count"),
        studentsTab: document.getElementById("students-tab"),
        coursesTab: document.getElementById("courses-tab"),
        enrollmentForm: document.getElementById("enrollment-form"),
        enrollmentStudent: document.getElementById("enrollment-student"),
        enrollmentCourse: document.getElementById("enrollment-course"),
        studentOptions: document.getElementById("student-options"),
        courseOptions: document.getElementById("course-options"),
        dialog: document.getElementById("editor-dialog"),
        toastRegion: document.getElementById("toast-region")
    };

    const icon = {
        edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>',
        delete: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5M14 11v5"/></svg>'
    };

    function escapePath(id) {
        return encodeURIComponent(id);
    }

    async function request(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: {
                ...(options.body ? { "Content-Type": "application/json" } : {}),
                ...options.headers
            }
        });

        const text = await response.text();
        let body = null;
        if (text) {
            try {
                body = JSON.parse(text);
            } catch (_) {
                body = null;
            }
        }

        if (!response.ok) {
            const message = body && body.error
                ? body.error
                : `Request failed (${response.status}${response.statusText ? ` ${response.statusText}` : ""})`;
            throw new Error(message);
        }
        return body;
    }

    function idOf(value) {
        return String(value);
    }

    function studentName(student) {
        return `${student.firstName || ""} ${student.lastName || ""}`.trim();
    }

    function sortedStudents() {
        return [...state.students.values()].sort((a, b) =>
            (a.lastName || "").localeCompare(b.lastName || "") ||
            (a.firstName || "").localeCompare(b.firstName || ""));
    }

    function sortedCourses() {
        return [...state.courses.values()].sort((a, b) =>
            (a.name || "").localeCompare(b.name || ""));
    }

    function setRecords(items, map) {
        map.clear();
        items.forEach(item => {
            if (item && item.id != null) {
                map.set(idOf(item.id), item);
            }
        });
    }

    async function loadInitialData() {
        elements.loading.hidden = false;
        elements.loadError.hidden = true;
        try {
            const [students, courses] = await Promise.all([
                request(api.students),
                request(api.courses)
            ]);
            if (!Array.isArray(students) || !Array.isArray(courses)) {
                throw new Error("The server returned an unexpected records response.");
            }
            setRecords(students, state.students);
            setRecords(courses, state.courses);
            elements.loading.hidden = true;
            updateCounts();
            updateSuggestions();
            renderRecords();
        } catch (error) {
            elements.loading.hidden = true;
            elements.loadError.textContent = `Could not load records: ${error.message}`;
            elements.loadError.hidden = false;
        }
    }

    function updateCounts() {
        elements.studentCount.textContent = state.students.size;
        elements.courseCount.textContent = state.courses.size;
        if (elements.recordCount) {
            elements.recordCount.textContent = state.students.size + state.courses.size;
        }
    }

    function updateSuggestions() {
        elements.studentOptions.replaceChildren();
        sortedStudents().forEach(student => {
            const option = document.createElement("option");
            option.value = studentName(student);
            elements.studentOptions.append(option);
        });

        elements.courseOptions.replaceChildren();
        sortedCourses().forEach(course => {
            const option = document.createElement("option");
            option.value = course.name;
            elements.courseOptions.append(option);
        });
    }

    function activeRecords() {
        const query = elements.search.value.trim().toLocaleLowerCase();
        const values = state.activeTab === "students" ? sortedStudents() : sortedCourses();
        if (!query) {
            return values;
        }
        return values.filter(record => {
            const fields = state.activeTab === "students"
                ? [record.firstName, record.lastName, studentName(record), record.email]
                : [record.name, record.abbreviation, record.description];
            return fields.some(field => String(field || "").toLocaleLowerCase().includes(query));
        });
    }

    function make(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function button(className, label, content, onClick) {
        const node = document.createElement("button");
        node.type = "button";
        node.className = className;
        node.setAttribute("aria-label", label);
        node.title = label;
        node.innerHTML = content;
        node.addEventListener("click", onClick);
        return node;
    }

    function createCard(record, type) {
        const isStudent = type === "students";
        const id = idOf(record.id);
        const expanded = (isStudent ? state.expandedStudents : state.expandedCourses).has(id);
        const card = make("article", "record-card");
        const main = make("div", "card-main");
        const identity = make("div", "record-identity");
        const title = isStudent ? studentName(record) : record.name;
        const initials = isStudent
            ? `${(record.firstName || "?")[0]}${(record.lastName || "")[0] || ""}`.toUpperCase()
            : (record.abbreviation || "?").toUpperCase();

        const avatar = make("div", `avatar${isStudent ? "" : " course-avatar"}`, initials);
        avatar.setAttribute("aria-hidden", "true");
        const text = make("div");
        text.append(make("h3", "record-title", title || "Unnamed record"));
        text.append(make("p", "record-subtitle", isStudent
            ? (record.email || "No email address")
            : `${record.credits != null ? `${record.credits} credits` : ""}`));
        identity.append(avatar, text);

        const actions = make("div", "card-actions");
        actions.append(button("icon-button", `Edit ${isStudent ? "student" : "course"}`, icon.edit, () => openEditor(type, record)));
        actions.append(button("icon-button delete", `Delete ${isStudent ? "student" : "course"}`, icon.delete, () => deleteRecord(type, id, title)));
        main.append(identity, actions);

        const detailsButton = make("button", "details-toggle");
        detailsButton.type = "button";
        detailsButton.setAttribute("aria-expanded", String(expanded));
        detailsButton.append(
            make("span", "", expanded ? "Hide enrollments" : "View enrollments"),
            make("span", "chevron", "⌄")
        );
        const detailArea = make("div", "card-details");
        detailArea.hidden = !expanded;
        detailsButton.addEventListener("click", () => toggleDetails(type, id, detailsButton, detailArea));

        card.append(main, detailsButton, detailArea);
        if (expanded) {
            loadAndRenderDetails(type, id, detailArea);
        }
        return card;
    }

    function renderRecords() {
        const records = activeRecords();
        elements.list.replaceChildren();
        elements.resultCount.textContent = `${records.length} ${records.length === 1 ? "record" : "records"}`;
        if (records.length === 0) {
            const empty = make("div", "empty-state");
            empty.append(
                make("strong", "", elements.search.value.trim() ? "No matching records" : `No ${state.activeTab} yet`),
                make("span", "", elements.search.value.trim() ? "Try a different search." : `Add a ${state.activeTab === "students" ? "student" : "course"} to get started.`)
            );
            elements.list.append(empty);
            return;
        }
        records.forEach(record => elements.list.append(createCard(record, state.activeTab)));
    }

    function switchTab(type) {
        state.activeTab = type;
        const studentsActive = type === "students";
        elements.studentsTab.classList.toggle("active", studentsActive);
        elements.coursesTab.classList.toggle("active", !studentsActive);
        elements.studentsTab.setAttribute("aria-selected", String(studentsActive));
        elements.coursesTab.setAttribute("aria-selected", String(!studentsActive));
        elements.studentsTab.tabIndex = studentsActive ? 0 : -1;
        elements.coursesTab.tabIndex = studentsActive ? -1 : 0;
        elements.list.setAttribute("aria-labelledby", studentsActive ? "students-tab" : "courses-tab");
        elements.search.placeholder = studentsActive ? "Search students" : "Search courses";
        elements.search.value = "";
        renderRecords();
    }

    function toggleDetails(type, id, toggle, panel) {
        const expandedSet = type === "students" ? state.expandedStudents : state.expandedCourses;
        if (expandedSet.has(id)) {
            expandedSet.delete(id);
            toggle.setAttribute("aria-expanded", "false");
            toggle.firstElementChild.textContent = "View enrollments";
            panel.hidden = true;
            return;
        }
        expandedSet.add(id);
        toggle.setAttribute("aria-expanded", "true");
        toggle.firstElementChild.textContent = "Hide enrollments";
        panel.hidden = false;
        loadAndRenderDetails(type, id, panel);
    }

    async function loadAndRenderDetails(type, id, panel) {
        panel.replaceChildren(make("div", "inline-state", "Loading enrollments…"));
        const cache = type === "students" ? state.enrollmentsByStudent : state.enrollmentsByCourse;
        try {
            let enrollments = cache.get(id);
            if (!enrollments) {
                const query = type === "students" ? "for_student" : "for_course";
                enrollments = await request(`${api.enrollments}/${query}/${escapePath(id)}`);
                if (!Array.isArray(enrollments)) {
                    throw new Error("The server returned an unexpected enrollment response.");
                }
                cache.set(id, enrollments);
            }
            renderEnrollmentRows(type, id, enrollments, panel);
        } catch (error) {
            const failure = make("div", "inline-state error", `Could not load enrollments: ${error.message}`);
            panel.replaceChildren(failure);
        }
    }

    function renderEnrollmentRows(type, parentId, enrollments, panel) {
        panel.replaceChildren();
        if (enrollments.length === 0) {
            panel.append(make("div", "inline-state", "No enrollments yet."));
            return;
        }

        enrollments.forEach(enrollment => {
            const relatedId = idOf(type === "students" ? enrollment.courseId : enrollment.studentId);
            const related = type === "students" ? state.courses.get(relatedId) : state.students.get(relatedId);
            const row = make("div", "enrollment-row");
            const info = make("div", "enrollment-person");
            let name;
            if (!related) {
                name = type === "students" ? "Course no longer available" : "Student no longer available";
            } else {
                name = type === "students" ? related.name : studentName(related);
            }
            info.append(make("p", "enrollment-name", name || "Unnamed record"));
            info.append(make("p", "enrollment-date", `Started ${formatDate(enrollment.enrollmentDate)}`));
            const remove = button("icon-button delete", "Delete enrollment", icon.delete, async () => {
                if (!window.confirm(`Delete this enrollment for ${name || "this record"}?`)) return;
                try {
                    await request(`${api.enrollments}/${escapePath(enrollment.id)}`, { method: "DELETE" });
                    clearEnrollmentCaches();
                    refreshOpenCards();
                    showToast("Enrollment deleted.");
                } catch (error) {
                    showToast(error.message, true);
                }
            });
            row.append(info, remove);
            panel.append(row);
        });
    }

    function formatDate(value) {
        if (!value) return "an unknown date";
        const date = new Date(`${value}T00:00:00`);
        return Number.isNaN(date.getTime())
            ? value
            : new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "numeric" }).format(date);
    }

    function clearEnrollmentCaches() {
        state.enrollmentsByStudent.clear();
        state.enrollmentsByCourse.clear();
    }

    function refreshOpenCards() {
        renderRecords();
    }

    function resolveStudent(value) {
        const needle = value.trim().toLocaleLowerCase();
        const matches = sortedStudents().filter(student =>
            studentName(student).toLocaleLowerCase() === needle ||
            String(student.email || "").toLocaleLowerCase() === needle);
        return uniqueMatch(matches, "student");
    }

    function resolveCourse(value) {
        const needle = value.trim().toLocaleLowerCase();
        const matches = sortedCourses().filter(course =>
            String(course.name || "").toLocaleLowerCase() === needle ||
            String(course.abbreviation || "").toLocaleLowerCase() === needle);
        return uniqueMatch(matches, "course");
    }

    function uniqueMatch(matches, type) {
        const unique = new Map(matches.map(item => [idOf(item.id), item]));
        if (unique.size === 0) {
            throw new Error(`Choose an existing ${type} from the suggestions.`);
        }
        if (unique.size > 1) {
            throw new Error(`That ${type} name is ambiguous. Use a unique value.`);
        }
        return unique.values().next().value;
    }

    async function createEnrollment(event) {
        event.preventDefault();
        try {
            const student = resolveStudent(elements.enrollmentStudent.value);
            const course = resolveCourse(elements.enrollmentCourse.value);
            await request(api.enrollments, {
                method: "POST",
                body: JSON.stringify({ studentId: student.id, courseId: course.id })
            });
            elements.enrollmentForm.reset();
            clearEnrollmentCaches();
            refreshOpenCards();
            showToast(`Enrolled ${studentName(student)} in ${course.name}.`);
        } catch (error) {
            showToast(error.message, true);
        }
    }

    function createField(labelText, name, value, options = {}) {
        const label = make("label", "field");
        label.append(make("span", "", labelText));
        const input = document.createElement(options.multiline ? "textarea" : "input");
        input.name = name;
        input.value = value == null ? "" : value;
        if (options.type) input.type = options.type;
        if (options.required) input.required = true;
        if (options.min != null) input.min = String(options.min);
        if (options.step != null) input.step = String(options.step);
        if (options.multiline) input.rows = 3;
        label.append(input);
        return { label, input };
    }

    function openEditor(type, record = null) {
        const isStudent = type === "students";
        const form = document.createElement("form");
        form.className = "dialog-content";
        const head = make("div", "dialog-head");
        const titleBlock = make("div");
        titleBlock.append(make("p", "eyebrow", record ? "UPDATE RECORD" : "NEW RECORD"));
        titleBlock.append(make("h2", "", `${record ? "Edit" : "Add"} ${isStudent ? "student" : "course"}`));
        head.append(titleBlock);

        const fields = make("div", "dialog-fields");
        const inputs = {};
        const definitions = isStudent
            ? [
                ["First name", "firstName", record?.firstName, { required: true }],
                ["Last name", "lastName", record?.lastName, { required: true }],
                ["Email address", "email", record?.email, { type: "email", required: true }]
            ]
            : [
                ["Course name", "name", record?.name, { required: true }],
                ["Abbreviation", "abbreviation", record?.abbreviation, { required: true }],
                ["Description", "description", record?.description, { multiline: true }],
                ["Credits", "credits", record?.credits, { type: "number", min: 0, step: 1, required: true }]
            ];
        definitions.forEach(([label, name, value, options]) => {
            const field = createField(label, name, value, options);
            inputs[name] = field.input;
            fields.append(field.label);
        });

        const error = make("p", "dialog-error");
        error.hidden = true;
        const actions = make("div", "dialog-actions");
        const cancel = make("button", "button button-outline", "Cancel");
        cancel.type = "button";
        cancel.addEventListener("click", () => elements.dialog.close());
        const save = make("button", "button button-primary", record ? "Save changes" : `Create ${isStudent ? "student" : "course"}`);
        save.type = "submit";
        actions.append(cancel, save);
        form.append(head, fields, error, actions);

        form.addEventListener("submit", async event => {
            event.preventDefault();
            error.hidden = true;
            const payload = {};
            definitions.forEach(([, name]) => {
                payload[name] = name === "credits" ? Number(inputs[name].value) : inputs[name].value.trim();
            });

            try {
                const url = record ? `${api[type]}/${escapePath(record.id)}` : api[type];
                const saved = await request(url, {
                    method: record ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                const records = state[type];
                records.set(idOf(saved.id), saved);
                clearEnrollmentCaches();
                updateCounts();
                updateSuggestions();
                elements.dialog.close();
                renderRecords();
                showToast(`${isStudent ? "Student" : "Course"} ${record ? "updated" : "created"}.`);
            } catch (failure) {
                error.textContent = failure.message;
                error.hidden = false;
            }
        });

        elements.dialog.replaceChildren(form);
        elements.dialog.showModal();
        const firstInput = form.querySelector("input, textarea");
        if (firstInput) firstInput.focus();
    }

    async function deleteRecord(type, id, label) {
        const kind = type === "students" ? "student" : "course";
        if (!window.confirm(`Delete ${kind} "${label}"? Its enrollments will also be deleted.`)) return;
        try {
            await request(`${api[type]}/${escapePath(id)}`, { method: "DELETE" });
            state[type].delete(id);
            (type === "students" ? state.expandedStudents : state.expandedCourses).delete(id);
            clearEnrollmentCaches();
            updateCounts();
            updateSuggestions();
            renderRecords();
            showToast(`${kind[0].toUpperCase()}${kind.slice(1)} deleted.`);
        } catch (error) {
            showToast(error.message, true);
        }
    }

    function showToast(message, isError = false) {
        const toast = make("div", `toast${isError ? " error" : ""}`, message);
        elements.toastRegion.append(toast);
        window.setTimeout(() => toast.remove(), 4000);
    }

    elements.studentsTab.addEventListener("click", () => switchTab("students"));
    elements.coursesTab.addEventListener("click", () => switchTab("courses"));
    elements.search.addEventListener("input", renderRecords);
    elements.enrollmentForm.addEventListener("submit", createEnrollment);
    document.getElementById("add-student").addEventListener("click", () => openEditor("students"));
    document.getElementById("add-course").addEventListener("click", () => openEditor("courses"));
    elements.dialog.addEventListener("click", event => {
        if (event.target === elements.dialog) elements.dialog.close();
    });

    loadInitialData();
})();
