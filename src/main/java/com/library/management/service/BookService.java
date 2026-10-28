package com.library.management.service;

import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Book;
import com.library.management.repository.BookIssueRepository;
import com.library.management.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookIssueRepository bookIssueRepository;

    public BookService(BookRepository bookRepository, BookIssueRepository bookIssueRepository) {
        this.bookRepository = bookRepository;
        this.bookIssueRepository = bookIssueRepository;
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Book> searchBooks(String query, String category, String status) {
        return bookRepository.searchBooks(
                (query != null && !query.isBlank()) ? query.trim() : null,
                (category != null && !category.isBlank()) ? category.trim() : null,
                (status != null && !status.isBlank()) ? status.trim() : null
        );
    }

    @Transactional(readOnly = true)
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Book getBookOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
    }

    public Book createBook(Book book) {
        if (book.getIsbn() != null && bookRepository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateResourceException("A book with ISBN '" + book.getIsbn() + "' already exists.");
        }

        // Initialize copies safely
        if (book.getTotalCopies() < 1) {
            book.setTotalCopies(1);
        }
        if (book.getAvailableCopies() <= 0 || book.getAvailableCopies() > book.getTotalCopies()) {
            book.setAvailableCopies(book.getTotalCopies());
        }

        book.updateStatus();
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book updatedBookData) {
        Book existingBook = getBookOrThrow(id);

        if (updatedBookData.getIsbn() != null &&
            bookRepository.existsByIsbnAndIdNot(updatedBookData.getIsbn(), id)) {
            throw new DuplicateResourceException("Another book already exists with ISBN: " + updatedBookData.getIsbn());
        }

        int activeLoans = existingBook.getTotalCopies() - existingBook.getAvailableCopies();
        if (updatedBookData.getTotalCopies() < activeLoans) {
            throw new BusinessRuleException("Total copies (" + updatedBookData.getTotalCopies() +
                    ") cannot be less than currently issued copies (" + activeLoans + ").");
        }

        existingBook.setIsbn(updatedBookData.getIsbn());
        existingBook.setTitle(updatedBookData.getTitle());
        existingBook.setAuthor(updatedBookData.getAuthor());
        existingBook.setCategory(updatedBookData.getCategory());
        existingBook.setDescription(updatedBookData.getDescription());

        // Update total and adjust available accordingly
        int diff = updatedBookData.getTotalCopies() - existingBook.getTotalCopies();
        existingBook.setTotalCopies(updatedBookData.getTotalCopies());
        existingBook.setAvailableCopies(Math.max(0, existingBook.getAvailableCopies() + diff));
        existingBook.updateStatus();

        return bookRepository.save(existingBook);
    }

    public void deleteBook(Long id) {
        Book book = getBookOrThrow(id);
        boolean hasActiveLoans = bookIssueRepository.existsByBookIdAndStatusIn(id, Set.of("ISSUED", "OVERDUE"));
        if (hasActiveLoans) {
            throw new BusinessRuleException("Cannot delete book '" + book.getTitle() + "' because it is currently issued to a member.");
        }
        bookRepository.delete(book);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return bookRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<Book> getAvailableBooksForIssue() {
        return bookRepository.findByAvailableCopiesGreaterThan(0);
    }

    @Transactional(readOnly = true)
    public long countTotalBooks() {
        return bookRepository.sumTotalCopies();
    }

    @Transactional(readOnly = true)
    public long countAvailableBooks() {
        return bookRepository.sumAvailableCopies();
    }

    @Transactional(readOnly = true)
    public long countDistinctTitles() {
        return bookRepository.count();
    }
}
