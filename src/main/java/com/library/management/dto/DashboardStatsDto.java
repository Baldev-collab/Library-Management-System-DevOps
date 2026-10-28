package com.library.management.dto;

import java.math.BigDecimal;

public class DashboardStatsDto {

    private long totalBooks;
    private long totalTitles;
    private long availableBooks;
    private long issuedBooks;
    private long registeredMembers;
    private long activeMembers;
    private long overdueIssues;
    private BigDecimal totalLateFees;

    public DashboardStatsDto() {
        this.totalLateFees = BigDecimal.ZERO;
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(long totalBooks) {
        this.totalBooks = totalBooks;
    }

    public long getTotalTitles() {
        return totalTitles;
    }

    public void setTotalTitles(long totalTitles) {
        this.totalTitles = totalTitles;
    }

    public long getAvailableBooks() {
        return availableBooks;
    }

    public void setAvailableBooks(long availableBooks) {
        this.availableBooks = availableBooks;
    }

    public long getIssuedBooks() {
        return issuedBooks;
    }

    public void setIssuedBooks(long issuedBooks) {
        this.issuedBooks = issuedBooks;
    }

    public long getRegisteredMembers() {
        return registeredMembers;
    }

    public void setRegisteredMembers(long registeredMembers) {
        this.registeredMembers = registeredMembers;
    }

    public long getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(long activeMembers) {
        this.activeMembers = activeMembers;
    }

    public long getOverdueIssues() {
        return overdueIssues;
    }

    public void setOverdueIssues(long overdueIssues) {
        this.overdueIssues = overdueIssues;
    }

    public BigDecimal getTotalLateFees() {
        return totalLateFees;
    }

    public void setTotalLateFees(BigDecimal totalLateFees) {
        this.totalLateFees = totalLateFees;
    }
}
