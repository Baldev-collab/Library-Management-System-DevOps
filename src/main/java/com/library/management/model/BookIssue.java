package com.library.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "book_issues")
public class BookIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Book selection is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @NotNull(message = "Member selection is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull(message = "Issue date is required")
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(nullable = false, length = 50)
    private String status = "ISSUED";

    @Column(name = "late_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public BookIssue() {
    }

    public BookIssue(Book book, Member member, LocalDate issueDate, LocalDate dueDate, String notes) {
        this.book = book;
        this.member = member;
        this.issueDate = issueDate != null ? issueDate : LocalDate.now();
        this.dueDate = dueDate;
        this.notes = notes;
        this.status = "ISSUED";
        this.lateFee = BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.issueDate == null) {
            this.issueDate = LocalDate.now();
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "ISSUED";
        }
        if (this.lateFee == null) {
            this.lateFee = BigDecimal.ZERO;
        }
    }

    public boolean isCurrentlyOverdue(LocalDate referenceDate) {
        if ("RETURNED".equalsIgnoreCase(this.status)) {
            return false;
        }
        LocalDate dateToCheck = referenceDate != null ? referenceDate : LocalDate.now();
        return this.dueDate != null && dateToCheck.isAfter(this.dueDate);
    }

    public long calculateDaysOverdue(LocalDate referenceDate) {
        LocalDate checkDate = (this.returnDate != null) ? this.returnDate : (referenceDate != null ? referenceDate : LocalDate.now());
        if (this.dueDate == null || !checkDate.isAfter(this.dueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(this.dueDate, checkDate);
    }

    public BigDecimal computeLateFee(BigDecimal feePerDay, LocalDate referenceDate) {
        long daysOverdue = calculateDaysOverdue(referenceDate);
        if (daysOverdue <= 0 || feePerDay == null) {
            return BigDecimal.ZERO;
        }
        return feePerDay.multiply(BigDecimal.valueOf(daysOverdue));
    }

    public void markReturned(LocalDate actualReturnDate, BigDecimal feePerDay) {
        this.returnDate = actualReturnDate != null ? actualReturnDate : LocalDate.now();
        this.status = "RETURNED";
        this.lateFee = computeLateFee(feePerDay, this.returnDate);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
