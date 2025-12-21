package bg.tuvarna.devicebackend.api;

import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorResponse;
import bg.tuvarna.devicebackend.models.dtos.AuthResponseDTO;
import bg.tuvarna.devicebackend.models.entities.Passport;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import bg.tuvarna.devicebackend.repositories.DeviceRepository;
import bg.tuvarna.devicebackend.repositories.PassportRepository;
import bg.tuvarna.devicebackend.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PassportApiTests {
    @Autowired private MockMvc mvc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper mapper;

    @Autowired private UserRepository userRepository;
    @Autowired private PassportRepository passportRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        deviceRepository.deleteAllInBatch();
        passportRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User user = User.builder()
                .fullName("gosho")
                .email("gosho@abv.bg")
                .phone("1111111111")
                .password(passwordEncoder.encode("Az$um_GOSHO123"))
                .role(UserRole.USER)
                .build();
        userRepository.save(user);

        User admin = User.builder()
                .fullName("admin")
                .email("admin@abv.bg")
                .phone("2222222222")
                .password(passwordEncoder.encode("Admin$12345"))
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);
    }

    @AfterEach
    void tearDown() {
        deviceRepository.deleteAllInBatch();
        passportRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDTO auth = mapper.readValue(login.getResponse().getContentAsString(), AuthResponseDTO.class);
        assertNotNull(auth.getToken());
        return auth.getToken();
    }

    private Passport seedPassport(
            String name, String model, String prefix, int from, int to, int warrantyMonths
    ) {
        Passport p = new Passport();
        p.setName(name);
        p.setModel(model);
        p.setSerialPrefix(prefix);
        p.setFromSerialNumber(from);
        p.setToSerialNumber(to);
        p.setWarrantyMonths(warrantyMonths);
        return passportRepository.save(p);
    }


    @Test
    void getBySerialId_PermitAll_Returns200_PassportForSerialNumberVO() throws Exception {
        Passport p = seedPassport("SeedName", "SeedModel", "AB", 100, 999, 24);

        mvc.perform(get("/api/v1/passports/getBySerialId/AB150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId()))
                .andExpect(jsonPath("$.name").value("SeedName"))
                .andExpect(jsonPath("$.model").value("SeedModel"));
    }

    @Test
    void getBySerialId_PermitAll_NotFound_Returns400_ErrorResponse() throws Exception {
        MvcResult res = mvc.perform(get("/api/v1/passports/getBySerialId/AB150"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse err = mapper.readValue(res.getResponse().getContentAsString(), ErrorResponse.class);
        assertTrue(err.getError().startsWith("Passport not found for serial number: "));
        // from your service: ErrorCode.Failed -> numeric code depends on your enum
        assertTrue(err.getErrorCode() > 0);
        assertNotNull(err.getTimestamp());
        assertNotNull(err.getType());
    }


    @Test
    void createPassport_WithoutToken_Is401() throws Exception {
        mvc.perform(post("/api/v1/passports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Phone",
                                  "model": "X",
                                  "serialPrefix": "AB",
                                  "warrantyMonths": 24,
                                  "fromSerialNumber": 100,
                                  "toSerialNumber": 999
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPassport_WithUserToken_Is403() throws Exception {
        String userToken = loginAndGetToken("gosho@abv.bg", "Az$um_GOSHO123");

        mvc.perform(post("/api/v1/passports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Phone",
                                  "model": "X",
                                  "serialPrefix": "AB",
                                  "warrantyMonths": 24,
                                  "fromSerialNumber": 100,
                                  "toSerialNumber": 999
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPassport_WithAdminToken_Success201_ReturnsPassportVO() throws Exception {
        String adminToken = loginAndGetToken("admin@abv.bg", "Admin$12345");

        mvc.perform(post("/api/v1/passports")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Phone",
                                  "model": "X",
                                  "serialPrefix": "AB",
                                  "warrantyMonths": 24,
                                  "fromSerialNumber": 100,
                                  "toSerialNumber": 999
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/passports/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.model").value("X"))
                .andExpect(jsonPath("$.serialPrefix").value("AB"))
                .andExpect(jsonPath("$.fromSerialNumber").value(100))
                .andExpect(jsonPath("$.toSerialNumber").value(999))
                .andExpect(jsonPath("$.warrantyMonths").value(24));
    }

    @Test
    void createPassport_DuplicateRange_Returns400_ErrorResponse() throws Exception {
        String adminToken = loginAndGetToken("admin@abv.bg", "Admin$12345");
        seedPassport("Existing", "M", "AB", 100, 999, 24);

        MvcResult res = mvc.perform(post("/api/v1/passports")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New",
                                  "model": "Z",
                                  "serialPrefix": "AB",
                                  "warrantyMonths": 12,
                                  "fromSerialNumber": 100,
                                  "toSerialNumber": 999
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse err = mapper.readValue(res.getResponse().getContentAsString(), ErrorResponse.class);
        assertEquals("Serial number already exists", err.getError());
        assertTrue(err.getErrorCode() > 0);
    }

    @Test
    void updatePassport_WithAdminToken_Success200() throws Exception {
        String adminToken = loginAndGetToken("admin@abv.bg", "Admin$12345");
        Passport existing = seedPassport("Old", "OldModel", "AB", 100, 999, 24);

        mvc.perform(put("/api/v1/passports/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated",
                                  "model": "NewModel",
                                  "warrantyMonths": 36
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.model").value("NewModel"))
                .andExpect(jsonPath("$.serialPrefix").value("AB"))
                .andExpect(jsonPath("$.fromSerialNumber").value(100))
                .andExpect(jsonPath("$.toSerialNumber").value(999))
                .andExpect(jsonPath("$.warrantyMonths").value(36));
    }

    @Test
    void updatePassport_NotFound_Returns400_ErrorResponse() throws Exception {
        String adminToken = loginAndGetToken("admin@abv.bg", "Admin$12345");

        MvcResult res = mvc.perform(put("/api/v1/passports/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse err = mapper.readValue(res.getResponse().getContentAsString(), ErrorResponse.class);
        assertEquals("Passport not found", err.getError());
        assertEquals(ErrorCode.EntityNotFound.getCode(), err.getErrorCode());
    }

    @Test
    void getPassports_WithAdminToken_Success200_ReturnsCustomPage() throws Exception {
        String adminToken = loginAndGetToken("admin@abv.bg", "Admin$12345");
        seedPassport("P1", "M1", "AB", 100, 199, 24);
        seedPassport("P2", "M2", "CD", 1, 10, 12);

        mvc.perform(get("/api/v1/passports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void deletePassport_WithAdminToken_Success200() throws Exception {
        String adminToken = loginAndGetToken("admin@abv.bg", "Admin$12345");
        Passport existing = seedPassport("ToDelete", "M", "AB", 100, 999, 24);

        mvc.perform(delete("/api/v1/passports/" + existing.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertFalse(passportRepository.existsById(existing.getId()));
    }

    @Test
    void deletePassport_WithUserToken_Is403() throws Exception {
        String userToken = loginAndGetToken("gosho@abv.bg", "Az$um_GOSHO123");
        Passport existing = seedPassport("ToDelete", "M", "AB", 100, 999, 24);

        mvc.perform(delete("/api/v1/passports/" + existing.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
