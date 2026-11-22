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