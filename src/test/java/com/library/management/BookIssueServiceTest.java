package com.library.management;

import com.library.management.dto.IssueBookRequestDto;
import com.library.management.exception.BusinessRuleException;
import com.library.management.model.Book;
import com.library.management.model.BookIssue;
import com.library.management.model.Member;
import com.library.management.repository.BookIssueRepository;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.BookIssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookIssueServiceTest {

    @Mock
    private BookIssueRepository bookIssueRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private BookIssueService bookIssueService;

    private Book testBook;
    private Member testMember;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookIssueService, "lateFeePerDay", new BigDecimal("2.00"));
        ReflectionTestUtils.setField(bookIssueService, "defaultLoanPeriodDays", 14);

        testBook = new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 3, "Test book");
        testBook.setId(10L);
        testBook.setAvailableCopies(2);

        testMember = new Member("LIB-MEM-001", "Alice Johnson", "alice@example.com", "555-0101", "123 St", LocalDate.now());
        testMember.setId(20L);
        testMember.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should successfully issue a book and decrement available copy count")
    void testIssueBook_Success() {
        IssueBookRequestDto request = new IssueBookRequestDto();
        request.setBookId(10L);
        request.setMemberId(20L);
        request.setIssueDate(LocalDate.now());
        request.setDueDate(LocalDate.now().plusDays(14));
        request.setNotes("Coursework loan");

        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(memberRepository.findById(20L)).thenReturn(Optional.of(testMember));
        when(bookIssueRepository.save(any(BookIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookIssue result = bookIssueService.issueBook(request);

        assertNotNull(result);
        assertEquals(1, testBook.getAvailableCopies(), "Available copies should decrement from 2 to 1");
        assertEquals("ISSUED", result.getStatus());
        verify(bookRepository, times(1)).save(testBook);
        verify(bookIssueRepository, times(1)).save(any(BookIssue.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when attempting to issue out-of-stock book")
    void testIssueBook_UnavailableCopies_ThrowsException() {
        testBook.setAvailableCopies(0);
        testBook.setStatus("OUT_OF_STOCK");

        IssueBookRequestDto request = new IssueBookRequestDto();
        request.setBookId(10L);
        request.setMemberId(20L);

        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            bookIssueService.issueBook(request);
        });

        assertTrue(ex.getMessage().contains("No copies are currently available"));
        verify(bookIssueRepository, never()).save(any(BookIssue.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when borrower account is not ACTIVE")
    void testIssueBook_InactiveMember_ThrowsException() {
        testMember.setStatus("SUSPENDED");

        IssueBookRequestDto request = new IssueBookRequestDto();
        request.setBookId(10L);
        request.setMemberId(20L);

        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(memberRepository.findById(20L)).thenReturn(Optional.of(testMember));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            bookIssueService.issueBook(request);
        });

        assertTrue(ex.getMessage().contains("SUSPENDED"));
        verify(bookIssueRepository, never()).save(any(BookIssue.class));
    }

    @Test
    @DisplayName("Should return book on time with zero late fee and increment availability")
    void testReturnBook_OnTime_ZeroFee() {
        BookIssue issue = new BookIssue(testBook, testMember, LocalDate.now().minusDays(7), LocalDate.now().plusDays(7), "Loan");
        issue.setId(100L);

        when(bookIssueRepository.findById(100L)).thenReturn(Optional.of(issue));
        when(bookIssueRepository.save(any(BookIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int initialAvailable = testBook.getAvailableCopies();
        BookIssue returned = bookIssueService.returnBook(100L, LocalDate.now(), "Returned in good condition");

        assertEquals("RETURNED", returned.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(returned.getLateFee()));
        assertEquals(initialAvailable + 1, testBook.getAvailableCopies());
        verify(bookRepository, times(1)).save(testBook);
    }

    @Test
    @DisplayName("Should calculate late fee accurately when book returned past due date")
    void testReturnBook_Overdue_CalculatesLateFee() {
        // Due 5 days ago (fee rate: $2.00 / day -> total $10.00)
        LocalDate issueDate = LocalDate.now().minusDays(19);
        LocalDate dueDate = LocalDate.now().minusDays(5);
        LocalDate actualReturnDate = LocalDate.now();

        BookIssue issue = new BookIssue(testBook, testMember, issueDate, dueDate, "Late loan");
        issue.setId(200L);

        when(bookIssueRepository.findById(200L)).thenReturn(Optional.of(issue));
        when(bookIssueRepository.save(any(BookIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookIssue returned = bookIssueService.returnBook(200L, actualReturnDate, "Late return with fee");

        assertEquals("RETURNED", returned.getStatus());
        assertEquals(new BigDecimal("10.00"), returned.getLateFee(), "Late fee should be 5 days * $2.00 = $10.00");
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when attempting to return an already returned book")
    void testReturnBook_AlreadyReturned_ThrowsException() {
        BookIssue issue = new BookIssue(testBook, testMember, LocalDate.now().minusDays(10), LocalDate.now().minusDays(2), "Loan");
        issue.setId(300L);
        issue.setStatus("RETURNED");
        issue.setReturnDate(LocalDate.now().minusDays(2));

        when(bookIssueRepository.findById(300L)).thenReturn(Optional.of(issue));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            bookIssueService.returnBook(300L, LocalDate.now(), null);
        });

        assertTrue(ex.getMessage().contains("already been marked as returned"));
    }
}
