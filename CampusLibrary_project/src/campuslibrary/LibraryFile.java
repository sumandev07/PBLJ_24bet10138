package campuslibrary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LibraryFile implements AutoCloseable {
    private static final String HEADER = "CAMPUS_LIBRARY_V1";

    private final Path data;
    private final FileChannel lockChannel;
    private final FileLock lock;

    LibraryFile(Path data) throws IOException {
        this.data = data.toAbsolutePath();
        Files.createDirectories(this.data.getParent());
        Path lockPath = this.data.resolveSibling(this.data.getFileName() + ".lock");
        lockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            lock = lockChannel.tryLock();
            if (lock == null) {
                throw new IOException("Campus Library is already open for this data folder.");
            }
        } catch (IOException | RuntimeException exception) {
            lockChannel.close();
            if (exception instanceof OverlappingFileLockException) {
                throw new IOException("Campus Library is already open for this data folder.", exception);
            }
            throw exception;
        }
    }

    Library.Snapshot load() throws IOException {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(data);
        } catch (NoSuchFileException exception) {
            return null;
        }
        try (reader) {
            if (!HEADER.equals(reader.readLine())) {
                throw new IOException("Unrecognized file format in " + data);
            }
            List<Library.Book> books = new ArrayList<>();
            List<Library.Loan> loans = new ArrayList<>();
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] cells = line.split("\t", -1);
                try {
                    if (cells.length == 5 && cells[0].equals("BOOK")) {
                        books.add(new Library.Book(cells[1], cells[2], cells[3], Integer.parseInt(cells[4])));
                    } else if (cells.length == 7 && cells[0].equals("LOAN")) {
                        LocalDate returned = cells[6].isEmpty() ? null : LocalDate.parse(cells[6]);
                        loans.add(new Library.Loan(cells[1], cells[2], cells[3],
                                LocalDate.parse(cells[4]), LocalDate.parse(cells[5]), returned));
                    } else {
                        throw new IllegalArgumentException("Unexpected record");
                    }
                } catch (IllegalArgumentException | DateTimeParseException exception) {
                    throw new IOException("Invalid record at line " + lineNumber + " in " + data, exception);
                }
            }
            validate(books, loans);
            return new Library.Snapshot(books, loans);
        }
    }

    private void validate(List<Library.Book> books, List<Library.Loan> loans) throws IOException {
        Map<String, Integer> remainingCopies = new HashMap<>();
        for (Library.Book book : books) {
            if (book.id().isBlank() || book.title().isBlank() || book.author().isBlank()
                    || book.copies() < 1 || remainingCopies.putIfAbsent(book.id(), book.copies()) != null) {
                throw new IOException("Invalid or duplicate book in " + data);
            }

        }
        Set<String> ids = new HashSet<>();
        Map<String, Set<String>> borrowers = new HashMap<>();
        for (Library.Loan loan : loans) {
            if (loan.id().isBlank() || loan.studentId().isBlank() || !ids.add(loan.id())
                    || !remainingCopies.containsKey(loan.bookId()) || loan.due().isBefore(loan.issued())
                    || (!loan.active() && loan.returned().isBefore(loan.issued()))) {
                throw new IOException("Invalid or duplicate loan in " + data);
            }
            if (loan.active()) {
                Set<String> students = borrowers.computeIfAbsent(loan.bookId(), id -> new HashSet<>());
                if (!students.add(loan.studentId()) || remainingCopies.merge(loan.bookId(), -1, Integer::sum) < 0) {
                    throw new IOException("Active loans conflict with book inventory in " + data);
                }
            }
        }
    }

    void save(Library.Snapshot snapshot) throws IOException {
        Path temporary = Files.createTempFile(data.getParent(), "library-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
                writeLine(writer, HEADER);
                for (Library.Book book : snapshot.books()) {
                    writeLine(writer, "BOOK", book.id(), book.title(), book.author(), Integer.toString(book.copies()));
                }
                for (Library.Loan loan : snapshot.loans()) {
                    String returned = loan.active() ? "" : loan.returned().toString();
                    writeLine(writer, "LOAN", loan.id(), loan.bookId(), loan.studentId(),
                            loan.issued().toString(), loan.due().toString(), returned);
                }
            }
            try {
                Files.move(temporary, data, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, data, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeLine(BufferedWriter writer, String... cells) throws IOException {
        writer.write(String.join("\t", cells));
        writer.newLine();
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            lockChannel.close();
        }
    }
}
