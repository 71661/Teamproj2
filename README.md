# Task Manager — Java CRUD
Student: **71661**

A local web application for creating, reading, updating and deleting tasks.
Each task has a title, description and completion status. Tasks persist in
`data/tasks.properties` between application restarts.

## Run
Install **JDK 21 or newer** (a JRE alone is insufficient).
On Windows, double-click `run.bat`, or run it in a terminal.
Then open http://localhost:8080 in your browser. Stop with Ctrl+C.

Other platforms:
```sh
mkdir -p out
javac -encoding UTF-8 -d out src/TaskApp.java
java -cp out TaskApp
```

## Try the CRUD operations
1. Enter a title and description, then click **Save task** (Create).
2. The task appears under **Your tasks** (Read).
3. Click **Edit**, change the text or check **Completed**, then save (Update).
4. Click **Delete** to remove the task (Delete).
5. Restart the application to verify that remaining tasks are preserved.

## How it works
- `src/TaskApp.java`: Java entry point, local HTTP server, HTML interface, form handling and file storage.
- GET / displays the list; GET /?edit=ID opens an existing task in the form.
- POST /save creates or updates a task; POST /delete removes it.
- A LinkedHashMap holds tasks in memory. Properties stores their fields on disk.
- HTML text is escaped and task input is validated.
- The server listens only on the local computer. This is a classroom demo, without authentication.

## Pair collaboration
Your partner should make a small real change on a branch or fork, open a pull
request, and the repository owner should review and merge it. Repeat in your
partner's repository. Keep both authors' commits in the history.

## Evidence
See `doc/README.md` for screenshots required for submission.
