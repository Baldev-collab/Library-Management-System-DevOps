package com.library.management;

import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.model.Book;
import com.library.management.repository.BookIssueRepository;
import com.library.management.repository.BookRepository;
import com.library.management.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookIssueRepository bookIssueRepository;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 3, "Sample description");
        sampleBook.setId(1L);
    }

    @Test
    @DisplayName("Should successfully create a book when ISBN is unique")
    void testCreateBook_Success() {
        when(bookRepository.existsByIsbn("978-0134685991")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        Book created = bookService.createBook(sampleBook);

        assertNotNull(created);
        assertEquals("Effective Java", created.getTitle());
        assertEquals(3, created.getAvailableCopies());
        verify(bookRepository, times(1)).save(sampleBook);
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when ISBN already exists")
    void testCreateBook_DuplicateIsbn_ThrowsException() {
        when(bookRepository.existsByIsbn("978-0134685991")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            bookService.createBook(sampleBook);
        });

        assertTrue(exception.getMessage().contains("978-0134685991"));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Should prevent deleting a book that has active loans")
    void testDeleteBook_WithActiveLoans_ThrowsException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookIssueRepository.existsByBookIdAndStatusIn(eq(1L), anySet())).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            bookService.deleteBook(1L);
        });

        assertTrue(exception.getMessage().contains("currently issued"));
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    @DisplayName("Should allow deleting a book when no active loans exist")
    void testDeleteBook_WithoutActiveLoans_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookIssueRepository.existsByBookIdAndStatusIn(eq(1L), anySet())).thenReturn(false);

        assertDoesNotThrow(() -> bookService.deleteBook(1L));
        verify(bookRepository, times(1)).delete(sampleBook);
    }
}
