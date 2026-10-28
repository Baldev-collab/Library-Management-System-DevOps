package com.library.management.controller;

import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.model.Book;
import com.library.management.repository.BookIssueRepository;
import com.library.management.service.BookService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final BookIssueRepository bookIssueRepository;

    public BookController(BookService bookService, BookIssueRepository bookIssueRepository) {
        this.bookService = bookService;
        this.bookIssueRepository = bookIssueRepository;
    }

    @GetMapping
    public String listBooks(@RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "category", required = false) String category,
                            @RequestParam(value = "status", required = false) String status,
                            Model model) {
        model.addAttribute("books", bookService.searchBooks(query, category, status));
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("selectedQuery", query);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeNav", "books");
        return "books/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Book book = new Book();
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
        model.addAttribute("book", book);
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("activeNav", "books");
        return "books/form";
    }

    @PostMapping("/save")
    public String saveBook(@Valid @ModelAttribute("book") Book book,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", bookService.getAllCategories());
            model.addAttribute("activeNav", "books");
            return "books/form";
        }

        try {
            bookService.createBook(book);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + book.getTitle() + "' successfully added to inventory!");
            return "redirect:/books";
        } catch (DuplicateResourceException e) {
            result.rejectValue("isbn", "error.book", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", bookService.getAllCategories());
            model.addAttribute("activeNav", "books");
            return "books/form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error adding book: " + e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", bookService.getAllCategories());
            model.addAttribute("activeNav", "books");
            return "books/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Book book = bookService.getBookOrThrow(id);
        model.addAttribute("book", book);
        model.addAttribute("isEdit", true);
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("activeNav", "books");
        return "books/form";
    }

    @PostMapping("/update/{id}")
    public String updateBook(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("book") Book book,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            book.setId(id);
            model.addAttribute("isEdit", true);
            model.addAttribute("categories", bookService.getAllCategories());
            model.addAttribute("activeNav", "books");
            return "books/form";
        }

        try {
            bookService.updateBook(id, book);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + book.getTitle() + "' updated successfully!");
            return "redirect:/books";
        } catch (DuplicateResourceException e) {
            result.rejectValue("isbn", "error.book", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("categories", bookService.getAllCategories());
            model.addAttribute("activeNav", "books");
            return "books/form";
        } catch (BusinessRuleException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("categories", bookService.getAllCategories());
            model.addAttribute("activeNav", "books");
            return "books/form";
        }
    }

    @GetMapping("/view/{id}")
    public String viewBook(@PathVariable("id") Long id, Model model) {
        Book book = bookService.getBookOrThrow(id);
        model.addAttribute("book", book);
        model.addAttribute("issues", bookIssueRepository.findByBookId(id));
        model.addAttribute("activeNav", "books");
        return "books/view";
    }

    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Book book = bookService.getBookOrThrow(id);
            String title = book.getTitle();
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("successMessage", "Book '" + title + "' removed successfully.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete book: " + e.getMessage());
        }
        return "redirect:/books";
    }
}
