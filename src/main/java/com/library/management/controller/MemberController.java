package com.library.management.controller;

import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.model.Member;
import com.library.management.repository.BookIssueRepository;
import com.library.management.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final BookIssueRepository bookIssueRepository;

    public MemberController(MemberService memberService, BookIssueRepository bookIssueRepository) {
        this.memberService = memberService;
        this.bookIssueRepository = bookIssueRepository;
    }

    @GetMapping
    public String listMembers(@RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "status", required = false) String status,
                             Model model) {
        model.addAttribute("members", memberService.searchMembers(query, status));
        model.addAttribute("selectedQuery", query);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeNav", "members");
        return "members/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Member member = new Member();
        member.setMembershipDate(LocalDate.now());
        member.setStatus("ACTIVE");
        model.addAttribute("member", member);
        model.addAttribute("isEdit", false);
        model.addAttribute("activeNav", "members");
        return "members/form";
    }

    @PostMapping("/save")
    public String saveMember(@Valid @ModelAttribute("member") Member member,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("activeNav", "members");
            return "members/form";
        }

        try {
            memberService.createMember(member);
            redirectAttributes.addFlashAttribute("successMessage", "Member '" + member.getName() + "' successfully registered!");
            return "redirect:/members";
        } catch (DuplicateResourceException e) {
            if (e.getMessage().toLowerCase().contains("code")) {
                result.rejectValue("memberCode", "error.member", e.getMessage());
            } else {
                result.rejectValue("email", "error.member", e.getMessage());
            }
            model.addAttribute("isEdit", false);
            model.addAttribute("activeNav", "members");
            return "members/form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error saving member: " + e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("activeNav", "members");
            return "members/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Member member = memberService.getMemberOrThrow(id);
        model.addAttribute("member", member);
        model.addAttribute("isEdit", true);
        model.addAttribute("activeNav", "members");
        return "members/form";
    }

    @PostMapping("/update/{id}")
    public String updateMember(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("member") Member member,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            member.setId(id);
            model.addAttribute("isEdit", true);
            model.addAttribute("activeNav", "members");
            return "members/form";
        }

        try {
            memberService.updateMember(id, member);
            redirectAttributes.addFlashAttribute("successMessage", "Member details for '" + member.getName() + "' updated successfully!");
            return "redirect:/members";
        } catch (DuplicateResourceException e) {
            if (e.getMessage().toLowerCase().contains("code")) {
                result.rejectValue("memberCode", "error.member", e.getMessage());
            } else {
                result.rejectValue("email", "error.member", e.getMessage());
            }
            model.addAttribute("isEdit", true);
            model.addAttribute("activeNav", "members");
            return "members/form";
        }
    }

    @GetMapping("/view/{id}")
    public String viewMember(@PathVariable("id") Long id, Model model) {
        Member member = memberService.getMemberOrThrow(id);
        model.addAttribute("member", member);
        model.addAttribute("issues", bookIssueRepository.findByMemberId(id));
        model.addAttribute("activeNav", "members");
        return "members/view";
    }

    @PostMapping("/delete/{id}")
    public String deleteMember(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Member member = memberService.getMemberOrThrow(id);
            String name = member.getName();
            memberService.deleteMember(id);
            redirectAttributes.addFlashAttribute("successMessage", "Member '" + name + "' removed successfully.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete member: " + e.getMessage());
        }
        return "redirect:/members";
    }
}
