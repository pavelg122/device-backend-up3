package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.Renovation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class RenovationRepositoryTests {
    @Autowired
    private RenovationRepository renovationRepository;

    @Autowired
    private EntityManager entityManager;

    private void addRenovation(Device device, String desc, LocalDate date) {
        Renovation r = new Renovation();
        r.setDescription(desc);
        r.setRenovationDate(date);
        r.setDevice(device);
        device.getRenovations().add(r);
    }

    @Test
    public void testRepositoryLoads() {
        assertNotNull(renovationRepository);
    }

    @Test
    public void testSaveAndFindById() {
        Device device = new Device();
        device.setSerialNumber("SN-1");

        addRenovation(device, "Screen replacement", LocalDate.of(2025, 1, 10));

        entityManager.persist(device);
        entityManager.flush();
        entityManager.clear();

        Device foundDevice = entityManager.find(Device.class, "SN-1");
        assertNotNull(foundDevice);
        assertEquals(1, foundDevice.getRenovations().size());

        Long renovationId = foundDevice.getRenovations().get(0).getId();
        Optional<Renovation> found = renovationRepository.findById(renovationId);

        assertTrue(found.isPresent());
        assertEquals("Screen replacement", found.get().getDescription());
        assertEquals(LocalDate.of(2025, 1, 10), found.get().getRenovationDate());
        assertNotNull(found.get().getDevice());
        assertEquals("SN-1", found.get().getDevice().getSerialNumber());
    }

    @Test
    public void testFindAllPaging() {
        Device d1 = new Device();
        d1.setSerialNumber("SN-1");
        addRenovation(d1, "R1", LocalDate.of(2025, 1, 1));
        entityManager.persist(d1);

        Device d2 = new Device();
        d2.setSerialNumber("SN-2");
        addRenovation(d2, "R2", LocalDate.of(2025, 1, 2));
        entityManager.persist(d2);

        entityManager.flush();
        entityManager.clear();

        Page<Renovation> page = renovationRepository.findAll(PageRequest.of(0, 1));
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    public void testDeleteByOrphanRemoval() {
        Device device = new Device();
        device.setSerialNumber("SN-DEL");

        addRenovation(device, "To be deleted", LocalDate.of(2025, 2, 1));

        entityManager.persist(device);
        entityManager.flush();
        entityManager.clear();

        Device managedDevice = entityManager.find(Device.class, "SN-DEL");
        assertEquals(1, managedDevice.getRenovations().size());

        Long renovationId = managedDevice.getRenovations().get(0).getId();
        assertTrue(renovationRepository.existsById(renovationId));

        managedDevice.getRenovations().clear();

        entityManager.flush();
        entityManager.clear();

        assertFalse(renovationRepository.existsById(renovationId));

        Long cnt = entityManager.createQuery(
                        "select count(r) from Renovation r where r.id = :id", Long.class)
                .setParameter("id", renovationId)
                .getSingleResult();
        assertEquals(0L, cnt);
    }

    @Test
    public void testDeleteByIdWorksWhenUnlinkedFirst() {
        Device device = new Device();
        device.setSerialNumber("SN-DEL2");

        addRenovation(device, "To be deleted", LocalDate.of(2025, 2, 2));

        entityManager.persist(device);
        entityManager.flush();
        entityManager.clear();

        Device managedDevice = entityManager.find(Device.class, "SN-DEL2");
        Long renovationId = managedDevice.getRenovations().getFirst().getId();

        managedDevice.getRenovations().clear();
        entityManager.flush();
        entityManager.clear();

        assertFalse(renovationRepository.existsById(renovationId));

        assertDoesNotThrow(() -> renovationRepository.deleteById(renovationId));
    }
}
