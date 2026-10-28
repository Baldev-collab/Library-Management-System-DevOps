package com.library.management.repository;

import com.library.management.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Query("SELECT b FROM Book b WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR b.category = :category) AND " +
           "(:status IS NULL OR :status = '' OR b.status = :status) " +
           "ORDER BY b.title ASC")
    List<Book> searchBooks(@Param("query") String query,
                           @Param("category") String category,
                           @Param("status") String status);

    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL AND b.category != '' ORDER BY b.category ASC")
    List<String> findDistinctCategories();

    @Query("SELECT COALESCE(SUM(b.totalCopies), 0) FROM Book b")
    long sumTotalCopies();

    @Query("SELECT COALESCE(SUM(b.availableCopies), 0) FROM Book b")
    long sumAvailableCopies();

    List<Book> findByAvailableCopiesGreaterThan(int copies);
}
