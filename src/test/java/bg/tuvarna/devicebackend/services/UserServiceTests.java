package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.models.dtos.ChangePasswordVO;
import bg.tuvarna.devicebackend.models.dtos.UserCreateVO;
import bg.tuvarna.devicebackend.models.dtos.UserUpdateVO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
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

        Device mockDevice = new Device();
        mockDevice.setSerialNumber("12345");
        when(deviceService.registerDevice(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(User.class))).thenReturn(mockDevice);

        assertDoesNotThrow(() -> userService.register(userCreateVO));
    }
    @Test
    public void testUpdatePasswordSuccess() {
        User user = User.builder()
                .fullName("test")
                .password("abc")
                .phone("+222")
                .email("test@test.com")
                .address("address")
                .role(UserRole.USER)
                .id(2L)
                .build();
        userRepository.save(user);
        assertDoesNotThrow(() -> userService.updatePassword(2L, new ChangePasswordVO("abc", "test")));
    }
    @Test
    public void testUpdateAdminPassword() {
        User user = User.builder()
                .fullName("test")
                .password("abc")
                .phone("+222")
                .email("test@test.com")
                .address("address")
                .role(UserRole.ADMIN)
                .id(2L)
                .build();
        userRepository.save(user);
        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.updatePassword(2L,new ChangePasswordVO("abc","def"))
        );
        assertEquals("Admin password can't be changed", ex.getMessage());
    }
    @Test
    public void testUpdatePasswordOldMismatch() {
        User user = User.builder()
                .fullName("test")
                .password("abc")
                .phone("+222")
                .email("test@test.com")
                .address("address")
                .role(UserRole.ADMIN)
                .id(2L)
                .build();
        userRepository.save(user);
        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.updatePassword(2L,new ChangePasswordVO("aaa","def"))
        );
        assertEquals("Old password didn't match", ex.getMessage());
    }
    @Test
    public void testUpdateUserEmailTaken() {
        User user = User.builder()
                .fullName("ivan")
                .password("abc")
                .phone("+222")
                .email("ivan@test.com")
                .address("address")
                .role(UserRole.USER)
                .id(1L)
                .build();
        userRepository.save(user);
        User user1 = User.builder()
                .fullName("petar")
                .password("abcd")
                .phone("+234")
                .email("petar@test.com")
                .address("address2")
                .role(UserRole.USER)
                .id(2L)
                .build();
        userRepository.save(user1);
        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.updateUser(2L,new UserUpdateVO("petar","address2","+234","ivan@test.com"))
        );
        assertEquals("Email already taken", ex.getMessage());
    }
    @Test
    public void testUpdateUserPhoneTaken() {
        User user = User.builder()
                .fullName("ivan")
                .password("abc")
                .phone("+222")
                .email("ivan@test.com")
                .address("address")
                .role(UserRole.USER)
                .id(1L)
                .build();
        userRepository.save(user);
        User user1 = User.builder()
                .fullName("petar")
                .password("abcd")
                .phone("+234")
                .email("petar@test.com")
                .address("address2")
                .role(UserRole.USER)
                .id(2L)
                .build();
        userRepository.save(user1);
        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.updateUser(2L,new UserUpdateVO("petar","address2","+222","petar@test.com"))
        );
        assertEquals("Phone already taken", ex.getMessage());
    }
}
