package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTests {
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .fullName("gosho")
                .email("gosho@abv.bg")
                .phone("0888123456")
                .address("adress1")
                .role(UserRole.USER)
                .build();

        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void userFindBySearchEmail(){
        User user = userRepository.searchBy("gosho@abv.bg",Pageable.ofSize(1)).getContent().getFirst();
        assertEquals("adress1", user.getAddress());
    }
    @Test
    void userFindBySearchAddress(){
        User user = userRepository.searchBy("adress1", Pageable.ofSize(1)).getContent().getFirst();
        assertEquals("gosho", user.getFullName());
    }

    @Test
    void userFindBySearchName() {
        User user = userRepository.searchBy("gosho", Pageable.ofSize(1)).getContent().getFirst();
        assertEquals("0888123456", user.getPhone());
    }

    @Test
    void userFindBySearchPhone() {
        User user = userRepository.searchBy("0888123456", Pageable.ofSize(1)).getContent().getFirst();
        assertEquals("gosho", user.getFullName());
    }

    @Test
    void userFindBySearchPartial(){
        User user = userRepository.searchBy("gos", Pageable.ofSize(1)).getContent().getFirst();
        assertEquals("adress1", user.getAddress());
    }

    @Test
    void userFindMultipleBySearchPartial(){
        User user = User.builder()
                .fullName("pesho")
                .phone("0123456789")
                .email("pesho@abv.bg")
                .address("adress2")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        var result = userRepository.searchBy("sho", Pageable.ofSize(10)).getContent();
        assertEquals(2, result.size());
    }

}
