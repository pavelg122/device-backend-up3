package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.execptions.CustomException;
import bg.tuvarna.devicebackend.models.dtos.UserCreateVO;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
public class UserServiceTests {
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private DeviceService deviceService;
    @Autowired
    private UserService userService;

    @Test
    public void testUserService() {
        UserCreateVO userCreateVO = new UserCreateVO(
                "Ivan",
                "123",
                "Email",
                "+123",
                "adress",
                LocalDate.now(),
                "123451"
        );

        when(userRepository.getByPhone("+123")).thenReturn(new User());
        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.register(userCreateVO)
        );
        assertEquals("Phone already taken", ex.getMessage());
    }
    @Test
    public void testPasswordIsEncoded(){
        UserCreateVO userCreateVO = new UserCreateVO(
                "Ivan",
                "123",
                "Email",
                "+123",
                "adress",
                LocalDate.now(),
                "123451"
        );

        when(userRepository.getByPhone("+123")).thenReturn(null);
        when(userRepository.getByEmail("Email")).thenReturn(null);
        when(passwordEncoder.encode("123")).thenReturn("encoded123");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer( i -> {
                    User user = i.getArgument(0);
                    assertEquals("encoded123", user.getPassword());
                    return user;
                });

        doNothing().when(deviceService).alreadyExist("123451");

        doNothing().when(deviceService).registerDevice(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(User.class)
        );

        assertDoesNotThrow(() -> userService.register(userCreateVO));
    }
}
