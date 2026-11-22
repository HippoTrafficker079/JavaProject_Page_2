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