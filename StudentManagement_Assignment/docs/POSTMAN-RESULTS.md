# Student Management REST API test results

Postman CLI ran the collection against the Java API and MySQL 8.0.32. All 12 requests and 23 assertions passed.

| Operation | Request | Observed response |
| --- | --- | --- |
| Add a student | `POST /api/students` | `201 Created` |
| List students | `GET /api/students` | `200 OK` |
| Get a student | `GET /api/students/1` | `200 OK` |
| Update a student | `PUT /api/students/1` | `200 OK` |
| Search by course | `GET /api/students/course/data%20science` | `200 OK` |
| Reject a duplicate email | `POST /api/students` | `409 Conflict` |
| Reject invalid fields | `POST /api/students` | `400 Bad Request` |
| Reject a fractional age | `POST /api/students` | `400 Bad Request` |
| Reject a nonnumeric ID | `GET /api/students/abc` | `400 Bad Request` |
| Delete the student | `DELETE /api/students/1` | `204 No Content` |
| Check for the deleted student | `GET /api/students/1` | `404 Not Found` |
| Search for a course with no students | `GET /api/students/course/course-with-no-students` | `200 OK` with an empty array |

The run created and deleted a fictional student. The ID in a new run may differ.

- [Postman summary](screenshots/00-postman-summary.jpg)
- [Screenshot of all requests](screenshots/01-all-requests.jpg)
- [Create response](screenshots/02-create-response.jpg)
- [Delete response](screenshots/03-delete-response.jpg)
- [Postman CLI output](postman-test-output.txt)
