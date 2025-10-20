package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.Passport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
@DataJpaTest
@ActiveProfiles("test")
public class PassportRepositoryTests {
    @Autowired
    PassportRepository passportRepository;
    @BeforeEach
    void setUp(){
        Passport passport = new Passport();
        passport.setId(1L);
        passport.setSerialPrefix("AB");
        passport.setName("test");
        passport.setModel("CD");
        passport.setFromSerialNumber(100);
        passport.setToSerialNumber(200);
        passportRepository.save(passport);
    }
    @AfterEach
    void tearDown(){
        passportRepository.deleteAll();
    }
    @Test
    void findFromSerialPrefixSuccess(){
        var result = passportRepository.findByFromSerial("AB");
        assertEquals(1,result.size());
    }
    @Test
    void findFromSerialPrefixFailure(){
        var result = passportRepository.findByFromSerial("FF");
        assertEquals(0,result.size());
    }
    @Test
    void findFromSerialNumberBetweenSuccess(){
        var result = passportRepository.findByFromSerialNumberBetween("AB",100,200);
        assertEquals(1,result.size());
    }
    @Test
    void findFromSerialNumberBetweenFailure(){
        var result = passportRepository.findByFromSerialNumberBetween("FF",100,200);
        assertEquals(0,result.size());
    }
}
