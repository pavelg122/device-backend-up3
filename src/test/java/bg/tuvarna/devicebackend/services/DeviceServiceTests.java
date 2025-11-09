package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
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
        when(deviceRepository.save(org.mockito.ArgumentMatchers.any(Device.class))).thenReturn(device);
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
        when(deviceRepository.save(org.mockito.ArgumentMatchers.any(Device.class))).thenReturn(device);
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
        when(deviceRepository.save(org.mockito.ArgumentMatchers.any(Device.class))).thenReturn(device);
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
}
