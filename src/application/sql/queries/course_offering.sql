--Course Offering

--CREATING COURSE OFFERING
--Insert course offering
INSERT INTO
    course_offering (
        teacher_id,
        course_id,
        `year`,
        total_sessions,
        term,
        max_student
    )
VALUES (?, ?, ?, ?, ?, ?);

--Insert Session Query
INSERT INTO
    class_session (
        offering_id,
        session_date,
        period
    )
VALUES (?, ?, ?);

--Insert Mark Weight
INSERT INTO
    assessment_weight (
        offering_id,
        tutorial_weight,
        assignment_weight,
        lab_test_weight,
        practical_weight,
        final_exam_weight
    )
VALUES (?, ?, ?, ?, ?, ?);

--Enroll Student into the class
INSERT IGNORE INTO
    course_enrollment (
        offering_id,
        student_id,
        enrollment_date
    )
SELECT ?, s.student_id, ?
FROM student s
    JOIN user_account ua ON s.user_id = ua.user_id
WHERE
    ua.status = TRUE
    AND s.year = ?
    AND s.dept_id IN (? ? ? ?)

--All Active Course Available RIGHT NOW
SELECT DISTINCT
    course_name
FROM course
WHERE
    is_active = TRUE
ORDER BY course_name ASC;

--All Active Class for your own department
SELECT
    co.offering_id,
    c.course_code,
    c.course_name,
    COALESCE(
        (
            SELECT d.dept_id
            FROM
                course_enrollment ce
                JOIN student s ON ce.student_id = s.student_id
                JOIN department d ON s.dept_id = d.dept_id
            WHERE
                ce.offering_id = co.offering_id
            LIMIT 1
        ),
        d_create.dept_id
    ) AS target_major,
    co.year,
    co.term,
    co.max_student,
    c.dept_id AS course_dept,
    t.full_name AS lecturer,
    co.badge_color,
    co.card_symbol
FROM
    course_offering co
    JOIN course c ON co.course_id = c.course_id
    JOIN department d_create ON co.create_dept_id = d_create.dept_id
    LEFT JOIN teacher t ON co.teacher_id = t.teacher_id
WHERE
    co.create_dept_id = ?
    OR EXISTS (
        SELECT 1
        FROM
            course_enrollment ce
            JOIN student s ON ce.student_id = s.student_id
        WHERE
            ce.offering_id = co.offering_id
            AND s.dept_id = ?
    )
ORDER BY co.offering_id DESC;