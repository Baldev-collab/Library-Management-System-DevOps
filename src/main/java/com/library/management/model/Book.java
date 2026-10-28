package com.library.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(?=(?:\\D*\\d){10}(?:(?:\\D*\\d){3})?$)[\\d-]+$", message = "Invalid ISBN format (must be valid 10 or 13-digit ISBN)")
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @NotBlank(message = "Book title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Author name is required")
    @Size(max = 255, message = "Author cannot exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String author;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String category;

    @Min(value = 1, message = "Total copies must be at least 1")
    @Column(name = "total_copies", nullable = false)
    private int totalCopies = 1;

    @Min(value = 0, message = "Available copies cannot be negative")
    @Column(name = "available_copies", nullable = false)
    private int availableCopies = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String status = "AVAILABLE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Book() {
    }

    public Book(String isbn, String title, String author, String category, int totalCopies, String description) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
        this.description = description;
        this.status = totalCopies > 0 ? "AVAILABLE" : "OUT_OF_STOCK";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        updateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        updateStatus();
    }

    public void updateStatus() {
        if (this.availableCopies <= 0) {
            this.status = "OUT_OF_STOCK";
        } else {
            this.status = "AVAILABLE";
        }
    }

    public boolean isAvailable() {
        return this.availableCopies > 0;
    }

    public void decrementAvailable() {
        if (this.availableCopies <= 0) {
            throw new IllegalStateException("Cannot issue book: no available copies remain.");
        }
        this.availableCopies--;
        updateStatus();
    }

    public void incrementAvailable() {
        if (this.availableCopies < this.totalCopies) {
            this.availableCopies++;
        }
        updateStatus();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn != null ? isbn.trim() : null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author != null ? author.trim() : null;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim() : null;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
        updateStatus();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
