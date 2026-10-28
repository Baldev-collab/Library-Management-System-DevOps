package com.library.management.repository;

import com.library.management.model.BookIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookIssueRepository extends JpaRepository<BookIssue, Long> {

    List<BookIssue> findByStatus(String status);

    List<BookIssue> findByMemberId(Long memberId);

    List<BookIssue> findByBookId(Long bookId);

    long countByStatus(String status);

    long countByStatusIn(Collection<String> statuses);

    boolean existsByBookIdAndStatusIn(Long bookId, Collection<String> statuses);

    boolean existsByMemberIdAndStatusIn(Long memberId, Collection<String> statuses);

    List<BookIssue> findByStatusInAndDueDateBefore(Collection<String> statuses, LocalDate date);

    @Query("SELECT bi FROM BookIssue bi WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(bi.book.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(bi.book.isbn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(bi.member.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(bi.member.memberCode) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR bi.status = :status) " +
           "ORDER BY bi.issueDate DESC, bi.id DESC")
    List<BookIssue> searchIssues(@Param("query") String query, @Param("status") String status);

    List<BookIssue> findTop5ByOrderByIssueDateDescIdDesc();
}
