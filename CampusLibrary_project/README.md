# Campus Library

Campus Library is a Java Swing desktop application for a college library. A librarian can search the catalog, add books, issue and return copies, and see which loans are overdue. The project demonstrates object-oriented modelling, collections, input validation, date handling, event-driven programming, and file persistence.

## Folder layout

```text
CampusLibrary/
├── data/
│   └── library.tsv
├── docs/
│   ├── screenshots/
│   │   ├── catalog.png
│   │   └── overdue-loans.png
│   ├── REPORT.md
│   ├── TESTING.md
│   ├── VIVA.md
│   ├── feature-test-output.txt
│   └── restart-test-output.txt
├── out/campuslibrary/    Compiled app and test classes
├── src/campuslibrary/    Java source
├── tests/campuslibrary/
│   └── SubmissionCheck.java
├── README.md
└── Run.command
```

The [project report](docs/REPORT.md) describes the design. [Testing](docs/TESTING.md) explains how to rerun the checks, and [Viva preparation](docs/VIVA.md) covers the main implementation decisions.

## Run on macOS

Open **Run.command** from Finder, or use Terminal:

```bash
cd ~/Desktop/CampusLibrary
./Run.command
```

Install a JDK version 17 or later to compile and run the project. The project uses only the Java standard library.

## Run from an IDE or another operating system

Open the project folder in a Java IDE, set `src` as the source root, select JDK 17 or later, and run `campuslibrary.CampusLibrary` with the project folder as the working directory. You can also compile from the project folder:

```sh
mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/campuslibrary/*.java
java -cp out campuslibrary.CampusLibrary
```

On Windows, you can use `mkdir out` and list the three Java source paths explicitly if your shell does not expand the wildcard.

## Five-minute demo

1. Open the app. The included sample file has eight book titles and three loans.
2. Search the catalog for **Java**. Select **Effective Java** and click **Issue loan**. Enter **ST-301** and check that the available count drops by one.
3. Open **Loan history** and select **Active**. Find **ST-301** and check that the due date is 14 days after the issue date.
4. Select that loan and click **Return loan**. Use the **Returned** filter to find it. The catalog should show the copy as available again.
5. Select **Overdue** to see the sample loan for **BK-107**. Status is calculated from today’s date, so other sample loans may also become overdue. Restart the app and confirm that the return is still recorded.

To demo validation, try adding **BK-101** again, or issue the same book twice to the same student before returning it. Select **Add book** and use a new ID such as **BK-109** to demo a successful addition.

## Source guide

| File | Responsibility |
| --- | --- |
| `CampusLibrary.java` | Swing window, catalog search, loan filters, and input dialogs |
| `Library.java` | Book and loan records, availability, the 14-day loan rule, and validation |
| `LibraryFile.java` | Reads and writes UTF-8 tab-separated records and locks the data so only one session can edit it |

Available copies equal the total copies minus active loans for that book. A loan is active until it has a return date. It becomes overdue the day after its due date if it is still active. The student ID is stored on each loan.

The app saves changes to `data/library.tsv` after each successful action. Keep that file to retain your catalog and loan history. To start with fresh sample data, quit the app and rename the data file before relaunching. The old file remains available as a backup. To run with a separate file, pass its path as the first argument to the Java command.

This project models one library branch and uses local storage.
