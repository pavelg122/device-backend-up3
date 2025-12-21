package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.RenovationCreateVO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.Renovation;
import bg.tuvarna.devicebackend.repositories.RenovationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RenovationServiceTests {
    @MockBean
    private RenovationRepository renovationRepository;

    @MockBean
    private DeviceService deviceService;

    @Autowired
    private RenovationService renovationService;

    @Test
    public void testSaveSuccess() {
        Device device = new Device();
        device.setSerialNumber("SN1");

        RenovationCreateVO vo = new RenovationCreateVO("SN1", "desc", LocalDate.of(2025, 1, 1));

        when(deviceService.isDeviceExists("SN1")).thenReturn(device);

        when(renovationRepository.save(any(Renovation.class))).thenAnswer(inv -> {
            Renovation r = inv.getArgument(0);
            assertEquals(device, r.getDevice());
            assertEquals("desc", r.getDescription());
            assertEquals(LocalDate.of(2025, 1, 1), r.getRenovationDate());
            return r;
        });

        Renovation saved = renovationService.save(vo);
        assertNotNull(saved);

        verify(deviceService).isDeviceExists("SN1");
        verify(renovationRepository).save(any(Renovation.class));
    }

    @Test
    public void testSavePropagatesWhenDeviceNotRegistered() {
        RenovationCreateVO vo = new RenovationCreateVO("BAD", "desc", LocalDate.now());

        when(deviceService.isDeviceExists("BAD"))
                .thenThrow(new CustomException("Device not registered", ErrorCode.NotRegistered));

        CustomException ex = assertThrows(CustomException.class, () -> renovationService.save(vo));
        assertEquals("Device not registered", ex.getMessage());

        verify(renovationRepository, never()).save(any());
    }
}
