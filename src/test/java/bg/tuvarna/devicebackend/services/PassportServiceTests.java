package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.PassportCreateVO;
import bg.tuvarna.devicebackend.models.dtos.PassportUpdateVO;
import bg.tuvarna.devicebackend.models.entities.Passport;
import bg.tuvarna.devicebackend.repositories.PassportRepository;
import bg.tuvarna.devicebackend.utils.CustomPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PassportServiceTests {
    @MockBean
    private PassportRepository passportRepository;

    @Autowired
    private PassportService passportService;

    @Test
    public void testCreateThrowsWhenSerialRangeExists() {
        PassportCreateVO vo = new PassportCreateVO(
                "name", "model", "AB",
                24, 1, 10
        );

        when(passportRepository.findByFromSerialNumberBetween("AB", 1, 10))
                .thenReturn(List.of(new Passport()));

        CustomException ex = assertThrows(CustomException.class, () -> passportService.create(vo));
        assertEquals("Serial number already exists", ex.getMessage());
        assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    @Test
    public void testCreateSuccessSavesPassport() {
        PassportCreateVO vo = new PassportCreateVO(
                "DeviceName", "DeviceModel", "AB",
                24, 1, 10
        );

        when(passportRepository.findByFromSerialNumberBetween("AB", 1, 10))
                .thenReturn(List.of());

        when(passportRepository.save(any(Passport.class))).thenAnswer(inv -> inv.getArgument(0));

        Passport created = passportService.create(vo);

        assertNotNull(created);
        assertEquals("DeviceName", created.getName());
        assertEquals("DeviceModel", created.getModel());
        assertEquals("AB", created.getSerialPrefix());
        assertEquals(24, created.getWarrantyMonths());
        assertEquals(1, created.getFromSerialNumber());
        assertEquals(10, created.getToSerialNumber());
    }

    @Test
    public void testUpdateThrowsWhenPassportNotFound() {
        when(passportRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> passportService.update(1L, new PassportUpdateVO(
                        "n", "m", "ZZ", 12, 1, 2
                )));

        assertEquals("Passport not found", ex.getMessage());
        assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    @Test
    public void testUpdateThrowsWhenSerialRangeConflictsWithOtherPassport() {
        Passport existing = new Passport();
        existing.setId(1L);
        existing.setName("OldName");
        existing.setModel("OldModel");
        existing.setSerialPrefix("AB");
        existing.setFromSerialNumber(1);
        existing.setToSerialNumber(10);
        existing.setWarrantyMonths(12);

        Passport other = new Passport();
        other.setId(2L);

        when(passportRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passportRepository.findByFromSerialNumberBetween("AB", 1, 10)).thenReturn(List.of(other));

        CustomException ex = assertThrows(CustomException.class,
                () -> passportService.update(1L, new PassportUpdateVO(
                        null, null, null, 36, null, null
                )));

        assertEquals("Serial number already exists", ex.getMessage());
        assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    @Test
    public void testUpdateSuccessUsesFallbackValuesWhenNull() {
        Passport existing = new Passport();
        existing.setId(1L);
        existing.setName("OldName");
        existing.setModel("OldModel");
        existing.setSerialPrefix("AB");
        existing.setFromSerialNumber(1);
        existing.setToSerialNumber(10);
        existing.setWarrantyMonths(12);

        when(passportRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passportRepository.findByFromSerialNumberBetween("AB", 1, 10))
                .thenReturn(List.of(existing));

        when(passportRepository.save(any(Passport.class))).thenAnswer(inv -> inv.getArgument(0));

        Passport updated = passportService.update(1L, new PassportUpdateVO(
                null, null, null, 36, null, null
        ));

        assertNotNull(updated);

        assertEquals("OldName", updated.getName());
        assertEquals("OldModel", updated.getModel());
        assertEquals("AB", updated.getSerialPrefix());
        assertEquals(1, updated.getFromSerialNumber());
        assertEquals(10, updated.getToSerialNumber());

        assertEquals(36, updated.getWarrantyMonths());
    }

    @Test
    public void testUpdateSuccessOverridesValuesWhenProvided() {
        Passport existing = new Passport();
        existing.setId(1L);
        existing.setName("OldName");
        existing.setModel("OldModel");
        existing.setSerialPrefix("AB");
        existing.setFromSerialNumber(1);
        existing.setToSerialNumber(10);
        existing.setWarrantyMonths(12);

        when(passportRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passportRepository.findByFromSerialNumberBetween("XY", 50, 60))
                .thenReturn(List.of(existing));
        when(passportRepository.save(any(Passport.class))).thenAnswer(inv -> inv.getArgument(0));

        Passport updated = passportService.update(1L, new PassportUpdateVO(
                "NewName", "NewModel", "XY", 48, 50, 60
        ));

        assertEquals("NewName", updated.getName());
        assertEquals("NewModel", updated.getModel());
        assertEquals("XY", updated.getSerialPrefix());
        assertEquals(48, updated.getWarrantyMonths());
        assertEquals(50, updated.getFromSerialNumber());
        assertEquals(60, updated.getToSerialNumber());
    }

    @Test
    public void testFindPassportBySerialIdReturnsMatchingPassport() {
        Passport p = new Passport();
        p.setId(1L);
        p.setSerialPrefix("AB");
        p.setFromSerialNumber(100);
        p.setToSerialNumber(200);

        when(passportRepository.findByFromSerial("AB150")).thenReturn(List.of(p));

        Passport found = passportService.findPassportBySerialId("AB150");
        assertEquals(p, found);
    }

    @Test
    public void testFindPassportBySerialIdSkipsNumberFormatAndFindsNext() {
        Passport bad = new Passport();
        bad.setSerialPrefix("AB");
        bad.setFromSerialNumber(1);
        bad.setToSerialNumber(10);

        Passport good = new Passport();
        good.setSerialPrefix("ABX");
        good.setFromSerialNumber(1);
        good.setToSerialNumber(10);

        when(passportRepository.findByFromSerial("ABX5")).thenReturn(List.of(bad, good));

        Passport found = passportService.findPassportBySerialId("ABX5");
        assertEquals(good, found);
    }

    @Test
    public void testFindPassportBySerialIdThrowsWhenNoMatch() {
        Passport p = new Passport();
        p.setSerialPrefix("AB");
        p.setFromSerialNumber(1);
        p.setToSerialNumber(2);

        when(passportRepository.findByFromSerial("AB999")).thenReturn(List.of(p));

        CustomException ex = assertThrows(CustomException.class,
                () -> passportService.findPassportBySerialId("AB999"));

        assertTrue(ex.getMessage().startsWith("Passport not found for serial number: "));
        assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }

    @Test
    public void testGetPassportsBuildsCustomPage() {
        Passport p = new Passport();
        p.setId(1L);

        Page<Passport> page = new PageImpl<>(List.of(p), PageRequest.of(0, 5), 1);
        when(passportRepository.findAll(any(PageRequest.class))).thenReturn(page);

        CustomPage<Passport> result = passportService.getPassports(1, 5);

        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getTotalItems());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getCurrentPage());
        assertEquals(5, result.getSize());
    }

    @Test
    public void testGetPassportsBySerialPrefixDelegatesToRepo() {
        Passport p = new Passport();
        when(passportRepository.findByFromSerial("AB123")).thenReturn(List.of(p));

        List<Passport> result = passportService.getPassportsBySerialPrefix("AB123");
        assertEquals(1, result.size());
        verify(passportRepository).findByFromSerial("AB123");
    }

    @Test
    public void testDeleteSuccess() {
        doNothing().when(passportRepository).deleteById(1L);

        assertDoesNotThrow(() -> passportService.delete(1L));
        verify(passportRepository).deleteById(1L);
    }

    @Test
    public void testDeleteFailureWrapsRuntimeException() {
        doThrow(new RuntimeException("fk")).when(passportRepository).deleteById(1L);

        CustomException ex = assertThrows(CustomException.class, () -> passportService.delete(1L));
        assertEquals("Can't delete passport", ex.getMessage());
        assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }
}
