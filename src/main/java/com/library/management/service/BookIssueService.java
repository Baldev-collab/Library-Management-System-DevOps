package com.library.management.service;

import com.library.management.dto.IssueBookRequestDto;
import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Book;
import com.library.management.model.BookIssue;
import com.library.management.model.Member;
import com.library.management.repository.BookIssueRepository;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class BookIssueService {

    private final BookIssueRepository bookIssueRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    @Value("${library.late-fee-per-day:2.00}")
    private BigDecimal lateFeePerDay;

    @Value("${library.default-loan-period-days:14}")
    private int defaultLoanPeriodDays;

    public BookIssueService(BookIssueRepository bookIssueRepository,
                            BookRepository bookRepository,
                            MemberRepository memberRepository) {
        this.bookIssueRepository = bookIssueRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<BookIssue> getAllIssues() {
        refreshOverdueStatuses();
        return bookIssueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<BookIssue> searchIssues(String query, String status) {
        refreshOverdueStatuses();
        return bookIssueRepository.searchIssues(
                (query != null && !query.isBlank()) ? query.trim() : null,
                (status != null && !status.isBlank()) ? status.trim() : null
        );
    }

    @Transactional(readOnly = true)
    public Optional<BookIssue> getIssueById(Long id) {
        return bookIssueRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public BookIssue getIssueOrThrow(Long id) {
        return bookIssueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book issue record not found with ID: " + id));
    }

    public BookIssue issueBook(IssueBookRequestDto request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + request.getBookId()));

        if (!book.isAvailable() || book.getAvailableCopies() <= 0) {
            throw new BusinessRuleException("Cannot issue book '" + book.getTitle() + "'. No copies are currently available in the library.");
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with ID: " + request.getMemberId()));

        if (!member.isActive()) {
            throw new BusinessRuleException("Cannot issue book to member '" + member.getName() + "' because the membership status is " + member.getStatus() + ".");
        }

        LocalDate issueDate = request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now();
        LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : issueDate.plusDays(defaultLoanPeriodDays);

        if (dueDate.isBefore(issueDate)) {
            throw new BusinessRuleException("Due date cannot be before the issue date.");
        }

        // Decrement available copies
        book.decrementAvailable();
        bookRepository.save(book);

        BookIssue bookIssue = new BookIssue(book, member, issueDate, dueDate, request.getNotes());
        return bookIssueRepository.save(bookIssue);
    }

    public BookIssue returnBook(Long issueId, LocalDate actualReturnDate, String notes) {
        BookIssue issue = getIssueOrThrow(issueId);

        if ("RETURNED".equalsIgnoreCase(issue.getStatus())) {
            throw new BusinessRuleException("This book has already been marked as returned on " + issue.getReturnDate() + ".");
        }

        LocalDate returnDate = (actualReturnDate != null) ? actualReturnDate : LocalDate.now();
        if (returnDate.isBefore(issue.getIssueDate())) {
            throw new BusinessRuleException("Return date cannot be earlier than the issue date (" + issue.getIssueDate() + ").");
        }

        // Increment book availability
        Book book = issue.getBook();
        book.incrementAvailable();
        bookRepository.save(book);

        // Update issue record
        issue.markReturned(returnDate, lateFeePerDay);
        if (notes != null && !notes.isBlank()) {
            String existingNotes = issue.getNotes() != null ? issue.getNotes() + " | " : "";
            issue.setNotes(existingNotes + notes.trim());
        }

        return bookIssueRepository.save(issue);
    }

    public void refreshOverdueStatuses() {
        LocalDate today = LocalDate.now();
        List<BookIssue> overdueCandidates = bookIssueRepository.findByStatusInAndDueDateBefore(Set.of("ISSUED"), today);
        for (BookIssue issue : overdueCandidates) {
            issue.setStatus("OVERDUE");
            issue.setLateFee(issue.computeLateFee(lateFeePerDay, today));
            bookIssueRepository.save(issue);
        }
    }

    @Transactional(readOnly = true)
    public long getActiveIssuedCount() {
        return bookIssueRepository.countByStatusIn(Set.of("ISSUED", "OVERDUE"));
    }

    @Transactional(readOnly = true)
    public long getOverdueCount() {
        LocalDate today = LocalDate.now();
        return bookIssueRepository.findByStatusInAndDueDateBefore(Set.of("ISSUED", "OVERDUE"), today).size();
    }

    @Transactional(readOnly = true)
    public List<BookIssue> getRecentIssues() {
        refreshOverdueStatuses();
        return bookIssueRepository.findTop5ByOrderByIssueDateDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<BookIssue> getOverdueIssues() {
        refreshOverdueStatuses();
        return bookIssueRepository.findByStatusInAndDueDateBefore(Set.of("ISSUED", "OVERDUE"), LocalDate.now());
    }

    public BigDecimal getLateFeePerDay() {
        return lateFeePerDay;
    }

    public int getDefaultLoanPeriodDays() {
        return defaultLoanPeriodDays;
    }
}
