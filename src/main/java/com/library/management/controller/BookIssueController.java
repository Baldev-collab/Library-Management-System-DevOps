package com.library.management.controller;

import com.library.management.dto.IssueBookRequestDto;
import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.BookIssue;
import com.library.management.service.BookIssueService;
import com.library.management.service.BookService;
import com.library.management.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/issues")
public class BookIssueController {

    private final BookIssueService bookIssueService;
    private final BookService bookService;
    private final MemberService memberService;

    public BookIssueController(BookIssueService bookIssueService,
                               BookService bookService,
                               MemberService memberService) {
        this.bookIssueService = bookIssueService;
        this.bookService = bookService;
        this.memberService = memberService;
    }

    @GetMapping
    public String listIssues(@RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "status", required = false) String status,
                             Model model) {
        model.addAttribute("issues", bookIssueService.searchIssues(query, status));
        model.addAttribute("selectedQuery", query);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeNav", "issues");
        model.addAttribute("lateFeePerDay", bookIssueService.getLateFeePerDay());
        return "issues/list";
    }

    @GetMapping("/new")
    public String showIssueForm(@RequestParam(value = "bookId", required = false) Long preselectedBookId,
                                @RequestParam(value = "memberId", required = false) Long preselectedMemberId,
                                Model model) {
        IssueBookRequestDto request = new IssueBookRequestDto();
        if (preselectedBookId != null) {
            request.setBookId(preselectedBookId);
        }
        if (preselectedMemberId != null) {
            request.setMemberId(preselectedMemberId);
        }
        request.setIssueDate(LocalDate.now());
        request.setDueDate(LocalDate.now().plusDays(bookIssueService.getDefaultLoanPeriodDays()));

        model.addAttribute("issueRequest", request);
        model.addAttribute("availableBooks", bookService.getAvailableBooksForIssue());
        model.addAttribute("activeMembers", memberService.getActiveMembers());
        model.addAttribute("activeNav", "issues");
        return "issues/form";
    }

    @PostMapping("/save")
    public String saveIssue(@Valid @ModelAttribute("issueRequest") IssueBookRequestDto request,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("availableBooks", bookService.getAvailableBooksForIssue());
            model.addAttribute("activeMembers", memberService.getActiveMembers());
            model.addAttribute("activeNav", "issues");
            return "issues/form";
        }

        try {
            BookIssue issue = bookIssueService.issueBook(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Book '" + issue.getBook().getTitle() + "' was successfully issued to " +
                    issue.getMember().getName() + " (Due: " + issue.getDueDate() + ").");
            return "redirect:/issues";
        } catch (BusinessRuleException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("availableBooks", bookService.getAvailableBooksForIssue());
            model.addAttribute("activeMembers", memberService.getActiveMembers());
            model.addAttribute("activeNav", "issues");
            return "issues/form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error issuing book: " + e.getMessage());
            model.addAttribute("availableBooks", bookService.getAvailableBooksForIssue());
            model.addAttribute("activeMembers", memberService.getActiveMembers());
            model.addAttribute("activeNav", "issues");
            return "issues/form";
        }
    }

    @PostMapping("/return/{id}")
    public String returnBook(@PathVariable("id") Long id,
                             @RequestParam(value = "returnDate", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                             @RequestParam(value = "notes", required = false) String notes,
                             RedirectAttributes redirectAttributes) {
        try {
            LocalDate actualDate = (returnDate != null) ? returnDate : LocalDate.now();
            BookIssue issue = bookIssueService.returnBook(id, actualDate, notes);
            String message = "Book '" + issue.getBook().getTitle() + "' returned successfully by " + issue.getMember().getName() + ".";
            if (issue.getLateFee() != null && issue.getLateFee().compareTo(java.math.BigDecimal.ZERO) > 0) {
                message += " Overdue late fee assessed: $" + issue.getLateFee();
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not complete book return: " + e.getMessage());
        }
        return "redirect:/issues";
    }
}
