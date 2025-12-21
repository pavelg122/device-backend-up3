package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.DeviceCreateVO;
import bg.tuvarna.devicebackend.models.dtos.DeviceUpdateVO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.Passport;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.repositories.DeviceRepository;
import bg.tuvarna.devicebackend.utils.CustomPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class DeviceServiceTests {
    @MockBean
    private DeviceRepository deviceRepository;
    @MockBean
    private PassportService passportService;

    @Autowired
    private DeviceService deviceService;

    @Test
    public void testRegisterDeviceSuccess(){
        Device device = new Device();
        device.setSerialNumber("123456");
        device.setUser(new User());
        Passport passport = new Passport();
        device.setPassport(passport);
        device.setPurchaseDate(LocalDate.now());
        when(passportService.findPassportBySerialId("123456")).thenReturn(passport);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        assertDoesNotThrow(() -> deviceService.registerDevice("123456",LocalDate.now(),new User()));
    }

    @Test
    public void testRegisterDeviceFailure(){
        Device device = new Device();
        device.setSerialNumber("123456");
        device.setUser(new User());
        Passport passport = new Passport();
        device.setPassport(passport);
        device.setPurchaseDate(LocalDate.now());
        when(passportService.findPassportBySerialId("123456")).thenReturn(null);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        CustomException ex = assertThrows(
                CustomException.class,() -> deviceService.registerDevice("123456",LocalDate.now(),new User())
        );
        Assertions.assertEquals("Invalid serial number", ex.getMessage());
    }
    @Test
    public void testUpdateDeviceSuccess(){
        Device device = new Device();
        device.setSerialNumber("123456");
        device.setUser(new User());
        Passport passport = new Passport();
        device.setPassport(passport);
        device.setPurchaseDate(LocalDate.now());
        when(passportService.findPassportBySerialId("123456")).thenReturn(passport);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(deviceRepository.findById("123456")).thenReturn(Optional.of(device));
        assertDoesNotThrow(() -> deviceService.updateDevice("123456",new DeviceUpdateVO(LocalDate.now(),"comment")));
    }
    @Test
    public void testGetDevicesSuccess(){
        Device device = new Device();
        device.setSerialNumber("123456");
        device.setUser(new User());
        Passport passport = new Passport();
        device.setPassport(passport);
        device.setPurchaseDate(LocalDate.now());
        List<Device> devices = new ArrayList<>();
        devices.add(device);
        Page<Device> devicePage = new PageImpl<>(devices, Pageable.ofSize(10), 1);
        when(deviceRepository.getAllDevices(Pageable.ofSize(10))).thenReturn(devicePage);
        CustomPage<Device> result = deviceService.getDevices(null,1,10);
        Assertions.assertEquals(result.getItems().size(), 1);
    }
    @Test
    public void testFindDeviceReturnsNullWhenMissing() {
        when(deviceRepository.findById("missing")).thenReturn(Optional.empty());
        Assertions.assertNull(deviceService.findDevice("missing"));
    }

    @Test
    public void testIsDeviceExistsThrowsWhenNotExists() {
        when(deviceRepository.existsById("abc")).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.isDeviceExists("abc"));
        Assertions.assertEquals("Device not registered", ex.getMessage());
        Assertions.assertEquals(ErrorCode.NotRegistered, ex.getErrorCode());
    }

    @Test
    public void testIsDeviceExistsReturnsDevice() {
        Device device = new Device();
        device.setSerialNumber("abc");

        when(deviceRepository.existsById("abc")).thenReturn(true);
        when(deviceRepository.findById("abc")).thenReturn(Optional.of(device));

        Device result = deviceService.isDeviceExists("abc");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("abc", result.getSerialNumber());
    }

    @Test
    public void testAlreadyExistThrowsWhenFound() {
        Device existing = new Device();
        existing.setSerialNumber("123");

        when(deviceRepository.findById("123")).thenReturn(Optional.of(existing));

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.alreadyExist("123"));
        Assertions.assertEquals("Device already registered", ex.getMessage());
        Assertions.assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    @Test
    public void testRegisterNewDeviceThrowsWhenUserNull() {
        DeviceCreateVO vo = new DeviceCreateVO("SN1", LocalDate.now());
        when(deviceRepository.findById("SN1")).thenReturn(Optional.empty()); // alreadyExist passes

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.registerNewDevice(vo, null));
        Assertions.assertEquals("User not found", ex.getMessage());
        Assertions.assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    @Test
    public void testRegisterNewDeviceSuccess() {
        LocalDate purchase = LocalDate.of(2025, 1, 1);
        DeviceCreateVO vo = new DeviceCreateVO("SNX", purchase);
        User user = new User();

        Passport passport = new Passport();
        passport.setWarrantyMonths(24);

        when(deviceRepository.findById("SNX")).thenReturn(Optional.empty()); // alreadyExist passes
        when(passportService.findPassportBySerialId("SNX")).thenReturn(passport);

        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        Device saved = deviceService.registerNewDevice(vo, user);
        Assertions.assertNotNull(saved);
        Assertions.assertEquals("SNX", saved.getSerialNumber());
        Assertions.assertEquals(purchase, saved.getPurchaseDate());
        Assertions.assertEquals(user, saved.getUser());
        Assertions.assertEquals(passport, saved.getPassport());

        Assertions.assertEquals(purchase.plusMonths(24).plusMonths(12), saved.getWarrantyExpirationDate());
    }

    @Test
    public void testUpdateDeviceThrowsWhenNotFound() {
        when(deviceRepository.findById("missing")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(
                CustomException.class,
                () -> deviceService.updateDevice("missing", new DeviceUpdateVO(LocalDate.now(), "c"))
        );
        Assertions.assertEquals("Device not found", ex.getMessage());
        Assertions.assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    @Test
    public void testUpdateDeviceWarrantyWithUserAdds12Months() {
        Passport passport = new Passport();
        passport.setWarrantyMonths(6);

        Device device = new Device();
        device.setSerialNumber("SN");
        device.setPassport(passport);
        device.setUser(new User()); // triggers +12 branch

        when(deviceRepository.findById("SN")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate purchase = LocalDate.of(2025, 2, 1);
        Device updated = deviceService.updateDevice("SN", new DeviceUpdateVO(purchase, "hello"));

        Assertions.assertEquals(purchase, updated.getPurchaseDate());
        Assertions.assertEquals("hello", updated.getComment());
        Assertions.assertEquals(purchase.plusMonths(6).plusMonths(12), updated.getWarrantyExpirationDate());
    }

    @Test
    public void testUpdateDeviceWarrantyAnonymousDoesNotAdd12Months() {
        Passport passport = new Passport();
        passport.setWarrantyMonths(6);

        Device device = new Device();
        device.setSerialNumber("SN");
        device.setPassport(passport);
        device.setUser(null); // does NOT trigger +12

        when(deviceRepository.findById("SN")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate purchase = LocalDate.of(2025, 2, 1);
        Device updated = deviceService.updateDevice("SN", new DeviceUpdateVO(purchase, "hello"));

        assertEquals(purchase.plusMonths(6), updated.getWarrantyExpirationDate());
    }

    @Test
    public void testDeleteDeviceSuccess() {
        doNothing().when(deviceRepository).deleteBySerialNumber("SN");
        assertDoesNotThrow(() -> deviceService.deleteDevice("SN"));
        verify(deviceRepository).deleteBySerialNumber("SN");
    }

    @Test
    public void testDeleteDeviceFailureCatchesRuntime() {
        doThrow(new RuntimeException("fk")).when(deviceRepository).deleteBySerialNumber("SN");

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.deleteDevice("SN"));
        Assertions.assertEquals("Cannot delete device: renovations exist", ex.getMessage());
        Assertions.assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }

    @Test
    public void testAddAnonymousDeviceSuccess() {
        Passport passport = new Passport();
        passport.setWarrantyMonths(10);

        LocalDate purchase = LocalDate.of(2025, 3, 10);
        DeviceCreateVO vo = new DeviceCreateVO("ANON1", purchase);

        when(deviceRepository.findById("ANON1")).thenReturn(Optional.empty()); // alreadyExist passes
        when(passportService.findPassportBySerialId("ANON1")).thenReturn(passport);
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        Device saved = deviceService.addAnonymousDevice(vo);
        Assertions.assertEquals("ANON1", saved.getSerialNumber());
        Assertions.assertNull(saved.getUser());
        Assertions.assertEquals(purchase.plusMonths(10), saved.getWarrantyExpirationDate());
    }

    @Test
    public void testAddAnonymousDeviceFailureInvalidSerial() {
        DeviceCreateVO vo = new DeviceCreateVO("BAD", LocalDate.now());

        when(deviceRepository.findById("BAD")).thenReturn(Optional.empty()); // alreadyExist passes
        when(passportService.findPassportBySerialId("BAD")).thenThrow(new RuntimeException("no passport"));

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.addAnonymousDevice(vo));
        Assertions.assertEquals("Invalid serial number", ex.getMessage());
        Assertions.assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }

    @Test
    public void testGetDevicesNoSearchUsesGetAllDevices() {
        Device d = new Device();
        d.setSerialNumber("D1");

        Page<Device> page = new PageImpl<>(List.of(d), PageRequest.of(0, 10), 1);
        when(deviceRepository.getAllDevices(any(PageRequest.class))).thenReturn(page);

        CustomPage<Device> result = deviceService.getDevices(null, 1, 10);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals(1, result.getTotalItems());
        Assertions.assertEquals(1, result.getTotalPages());
        Assertions.assertEquals(1, result.getCurrentPage());
        Assertions.assertEquals(10, result.getSize());

        verify(deviceRepository).getAllDevices(any(PageRequest.class));
        verify(deviceRepository, never()).findAll(anyString(), any(PageRequest.class));
    }

    @Test
    public void testGetDevicesWithSearchUsesFindAll() {
        Device d = new Device();
        d.setSerialNumber("ABC123");

        Page<Device> page = new PageImpl<>(List.of(d), PageRequest.of(1, 5), 1);
        when(deviceRepository.findAll(eq("ABC"), any(PageRequest.class))).thenReturn(page);

        CustomPage<Device> result = deviceService.getDevices("ABC", 2, 5);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals(2, result.getCurrentPage());
        Assertions.assertEquals(5, result.getSize());

        verify(deviceRepository, never()).getAllDevices(any(PageRequest.class));
        verify(deviceRepository).findAll(eq("ABC"), any(PageRequest.class));
    }
}
