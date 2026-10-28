package com.library.management.service;

import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Member;
import com.library.management.repository.BookIssueRepository;
import com.library.management.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final BookIssueRepository bookIssueRepository;

    public MemberService(MemberRepository memberRepository, BookIssueRepository bookIssueRepository) {
        this.memberRepository = memberRepository;
        this.bookIssueRepository = bookIssueRepository;
    }

    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Member> searchMembers(String query, String status) {
        return memberRepository.searchMembers(
                (query != null && !query.isBlank()) ? query.trim() : null,
                (status != null && !status.isBlank()) ? status.trim() : null
        );
    }

    @Transactional(readOnly = true)
    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Member getMemberOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with ID: " + id));
    }

    public Member createMember(Member member) {
        if (member.getMemberCode() != null && memberRepository.existsByMemberCode(member.getMemberCode())) {
            throw new DuplicateResourceException("Member code '" + member.getMemberCode() + "' is already in use.");
        }
        if (member.getEmail() != null && memberRepository.existsByEmail(member.getEmail())) {
            throw new DuplicateResourceException("A member with email '" + member.getEmail() + "' is already registered.");
        }

        return memberRepository.save(member);
    }

    public Member updateMember(Long id, Member updatedMemberData) {
        Member existingMember = getMemberOrThrow(id);

        if (updatedMemberData.getMemberCode() != null &&
            memberRepository.existsByMemberCodeAndIdNot(updatedMemberData.getMemberCode(), id)) {
            throw new DuplicateResourceException("Member code '" + updatedMemberData.getMemberCode() + "' is already taken.");
        }

        if (updatedMemberData.getEmail() != null &&
            memberRepository.existsByEmailAndIdNot(updatedMemberData.getEmail(), id)) {
            throw new DuplicateResourceException("Email '" + updatedMemberData.getEmail() + "' is already in use by another member.");
        }

        existingMember.setMemberCode(updatedMemberData.getMemberCode());
        existingMember.setName(updatedMemberData.getName());
        existingMember.setEmail(updatedMemberData.getEmail());
        existingMember.setPhone(updatedMemberData.getPhone());
        existingMember.setAddress(updatedMemberData.getAddress());
        existingMember.setMembershipDate(updatedMemberData.getMembershipDate());
        existingMember.setStatus(updatedMemberData.getStatus());

        return memberRepository.save(existingMember);
    }

    public void deleteMember(Long id) {
        Member member = getMemberOrThrow(id);
        boolean hasActiveLoans = bookIssueRepository.existsByMemberIdAndStatusIn(id, Set.of("ISSUED", "OVERDUE"));
        if (hasActiveLoans) {
            throw new BusinessRuleException("Cannot delete member '" + member.getName() + "' because they currently have issued or overdue books.");
        }
        memberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<Member> getActiveMembers() {
        return memberRepository.findByStatus("ACTIVE");
    }

    @Transactional(readOnly = true)
    public long countTotalMembers() {
        return memberRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveMembers() {
        return memberRepository.countByStatus("ACTIVE");
    }
}
