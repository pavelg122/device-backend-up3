package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class DeviceRepositoryTests {
    @Autowired
    private DeviceRepository deviceRepository;
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

        Device device = new Device();
        device.setUser(user);
        device.setSerialNumber("123456");
        Device device2 = new Device();
        device2.setUser(user);
        device2.setSerialNumber("234567");

        userRepository.save(user);
        deviceRepository.save(device);
        deviceRepository.save(device2);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        deviceRepository.deleteAll();
    }
    @Test
    void deleteBySerialNumber() {
        deviceRepository.deleteBySerialNumber("123456");
        var result = deviceRepository.findAll("gosho",Pageable.ofSize(10)).getContent();
        assertEquals(1, result.size());
    }
    @Test
    void findAllByUserName(){
        var result = deviceRepository.findAll("gosho",Pageable.ofSize(10)).getContent();
        assertEquals(2, result.size());
    }
    @Test
    void findAllByUserAddress(){
        var result = deviceRepository.findAll("adress1",Pageable.ofSize(10)).getContent();
        assertEquals(2, result.size());
    }
    @Test
    void findAllByUserEmail(){
        var result = deviceRepository.findAll("gosho@abv.bg",Pageable.ofSize(10)).getContent();
        assertEquals(2, result.size());
    }
    @Test
    void findAllByUserPhone(){
        var result = deviceRepository.findAll("0888123456",Pageable.ofSize(10)).getContent();
        assertEquals(2, result.size());
    }
}
