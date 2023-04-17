package study.datajpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.entity.Team;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;
    @PersistenceContext
    EntityManager em;

    @Test
    public void testMember() {
        Member member = new Member("memberA");
        Member savedMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(savedMember.getId()).get();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    public void basicCRUD() {
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        // 단건 조회 검증
        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();

        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        // 리스트 조회 검증
        List<Member> all = memberRepository.findAll();
        assertThat(all.size()).isEqualTo(2);

        // 카운트 검증
        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        // 삭제 검증
        memberRepository.delete(member1);
        memberRepository.delete(member2);

        // 카운트 검증
        long afterDeleteCount = memberRepository.count();
        assertThat(afterDeleteCount).isEqualTo(0);
    }


    @Test
    public void findByUsernameAndAgeGreaterThan() {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);
        memberRepository.save(m1);
        memberRepository.save(m2);
        List<Member> result =
                memberRepository.findByUsernameAndAgeGreaterThan("AAA", 15);
        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void pagin() {
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member9", 10));
        memberRepository.save(new Member("member10", 10));
        memberRepository.save(new Member("member4", 20));
        memberRepository.save(new Member("member5", 20));
        memberRepository.save(new Member("member6", 30));
        memberRepository.save(new Member("member7", 30));
        memberRepository.save(new Member("member8", 40));

        int age = 10;
        //int offset = 0;
        //int limit = 3;

        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

        //when
        Page<Member> page = memberRepository.findByAge(age, pageRequest);

        Page<MemberDto> toMap = page.map(m -> new MemberDto(m.getId(), m.getUsername(), null));

        for (MemberDto memberDto : toMap) {
            System.out.println("memberDto = " + memberDto);
        }

        //then
        List<Member> content = page.getContent();
        //long totalElements = page.getTotalElements();
        for (Member member : content) {
            System.out.println("member = " + member);
        }

        //System.out.println("totalElements = " + totalElements);


        assertThat(content.size()).isEqualTo(3);
        //assertThat(content.getTotalElements()).isEqualTo(5); // page Type
        //assertThat(page.getTotalPages()).isEqualTo(2); // page Type
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();

    }

    @Test
    public void bulkUpdate() throws Exception {
        // given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 19));
        memberRepository.save(new Member("member3", 20));
        memberRepository.save(new Member("member4", 21));
        memberRepository.save(new Member("member5", 40));

        // when
        int resultCount = memberRepository.bulkAgePlus(20);

        // 벌크 연산 이후에는 영속성 컨텍스트 초기화!! ->Spring Data Jpa : @Modifying(clearAutomatically = true)
        //em.flush();
        //em.clear();

        // then
        assertThat(resultCount).isEqualTo(3);
    }

    @Test
    public void findMemberLazy() throws Exception {
        // given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        teamRepository.save(teamA);
        teamRepository.save(teamB);

        Member member1 = new Member("meber1", 10, teamA);
        Member member2 = new Member("meber1", 10, teamA);
        Member member3 = new Member("meber2", 10, teamB);

        memberRepository.save(member1);
        memberRepository.save(member2);
        memberRepository.save(member3);

        em.flush();
        em.clear();

        // when
        //List<Member> members = memberRepository.findMemberEntityGraph();
        List<Member> members = memberRepository.findEntityGraphByUsername("member1");
        //List<Member> members = memberRepository.findMemberFetchJoin();

        for (Member member : members) {
            System.out.println("member = " + member.getUsername());
            System.out.println("member.getTeam().getName() = " + member.getTeam().getName());
        }

        // then

    }

    @Test
    public void queryHint() throws Exception {
        // given
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);
        em.flush();
        em.clear();

        // when
        Member findMember = memberRepository.findById(member1.getId()).get();
        findMember.setUsername("aaaaaMember2");

        em.flush();
        // then
    }

    @DisplayName("lock")
    @Test
    public void lock() throws Exception {
        // given
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        em.flush();
        em.clear();
        // when
        List<Member> result = memberRepository.findLockByUsername(member.getUsername());

        // then
    }

    @DisplayName("custom Repository")
    @Test
    public void cusmtomRepo() throws Exception {
        // given
        List<Member> memberCustom = memberRepository.findMemberCustom();

        // when

        // then
    }

    @DisplayName("porojections")
    @Test
    public void projections() throws Exception {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 10, teamA);
        Member member3 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamA);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);

        em.flush();
        em.clear();

        // when
        List<UsernameOnlyDto> member11 = memberRepository.findProjectionsByUsername("member1");
        for (UsernameOnlyDto usernameOnlyDto : member11) {

            System.out.println("usernameOnlyDto.getUsername() = " + usernameOnlyDto.getUsername());
        }

        // then
    }
    
    @DisplayName("native Query")
    @Test
    public void nativeQuery() throws Exception{
        // given

        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 10, teamA);
        Member member3 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamA);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);

        em.flush();
        em.clear();
        // when
        //Member nativeResult = memberRepository.findByNativeQuery("member2");
        Page<MemberProjection> byNativeProjection = memberRepository.findByNativeProjection(PageRequest.of(1, 10));
        List<MemberProjection> content = byNativeProjection.getContent();
        for (MemberProjection memberProjection : content) {
            System.out.println("memberProjection.getUsername() = " + memberProjection.getUsername());
            System.out.println("memberProjection.getTeamName() = " + memberProjection.getTeamName());
        }

        // then
    }

}