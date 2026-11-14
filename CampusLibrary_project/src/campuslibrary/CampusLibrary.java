package campuslibrary;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public final class CampusLibrary {
    private static final Color NAVY = new Color(24, 39, 64);
    private static final Color MUTED = new Color(78, 93, 111);
    private static final Color BACKGROUND = new Color(242, 246, 250);
    private static final Color BORDER = new Color(210, 221, 232);
    private static final Color GREEN = new Color(0, 109, 91);
    private static final Color ERROR = new Color(164, 47, 48);

    private final Library library;
    private final JPanel content = new JPanel(new BorderLayout(0, 18));
    private final JLabel bookCount = new JLabel();
    private final JLabel activeCount = new JLabel();
    private final JLabel overdueCount = new JLabel();
    private final JLabel status = new JLabel("Ready");
    private final JTextField search = new JTextField(24);
    private final JComboBox<String> loanFilter = new JComboBox<>(
            new String[]{"All loans", "Active", "Overdue", "Returned"});
    private final DefaultTableModel bookModel = tableModel(
            new String[]{"Book ID", "Title", "Author", "Total", "Available"}, 3);
    private final DefaultTableModel loanModel = tableModel(
            new String[]{"Book ID", "Title", "Student ID", "Issued", "Due", "Returned", "Status"},
            Integer.MAX_VALUE);
    private final JTable bookTable = table(bookModel);
    private final JTable loanTable = table(loanModel);
    private final JButton issueButton = new JButton("Issue loan");
    private final JButton returnButton = new JButton("Return loan");
    private List<Library.Book> visibleBooks = List.of();
    private List<Library.Loan> visibleLoans = List.of();

    CampusLibrary(Library library) {
        this.library = library;
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        content.add(header(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catalog", catalogPanel());
        tabs.addTab("Loan history", loansPanel());
        content.add(tabs, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        status.setForeground(MUTED);
        content.add(status, BorderLayout.SOUTH);

        refresh();
    }

    JPanel content() {
        return content;
    }

    private void show(LibraryFile file) {
        JFrame frame = new JFrame("Campus Library");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(content);
        Timer dateRefresh = new Timer(60_000, event -> {
            LocalDate today = library.today();
            if (!today.equals(visibleDate)) {
                refresh();
            }
        });
        dateRefresh.start();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                dateRefresh.stop();
                try {
                    file.close();
                } catch (IOException exception) {
                    System.err.println("Could not release the data file: " + exception.getMessage());
                }
            }
        });
        frame.setMinimumSize(new Dimension(790, 485));
        frame.setSize(1040, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private LocalDate visibleDate;

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        JLabel title = new JLabel("Campus Library");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(NAVY);
        header.add(title, BorderLayout.NORTH);

        JPanel counts = new JPanel(new GridLayout(1, 3, 12, 0));
        counts.setOpaque(false);
        counts.add(counter("Book titles", bookCount, NAVY));
        counts.add(counter("Active loans", activeCount, GREEN));
        counts.add(counter("Overdue loans", overdueCount, ERROR));
        header.add(counts, BorderLayout.SOUTH);
        return header;
    }

    private static JPanel counter(String caption, JLabel number, Color color) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 14, 9, 14)));
        number.setFont(number.getFont().deriveFont(Font.BOLD, 23f));
        number.setForeground(color);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setForeground(MUTED);
        panel.add(number, BorderLayout.CENTER);
        panel.add(captionLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel catalogPanel() {
        JPanel panel = tablePanel(bookTable);
        JPanel toolbar = toolbar();
        JLabel searchLabel = new JLabel("Search catalog");
        searchLabel.setLabelFor(search);
        JButton add = new JButton("Add book");
        add.addActionListener(event -> addBook());
        issueButton.addActionListener(event -> issueLoan());
        bookTable.getSelectionModel().addListSelectionListener(event -> updateButtons());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshBooks();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshBooks();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshBooks();
            }
        });
        toolbar.add(searchLabel);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(search);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(add);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(issueButton);
        panel.add(toolbar, BorderLayout.NORTH);
        widths(bookTable, 85, 360, 260, 60, 80);
        return panel;
    }

    private JPanel loansPanel() {
        JPanel panel = tablePanel(loanTable);
        JPanel toolbar = toolbar();
        JLabel filterLabel = new JLabel("Show");
        filterLabel.setLabelFor(loanFilter);
        loanFilter.setMaximumSize(loanFilter.getPreferredSize());
        loanFilter.addActionListener(event -> refreshLoans());
        returnButton.addActionListener(event -> returnLoan());
        loanTable.getSelectionModel().addListSelectionListener(event -> updateButtons());
        toolbar.add(filterLabel);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(loanFilter);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(returnButton);
        panel.add(toolbar, BorderLayout.NORTH);
        widths(loanTable, 75, 245, 90, 90, 90, 90, 85);
        return panel;
    }

    private void refresh() {
        visibleDate = library.today();
        bookCount.setText(Integer.toString(library.books().size()));
        activeCount.setText(Long.toString(library.loans().stream().filter(Library.Loan::active).count()));
        overdueCount.setText(Long.toString(library.loans().stream().filter(loan -> loan.overdue(visibleDate)).count()));
        refreshBooks();
        refreshLoans();
    }

    private void refreshBooks() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        bookTable.clearSelection();
        visibleBooks = library.books().stream()
                .filter(book -> List.of(book.id(), book.title(), book.author()).stream()
                        .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(query)))
                .toList();
        bookModel.setRowCount(0);
        for (Library.Book book : visibleBooks) {
            bookModel.addRow(new Object[]{book.id(), book.title(), book.author(), book.copies(), library.available(book)});
        }
        updateButtons();
    }

    private void refreshLoans() {
        int filter = loanFilter.getSelectedIndex();
        LocalDate today = library.today();
        loanTable.clearSelection();
        visibleLoans = library.loans().stream()
                .filter(loan -> switch (filter) {
                    case 1 -> loan.active();
                    case 2 -> loan.overdue(today);
                    case 3 -> !loan.active();
                    default -> true;
                }).toList();
        loanModel.setRowCount(0);
        for (Library.Loan loan : visibleLoans) {
            String state = loan.overdue(today) ? "Overdue" : loan.active() ? "Active" : "Returned";
            loanModel.addRow(new Object[]{loan.bookId(), library.book(loan.bookId()).title(), loan.studentId(),
                    loan.issued(), loan.due(), loan.active() ? "" : loan.returned(), state});
        }
        updateButtons();
    }

    private static <T> T selected(JTable table, List<T> items) {
        int row = table.getSelectedRow();
        return row < 0 ? null : items.get(table.convertRowIndexToModel(row));
    }

    private void updateButtons() {
        Library.Book book = selected(bookTable, visibleBooks);
        Library.Loan loan = selected(loanTable, visibleLoans);
        issueButton.setEnabled(book != null && library.available(book) > 0);
        returnButton.setEnabled(loan != null && loan.active());
    }

    private void addBook() {
        JTextField id = new JTextField(20);
        JTextField title = new JTextField(20);
        JTextField author = new JTextField(20);
        JTextField copies = new JTextField("1", 20);
        JPanel form = form(new String[]{"Book ID", "Title", "Author", "Copies"}, id, title, author, copies);
        while (confirm(form, "Add a book")) {
            try {
                int count = Integer.parseInt(copies.getText().strip());
                library.addBook(id.getText(), title.getText(), author.getText(), count);
                search.setText("");
                refresh();
                status.setText("Added " + title.getText().strip() + ".");
                return;
            } catch (NumberFormatException exception) {
                showError("Enter a whole number from 1 to 999 for copies.");
            } catch (IllegalArgumentException | IOException exception) {
                showError(exception.getMessage());
            }
        }
    }

    private void issueLoan() {
        Library.Book book = selected(bookTable, visibleBooks);
        if (book == null) {
            return;
        }
        JTextField student = new JTextField(20);
        JPanel form = form(new String[]{"Student ID"}, student);
        while (confirm(form, "Issue " + book.id())) {
            try {
                library.issue(book.id(), student.getText());
                refresh();
                status.setText("Issued " + book.title() + " for " + Library.LOAN_DAYS + " days.");
                return;
            } catch (IllegalArgumentException | IOException exception) {
                showError(exception.getMessage());
            }
        }
    }

    private void returnLoan() {
        Library.Loan loan = selected(loanTable, visibleLoans);
        if (loan == null || !confirm(new JLabel("Return " + loan.bookId() + " from " + loan.studentId() + "?"),
                "Return loan")) {
            return;
        }
        try {
            library.returnLoan(loan.id());
            refresh();
            status.setText("Returned " + library.book(loan.bookId()).title() + ".");
        } catch (IllegalArgumentException | IOException exception) {
            showError(exception.getMessage());
        }
    }

    private boolean confirm(JComponent message, String title) {
        return JOptionPane.showConfirmDialog(content, message, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(content, message, "Campus Library", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel form(String[] labels, JTextField... inputs) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(4, 4, 4, 8);
        for (int row = 0; row < labels.length; row++) {
            constraints.gridy = row;
            constraints.gridx = 0;
            JLabel label = new JLabel(labels[row]);
            label.setLabelFor(inputs[row]);
            panel.add(label, constraints);
            constraints.gridx = 1;
            panel.add(inputs[row], constraints);
        }
        return panel;
    }

    private static JPanel tablePanel(JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel toolbar() {
        JPanel panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.LINE_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    private static void widths(JTable table, int... widths) {
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
    }

    private static DefaultTableModel tableModel(String[] columns, int firstIntegerColumn) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column >= firstIntegerColumn ? Integer.class : String.class;
            }
        };
    }

    public static void main(String[] args) {
        Path data = args.length == 0 ? Path.of("data", "library.tsv") : Path.of(args[0]);
        SwingUtilities.invokeLater(() -> {
            UIManager.put("swing.boldMetal", false);
            LibraryFile file = null;
            try {
                file = new LibraryFile(data);
                new CampusLibrary(new Library(file, Clock.systemDefaultZone())).show(file);
            } catch (IOException | RuntimeException exception) {
                if (file != null) {
                    try {
                        file.close();
                    } catch (IOException closeError) {
                        exception.addSuppressed(closeError);
                    }
                }
                System.err.println("Campus Library could not start: " + exception.getMessage());
                JOptionPane.showMessageDialog(null, exception.getMessage(), "Campus Library could not start",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
