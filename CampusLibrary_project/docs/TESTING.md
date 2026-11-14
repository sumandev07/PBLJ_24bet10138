# Campus Library testing

## Run the automated checks

From the project folder, run the following commands with JDK 17 or later.

```sh
mkdir -p out
javac --release 17 -encoding UTF-8 -Xlint:all -d out src/campuslibrary/*.java tests/campuslibrary/*.java
java -Djava.awt.headless=true -cp out campuslibrary.SubmissionCheck features
java -cp out campuslibrary.SubmissionCheck restart
```

The feature checks exercise the Swing catalog search and loan status filters, book validation, lending rules, due date calculations, returns, and file locking. The restart check writes data in one JVM and loads it in a second JVM. Both use temporary data files.

| Check | Saved result |
| --- | --- |
| Features | [feature-test-output.txt](feature-test-output.txt) |
| Separate-process restart | [restart-test-output.txt](restart-test-output.txt) |

## Manual walkthrough

1. Launch `Run.command`. Search for **Effective Java** in **Catalog**.
2. Select the book and choose **Issue loan**. Enter **ST-301** and verify that the available count drops by one.
3. Open **Loan history**, choose **Active**, and find **ST-301**. Confirm the due date is 14 days after the issue date.
4. Select the loan and choose **Return loan**. Confirm that the **Returned** view includes the loan and that availability rises by one.
5. Close and reopen the app. Check that the return remains in the history.
