package campuslibrary;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class Library {
    static final int LOAN_DAYS = 14;

    record Book(String id, String title, String author, int copies) {}

    record Loan(String id, String bookId, String studentId, LocalDate issued,
                LocalDate due, LocalDate returned) {
        boolean active() {
            return returned == null;
        }

        boolean overdue(LocalDate today) {
            return active() && due.isBefore(today);
        }
    }

    record Snapshot(List<Book> books, List<Loan> loans) {
        Snapshot {
            books = List.copyOf(books);
            loans = List.copyOf(loans);
        }
    }

    private final LibraryFile file;
    private final Clock clock;
    private Snapshot state;

    Library(LibraryFile file, Clock clock) throws IOException {
        this.file = file;
        this.clock = clock;
        state = file.load();
        if (state == null) {
            commit(demo(today()));
        }
    }

    LocalDate today() {
        return LocalDate.now(clock);
    }

    List<Book> books() {
        return state.books();
    }

    List<Loan> loans() {
        return state.loans();
    }

    Book book(String id) {
        return books().stream()
                .filter(book -> book.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Book ID not found."));
    }

    int available(Book book) {
        long checkedOut = loans().stream()
                .filter(loan -> loan.active() && loan.bookId().equals(book.id()))
                .count();
        return book.copies() - (int) checkedOut;
    }

    void addBook(String id, String title, String author, int copies) throws IOException {
        String cleanId = identifier(id, "Book ID");
        if (books().stream().anyMatch(book -> book.id().equals(cleanId))) {
            throw new IllegalArgumentException("A book with this ID already exists.");
        }
        if (copies < 1 || copies > 999) {
            throw new IllegalArgumentException("Enter a copy count from 1 to 999.");
        }
        Book book = new Book(cleanId, field(title, "Title"), field(author, "Author"), copies);
        List<Book> updated = new ArrayList<>(books());
        updated.add(book);
        commit(new Snapshot(updated, loans()));
    }

    void issue(String bookId, String studentId) throws IOException {
        Book book = book(bookId);
        String student = identifier(studentId, "Student ID");
        if (available(book) == 0) {
            throw new IllegalArgumentException("All copies of this book are on loan.");
        }
        if (loans().stream().anyMatch(loan -> loan.active() && loan.bookId().equals(bookId)
                && loan.studentId().equals(student))) {
            throw new IllegalArgumentException("This student already has an active loan for this book.");
        }
        LocalDate issued = today();
        List<Loan> updated = new ArrayList<>(loans());
        updated.add(new Loan(UUID.randomUUID().toString(), bookId, student, issued,
                issued.plusDays(LOAN_DAYS), null));
        commit(new Snapshot(books(), updated));
    }

    void returnLoan(String id) throws IOException {
        List<Loan> updated = new ArrayList<>(loans());
        for (int index = 0; index < updated.size(); index++) {
            Loan loan = updated.get(index);
            if (loan.id().equals(id) && loan.active()) {
                LocalDate returned = today().isBefore(loan.issued()) ? loan.issued() : today();
                updated.set(index, new Loan(loan.id(), loan.bookId(), loan.studentId(),
                        loan.issued(), loan.due(), returned));
                commit(new Snapshot(books(), updated));
                return;
            }
        }
        throw new IllegalArgumentException("Select an active loan to return.");
    }

    private void commit(Snapshot next) throws IOException {
        file.save(next);
        state = next;
    }

    private static String field(String value, String name) {
        String cleaned = value.strip();
        if (cleaned.isBlank() || cleaned.length() > 120
                || cleaned.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " must contain 1 to 120 printable characters.");
        }
        return cleaned;
    }

    private static String identifier(String value, String name) {
        String cleaned = value.strip();
        if (!cleaned.matches("[A-Za-z0-9-]{1,24}")) {
            throw new IllegalArgumentException(name + " must use 1 to 24 letters, digits or hyphens.");
        }
        return cleaned.toUpperCase(java.util.Locale.ROOT);
    }

    private static Snapshot demo(LocalDate today) {
        List<Book> books = List.of(
                new Book("BK-101", "Effective Java", "Joshua Bloch", 3),
                new Book("BK-102", "Clean Code", "Robert C. Martin", 2),
                new Book("BK-103", "Introduction to Algorithms", "Thomas H. Cormen et al.", 2),
                new Book("BK-104", "Head First Design Patterns", "Eric Freeman and Elisabeth Robson", 3),
                new Book("BK-105", "The Pragmatic Programmer", "David Thomas and Andrew Hunt", 2),
                new Book("BK-106", "Computer Networking: A Top-Down Approach", "James Kurose and Keith Ross", 2),
                new Book("BK-107", "Database System Concepts", "Abraham Silberschatz et al.", 1),
                new Book("BK-108", "Operating System Concepts", "Abraham Silberschatz et al.", 2)
        );
        return new Snapshot(books, List.of(
                new Loan("DEMO-01", "BK-101", "ST-202", today.minusDays(4),
                        today.plusDays(10), null),
                new Loan("DEMO-02", "BK-107", "ST-205", today.minusDays(17),
                        today.minusDays(3), null),
                new Loan("DEMO-03", "BK-104", "ST-207", today.minusDays(12),
                        today.plusDays(2), today.minusDays(1))
        ));
    }
}
