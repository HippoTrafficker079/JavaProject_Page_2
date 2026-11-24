--For CREATING COURSE

INSERT
    course (
        course_name,
        course_code,
        dept_id,
        course_type,
        credit
    )
VALUES (?, ?, ?, ?, ?);

INSERT
    prerequisite_course (
        course_id,
        prerequisite_course_id
    )
VALUES (?, ?);

--Registered Course Catalog
SELECT c.course_id, c.course_code, c.course_name, d.dept_code, c.course_type, c.credit, c.is_active, COALESCE(
        GROUP_CONCAT(
            DISTINCT prereq.course_code
            ORDER BY prereq.course_code SEPARATOR ', '
        ), 'None'
    ) AS prerequisites
FROM course c
FROM
    course c
    LEFT JOIN department d ON c.dept_id = d.dept_id
    LEFT JOIN prerequisite_course pc ON c.course_id = pc.course_id
    LEFT JOIN course prereq ON pc.prereq_course_id = prereq.course_id
GROUP BY
    c.course_id,
    c.course_code,
    c.course_title,
    d.dept_code,
    c.program_type,
    c.credits,
    c.status
ORDER BY c.course_code ASC;