package campuslibrary;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class SubmissionCheck {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC);
    private static final String BOOK_ID = "BK-109";
    private static final String STUDENT_ID = "ST-301";

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            Path data = Path.of(args[1]);
            switch (args[0]) {
                case "write" -> writeRecords(data);
                case "read" -> readRecords(data);
                default -> throw new IllegalArgumentException("Unknown restart test phase.");
            }
            return;
        }
        if (args.length != 1 || !(args[0].equals("features") || args[0].equals("restart"))) {
            throw new IllegalArgumentException("Choose features or restart.");
        }

        Path folder = Files.createTempDirectory("campus-library-test-");
        try {
            Path data = folder.resolve("library.tsv");
            if (args[0].equals("features")) {
                features(data);
            } else {
                runPhase("write", data);
                runPhase("read", data);
            }
        } finally {
            try (var paths = Files.walk(folder)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void features(Path data) throws Exception {
        System.out.println("Campus Library feature checks");
        try (LibraryFile file = new LibraryFile(data)) {
            Library library = new Library(file, CLOCK);
            check(library.books().size() == 8 && library.loans().size() == 3, "Sample catalog and loans");
            check(library.loans().stream().filter(loan -> loan.overdue(library.today())).count() == 1,
                    "Sample overdue loan");
            checkViews(library);

            addDemoBook(library);
            check(library.book(BOOK_ID).title().equals("Java: The Complete Reference"), "Add and find a book");
            rejects("Duplicate book ID", () -> addDemoBook(library));
            rejects("Invalid copy count", () -> library.addBook("BK-110", "Title", "Author", 0));
            rejects("Invalid student ID", () -> library.issue(BOOK_ID, "ST 301"));
            rejects("Unknown book ID", () -> library.issue("BK-999", STUDENT_ID));

            library.issue(BOOK_ID, "st-301");
            check(library.available(library.book(BOOK_ID)) == 1, "Issue reduces available copies");
            rejects("Duplicate active loan", () -> library.issue(BOOK_ID, STUDENT_ID));
            library.issue(BOOK_ID, "ST-302");
            rejects("Issue with no available copies", () -> library.issue(BOOK_ID, "ST-303"));

            Library.Loan loan = studentLoan(library, STUDENT_ID);
            LocalDate due = LocalDate.of(2026, 11, 28);
            check(loan.due().equals(due), "14-day due date");
            check(!loan.overdue(due) && loan.overdue(due.plusDays(1)), "Overdue starts the day after the due date");
            library.returnLoan(loan.id());
            check(library.available(library.book(BOOK_ID)) == 1
                    && !studentLoan(library, STUDENT_ID).active(), "Return restores the copy and preserves the loan");
            rejects("Repeat return", () -> library.returnLoan(loan.id()));

            try (LibraryFile secondFile = new LibraryFile(data)) {
                throw new AssertionError("Concurrent writer acquired a lock on " + secondFile);
            } catch (IOException expected) {
                check(expected.getMessage().contains("already open"), "Second writer blocked");
            }
        }

        Files.writeString(data, "corrupt");
        try (LibraryFile file = new LibraryFile(data)) {
            try {
                new Library(file, CLOCK);
                throw new AssertionError("Corrupt data loaded");
            } catch (IOException expected) {
                check(Files.readString(data).equals("corrupt"), "Invalid data rejected and preserved");
            }
        }
    }

    private static void checkViews(Library library) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var tabs = descendant(new CampusLibrary(library).content(), JTabbedPane.class);
            var catalog = tabs.getComponentAt(0);
            var search = descendant(catalog, JTextField.class);
            var catalogTable = descendant(catalog, JTable.class);
            search.setText("bloch");
            check(catalogTable.getRowCount() == 1 && catalogTable.getValueAt(0, 0).equals("BK-101"),
                    "Catalog search filters by author");
            search.setText("algorithms");
            check(catalogTable.getRowCount() == 1 && catalogTable.getValueAt(0, 0).equals("BK-103"),
                    "Catalog search filters by title");
            search.setText("bk-107");
            check(catalogTable.getRowCount() == 1 && catalogTable.getValueAt(0, 0).equals("BK-107"),
                    "Catalog search filters by book ID");

            tabs.setSelectedIndex(1);
            var loans = tabs.getSelectedComponent();
            var filter = descendant(loans, JComboBox.class);
            var loanTable = descendant(loans, JTable.class);
            filter.setSelectedItem("Overdue");
            check(loanTable.getRowCount() == 1 && loanTable.getValueAt(0, 6).equals("Overdue"),
                    "Loan history filters overdue records");
            filter.setSelectedItem("Returned");
            check(loanTable.getRowCount() == 1 && loanTable.getValueAt(0, 6).equals("Returned"),
                    "Loan history filters returned records");
        });
    }

    private static <T extends Component> T descendant(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = descendant(child, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void runPhase(String phase, Path data) throws InterruptedException, IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                SubmissionCheck.class.getName(), phase, data.toString()).inheritIO().start();
        if (process.waitFor() != 0) {
            throw new AssertionError("Restart test failed in the " + phase + " process");
        }
    }

    private static void writeRecords(Path data) throws IOException {
        System.out.println("Campus Library restart checks");
        try (LibraryFile file = new LibraryFile(data)) {
            Library library = new Library(file, CLOCK);
            addDemoBook(library);
            library.issue(BOOK_ID, STUDENT_ID);
            library.issue(BOOK_ID, "ST-302");
            library.returnLoan(studentLoan(library, STUDENT_ID).id());
            check(Files.isRegularFile(data), "First JVM saves the new book, two loans, and one return");
        }
    }

    private static void readRecords(Path data) throws IOException {
        try (LibraryFile file = new LibraryFile(data)) {
            Library library = new Library(file, CLOCK);
            check(library.books().size() == 9 && library.loans().size() == 5,
                    "Second JVM loads the saved catalog and loan history");
            check(library.available(library.book(BOOK_ID)) == 1, "Available copies persist after restart");
            check(!studentLoan(library, STUDENT_ID).active() && studentLoan(library, "ST-302").active(),
                    "Returned and active loan states persist after restart");
        }
    }

    private static void addDemoBook(Library library) throws IOException {
        library.addBook(" bk-109 ", "Java: The Complete Reference", "Herbert Schildt", 2);
    }

    private static Library.Loan studentLoan(Library library, String studentId) {
        return library.loans().stream().filter(loan -> loan.studentId().equals(studentId)).findFirst().orElseThrow();
    }

    private static void rejects(String name, CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            check(true, name);
            return;
        }
        throw new AssertionError(name);
    }

    private static void check(boolean passed, String name) {
        if (!passed) {
            throw new AssertionError(name);
        }
        System.out.println("PASS " + name);
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
