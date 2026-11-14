# Student Management REST API

A Java REST API for managing student records. Spring Boot handles HTTP requests, Spring Data JPA reads and writes records, and MySQL stores student data. Each student has an auto-generated ID, name, email, course, and age.

## Setup

Use JDK 17 or later and a running MySQL 8 server. The included Maven Wrapper downloads Maven on its first run.

1. From the project folder, create the database and table with a MySQL administrator account.

   ```sh
   mysql -u root -p < database/schema.sql
   ```

2. As a MySQL administrator, create a user for the API. Replace the example password with one you choose.

   ```sql
   CREATE USER 'student_app'@'127.0.0.1' IDENTIFIED BY 'replace-with-your-own-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON student_management.*
       TO 'student_app'@'127.0.0.1';
   ```

3. In Terminal, load your MySQL password into the environment and run the API.

   ```sh
   printf 'MySQL student_app password: '
   stty -echo
   IFS= read -r DB_PASSWORD
   stty echo
   printf '\n'
   export DB_PASSWORD
   ./mvnw spring-boot:run
   ```

   On Windows, set the same environment variable in your shell and run `mvnw.cmd spring-boot:run`.

The API listens at `http://127.0.0.1:8080`. Set `DB_URL`, `DB_USERNAME`, or `SERVER_PORT` if your setup differs. Spring checks the database schema at startup. In IntelliJ IDEA, open the folder as a Maven project and run `StudentManagementApplication` with the same environment variables.

## Routes

| Method | Route | Success |
| --- | --- | --- |
| POST | `/api/students` | `201 Created`, student JSON and a `Location` header |
| GET | `/api/students` | `200 OK`, array sorted by ID |
| GET | `/api/students/{id}` | `200 OK`, student JSON |
| PUT | `/api/students/{id}` | `200 OK`, updated student JSON |
| DELETE | `/api/students/{id}` | `204 No Content` |
| GET | `/api/students/course/{course}` | `200 OK`, case-insensitive course matches |

Use the following JSON for POST and PUT. PUT replaces all four editable fields. For course names with spaces, use a path such as `/api/students/course/Computer%20Science`.

```json
{
  "name": "Asha Rao",
  "email": "asha.rao@example.com",
  "course": "Computer Science",
  "age": 20
}
```

A missing student returns `404`. Invalid fields or malformed requests return `400`, and a duplicate email returns `409`. A list or course search with no matches returns `200` and `[]`.

## Postman

Import [the collection](postman/Student-Management.postman_collection.json), start the API, and run the collection in order. The first request creates a sample record and saves its ID. The delete request removes that sample. The default collection URL is `http://127.0.0.1:8080`. Change `baseUrl` if you use a different port.

With Postman CLI installed, run the same checks from Terminal:

```sh
postman collection run postman/Student-Management.postman_collection.json
```

The recorded Postman CLI run against MySQL 8.0.32 passed 12 requests and 23 assertions. See the [interactive Postman report](docs/postman-report.html), the [CLI output](docs/postman-test-output.txt), and the [screenshots showing all requests](docs/screenshots/01-all-requests.jpg). The report was captured on a separate test port.

## Source guide

`StudentController` maps HTTP routes to `StudentService`. The service uses `StudentRepository` for MySQL operations. `StudentRequest` validates request data, `Student` maps to the database table, and `ApiExceptionHandler` gives clients structured error responses.
