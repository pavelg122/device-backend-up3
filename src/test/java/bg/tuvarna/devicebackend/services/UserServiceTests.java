package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.ChangePasswordVO;
import bg.tuvarna.devicebackend.models.dtos.UserCreateVO;
import bg.tuvarna.devicebackend.models.dtos.UserListing;
import bg.tuvarna.devicebackend.models.dtos.UserUpdateVO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import bg.tuvarna.devicebackend.repositories.UserRepository;
import bg.tuvarna.devicebackend.utils.CustomPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user);
        when(passwordEncoder.matches("abc", "abc")).thenReturn(true);
        when(passwordEncoder.encode("test")).thenReturn("encodedTest");
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
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user);
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
                .id(2L)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user);
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
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user);
        User user1 = User.builder()
                .fullName("petar")
                .password("abcd")
                .phone("+234")
                .email("petar@test.com")
                .address("address2")
                .role(UserRole.USER)
                .id(2L)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user1));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user1);
        when(userRepository.getByEmail("ivan@test.com")).thenReturn(user);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user);
        User user1 = User.builder()
                .fullName("petar")
                .password("abcd")
                .phone("+234")
                .email("petar@test.com")
                .address("address2")
                .role(UserRole.USER)
                .id(2L)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user1));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(user1);
        when(userRepository.getByPhone("+222")).thenReturn(user);
        CustomException ex = assertThrows(
                CustomException.class,
                () -> userService.updateUser(2L,new UserUpdateVO("petar","address2","+222","petar@test.com"))
        );
        assertEquals("Phone already taken", ex.getMessage());
    }

    @Test
    public void testRegisterEmailTaken() {
        UserCreateVO vo = new UserCreateVO(
                "Ivan", "123", "Email", "+123", "adress",
                LocalDate.now(), "123451"
        );

        when(userRepository.getByEmail("Email")).thenReturn(new User());

        CustomException ex = assertThrows(CustomException.class, () -> userService.register(vo));
        assertEquals("Email already taken", ex.getMessage());
        assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    @Test
    public void testRegisterReturnsEarlyWhenDeviceDataMissing() {
        UserCreateVO vo = new UserCreateVO(
                "Ivan", "123", "Email", "+123", "adress",
                null, // purchaseDate missing => early return
                "SN"
        );

        when(userRepository.getByEmail("Email")).thenReturn(null);
        when(userRepository.getByPhone("+123")).thenReturn(null);
        when(passwordEncoder.encode("123")).thenReturn("enc");

        User saved = User.builder().id(10L).build();
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(saved);

        assertDoesNotThrow(() -> userService.register(vo));

        verify(deviceService, never()).alreadyExist(anyString());
        verify(deviceService, never()).registerDevice(anyString(), any(), any());
    }

    @Test
    public void testRegisterRollsBackWhenDeviceRegistrationFails() {
        UserCreateVO vo = new UserCreateVO(
                "Ivan", "123", "Email", "+123", "adress",
                LocalDate.of(2025, 1, 1),
                "SN1"
        );

        when(userRepository.getByEmail("Email")).thenReturn(null);
        when(userRepository.getByPhone("+123")).thenReturn(null);
        when(passwordEncoder.encode("123")).thenReturn("enc");

        User saved = User.builder().id(10L).email("Email").phone("+123").build();
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(saved);

        doNothing().when(deviceService).alreadyExist("SN1");
        doThrow(new CustomException("Invalid serial number", ErrorCode.Failed))
                .when(deviceService).registerDevice(eq("SN1"), any(LocalDate.class), eq(saved));

        CustomException ex = assertThrows(CustomException.class, () -> userService.register(vo));
        assertEquals("Invalid serial number", ex.getMessage());

        verify(userRepository).delete(saved);
    }

    @Test
    public void testGetUserByIdThrowsWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUserById(1L));
        assertEquals("User not found", ex.getMessage());
        assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    @Test
    public void testGetUserByUsernameThrowsWhenMissing() {
        when(userRepository.findByEmailOrPhone("x")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUserByUsername("x"));
        assertEquals("User not found", ex.getMessage());
        assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    @Test
    public void testUpdateUserAdminForbidden() {
        User admin = User.builder().id(1L).role(UserRole.ADMIN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.updateUser(1L, new UserUpdateVO("n", "a", "p", "e")));
        assertEquals("Admin password can't be changed", ex.getMessage());
        assertEquals(ErrorCode.Validation, ex.getErrorCode());
    }

    @Test
    public void testGetUsersSearchFiltersDevicesInListingBranch() {
        Device d1 = new Device();
        d1.setSerialNumber("ABC-1");
        Device d2 = new Device();
        d2.setSerialNumber("ZZZ-9");

        User u = User.builder()
                .id(1L)
                .fullName("Test")
                .role(UserRole.USER)
                .devices(List.of(d1, d2))
                .build();

        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 10), 1);
        when(userRepository.searchBy(eq("ABC"), any(PageRequest.class))).thenReturn(page);

        CustomPage<UserListing> result = userService.getUsers("ABC", 1, 10);

        assertEquals(1, result.getItems().size());
        UserListing listing = result.getItems().getFirst();

        verify(userRepository).searchBy(eq("ABC"), any(PageRequest.class));
        verify(userRepository, never()).getAllUsers(any(PageRequest.class));
        assertEquals(1, result.getTotalItems());
    }
}
