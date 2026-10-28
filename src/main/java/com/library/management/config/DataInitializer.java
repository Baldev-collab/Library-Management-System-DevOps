package com.library.management.config;

import com.library.management.model.Book;
import com.library.management.model.BookIssue;
import com.library.management.model.Member;
import com.library.management.repository.BookIssueRepository;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BookIssueRepository bookIssueRepository;

    public DataInitializer(BookRepository bookRepository,
                           MemberRepository memberRepository,
                           BookIssueRepository bookIssueRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.bookIssueRepository = bookIssueRepository;
    }

    @Override
    public void run(String... args) {
        seedBooks();
        seedMembers();
        seedIssues();
    }

    private void seedBooks() {
        if (bookRepository.count() == 0) {
            log.info("Seeding initial library books...");
            List<Book> sampleBooks = List.of(
                    new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 5,
                            "The definitive guide to Java best practices, design patterns, and idiomatic coding."),
                    new Book("978-0132350884", "Clean Code", "Robert C. Martin", "Software Engineering", 4,
                            "A handbook of agile software craftsmanship, code smells, and refactoring techniques."),
                    new Book("978-1491950357", "Designing Data-Intensive Applications", "Martin Kleppmann", "Architecture", 3,
                            "The big ideas behind reliable, scalable, and maintainable systems."),
                    new Book("978-0201633610", "Design Patterns: Elements of Reusable Object-Oriented Software",
                            "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides", "Design", 2,
                            "Classic GoF catalog of essential patterns in object-oriented software engineering."),
                    new Book("978-1617294945", "Spring in Action, Sixth Edition", "Craig Walls", "Frameworks", 4,
                            "Hands-on guide to building modern enterprise web applications with Spring Boot.")
            );
            bookRepository.saveAll(sampleBooks);
            log.info("Successfully seeded {} sample books.", sampleBooks.size());
        }
    }

    private void seedMembers() {
        if (memberRepository.count() == 0) {
            log.info("Seeding initial library members...");
            List<Member> sampleMembers = List.of(
                    new Member("LIB-MEM-001", "Alice Johnson", "alice.johnson@example.com", "+1-555-0101", "123 Maple Street, Springfield", LocalDate.of(2025, 1, 15)),
                    new Member("LIB-MEM-002", "Brian Miller", "brian.miller@example.com", "+1-555-0102", "456 Oak Avenue, Metropolis", LocalDate.of(2025, 2, 20)),
                    new Member("LIB-MEM-003", "Clara Vance", "clara.vance@example.com", "+1-555-0103", "789 Pine Road, Gotham", LocalDate.of(2025, 3, 10)),
                    new Member("LIB-MEM-004", "David Kumar", "david.kumar@example.com", "+1-555-0104", "321 Elm Lane, Star City", LocalDate.of(2025, 4, 5))
            );
            memberRepository.saveAll(sampleMembers);
            log.info("Successfully seeded {} sample members.", sampleMembers.size());
        }
    }

    private void seedIssues() {
        if (bookIssueRepository.count() == 0) {
            var book1Opt = bookRepository.findByIsbn("978-0134685991");
            var member1Opt = memberRepository.findByMemberCode("LIB-MEM-001");

            if (book1Opt.isPresent() && member1Opt.isPresent()) {
                Book book1 = book1Opt.get();
                if (book1.getAvailableCopies() > 0) {
                    book1.decrementAvailable();
                    bookRepository.save(book1);
                    BookIssue issue1 = new BookIssue(book1, member1Opt.get(),
                            LocalDate.now().minusDays(5),
                            LocalDate.now().plusDays(9),
                            "Standard 2-week loan for coursework");
                    bookIssueRepository.save(issue1);
                }
            }

            var book2Opt = bookRepository.findByIsbn("978-0201633610");
            var member2Opt = memberRepository.findByMemberCode("LIB-MEM-002");

            if (book2Opt.isPresent() && member2Opt.isPresent()) {
                Book book2 = book2Opt.get();
                if (book2.getAvailableCopies() > 0) {
                    book2.decrementAvailable();
                    bookRepository.save(book2);
                    BookIssue issue2 = new BookIssue(book2, member2Opt.get(),
                            LocalDate.now().minusDays(20),
                            LocalDate.now().minusDays(6),
                            "Overdue loan for testing late fees");
                    issue2.setStatus("OVERDUE");
                    issue2.setLateFee(BigDecimal.valueOf(12.00));
                    bookIssueRepository.save(issue2);
                }
            }
        }
    }
}
