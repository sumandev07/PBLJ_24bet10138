# Campus Library viva preparation

**Why did you separate the UI from the library rules?**  
Swing handles input and display. The `Library` class can enforce the same rules regardless of which screen calls it. This separation also lets the tests issue and return loans without opening a window.

**How is book availability calculated?**  
The app counts active loans for the selected book and subtracts that count from its total copies. A return updates the loan date, so the availability calculation includes the freed copy on the next refresh.

**What makes a loan overdue?**  
A loan is overdue if its return date is blank and today's date is later than its due date. On the due date itself, it is still active.

**How does the project prevent duplicate loans?**  
It checks the book ID and the normalized student ID against active loans for that book. A student can borrow the title again after returning the first copy.

**Why a TSV file?**  
The catalog is small and local. Tab-separated records are easy to inspect and Java can read and write them using its standard library. This project stores each book and each loan on a separate row.

**How does the app protect saved records?**  
It holds a file lock while open and saves a complete snapshot through a temporary file. When the filesystem supports atomic replacement, readers see either the old file or the complete new one.

**How did you check persistence?**  
One Java process saves a book, issues two loans, and returns one of them. A second Java process opens the same file and verifies the catalog, availability, and both loan states.
