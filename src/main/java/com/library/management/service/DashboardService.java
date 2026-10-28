package com.library.management.service;

import com.library.management.dto.DashboardStatsDto;
import com.library.management.model.BookIssue;
import com.library.management.repository.BookIssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final BookService bookService;
    private final MemberService memberService;
    private final BookIssueService bookIssueService;
    private final BookIssueRepository bookIssueRepository;

    public DashboardService(BookService bookService,
                            MemberService memberService,
                            BookIssueService bookIssueService,
                            BookIssueRepository bookIssueRepository) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.bookIssueService = bookIssueService;
        this.bookIssueRepository = bookIssueRepository;
    }

    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setTotalBooks(bookService.countTotalBooks());
        stats.setTotalTitles(bookService.countDistinctTitles());
        stats.setAvailableBooks(bookService.countAvailableBooks());
        stats.setIssuedBooks(bookIssueService.getActiveIssuedCount());
        stats.setRegisteredMembers(memberService.countTotalMembers());
        stats.setActiveMembers(memberService.countActiveMembers());
        stats.setOverdueIssues(bookIssueService.getOverdueCount());

        BigDecimal sumLateFees = bookIssueRepository.findAll().stream()
                .map(BookIssue::getLateFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalLateFees(sumLateFees);

        return stats;
    }

    public List<BookIssue> getRecentIssues() {
        return bookIssueService.getRecentIssues();
    }

    public List<BookIssue> getOverdueIssues() {
        return bookIssueService.getOverdueIssues();
    }
}
