-- GET ALL ENROLLED STUDENT OF YOUR DEPT OR GET ALL ENROLLED STUDENT OF ANY DEPARTMENT THAT TOOK YOUR DEPT'S COURSE
SELECT
    s.student_id,
    s.roll_no,
    s.full_name,
    sci.email_address,
    COALESCE(
        ROUND(
            (
                COUNT(DISTINCT cs.session_id) - COUNT(DISTINCT a.attendance_id)
            ) * 100.0 / NULLIF(
                COUNT(DISTINCT cs.session_id),
                0
            ),
            2
        ),
        0.00
    ) AS attendance_percentage,
    s.gpa
FROM
    student s
    LEFT JOIN student_contact_information sci ON s.student_id = sci.student_id
    JOIN course_enrollment ce ON s.student_id = ce.student_id
    AND ce.status = 'ENROLLED'
    JOIN course_offering co ON ce.offering_id = co.offering_id
    JOIN course c ON co.course_id = c.course_id
    LEFT JOIN class_session cs ON cs.offering_id = co.offering_id
    AND cs.is_cancelled = FALSE
    LEFT JOIN attendance a ON cs.session_id = a.session_id
    AND ce.enrollment_id = a.enrollment_id
WHERE
    co.offering_id = ?
    AND (
        c.dept_id = ?
        OR s.dept_id = ?
    )
GROUP BY
    s.student_id,
    s.roll_no,
    s.full_name,
    sci.email_address,
    s.gpa
ORDER BY s.roll_no ASC;

--Remove The Student from Class(DROP)
UPDATE course_enrollment
SET
    status = 'DROPPED'
WHERE
    student_id = ?
    AND offering_id = ?;