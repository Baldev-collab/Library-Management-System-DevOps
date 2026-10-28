package com.library.management.repository;

import com.library.management.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberCode(String memberCode);

    Optional<Member> findByEmail(String email);

    boolean existsByMemberCode(String memberCode);

    boolean existsByMemberCodeAndIdNot(String memberCode, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT m FROM Member m WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.phone) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR m.status = :status) " +
           "ORDER BY m.name ASC")
    List<Member> searchMembers(@Param("query") String query, @Param("status") String status);

    List<Member> findByStatus(String status);

    long countByStatus(String status);
}
