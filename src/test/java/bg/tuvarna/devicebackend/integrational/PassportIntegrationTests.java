package bg.tuvarna.devicebackend.integrational;


import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorResponse;
import bg.tuvarna.devicebackend.models.dtos.AuthResponseDTO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.Passport;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import bg.tuvarna.devicebackend.repositories.DeviceRepository;
import bg.tuvarna.devicebackend.repositories.PassportRepository;
import bg.tuvarna.devicebackend.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PassportIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper mapper;

    @Autowired private UserRepository userRepository;
    @Autowired private PassportRepository passportRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static String adminToken;
    private static String userToken;
    private static Long createdPassportId;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @BeforeAll
    static void beforeAll(
            @Autowired UserRepository userRepository,
            @Autowired PassportRepository passportRepository,
            @Autowired DeviceRepository deviceRepository
    ) {
        deviceRepository.deleteAllInBatch();
        passportRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @AfterAll
    static void afterAll(
            @Autowired UserRepository userRepository,
            @Autowired PassportRepository passportRepository,
            @Autowired DeviceRepository deviceRepository
    ) {
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

    @Test
    @Order(1)
    void seedAdminAndUserAndLogin() throws Exception {
        User user = User.builder()
                .fullName("User One")
                .email("user1@abv.bg")
                .phone("0888000001")
                .password(passwordEncoder.encode("User$12345"))
                .role(UserRole.USER)
                .build();
        userRepository.save(user);

        User admin = User.builder()
                .fullName("Admin One")
                .email("admin1@abv.bg")
                .phone("0888000002")
                .password(passwordEncoder.encode("Admin$12345"))
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);

        userToken = loginAndGetToken("user1@abv.bg", "User$12345");
        adminToken = loginAndGetToken("admin1@abv.bg", "Admin$12345");

        assertNotNull(userToken);
        assertNotNull(adminToken);
    }

    @Test
    @Order(2)
    void createPassport_AsAdmin_Success201() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/passports")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "TestDevice",
                                  "model": "ModelX",
                                  "serialPrefix": "AB",
                                  "warrantyMonths": 24,
                                  "fromSerialNumber": 100,
                                  "toSerialNumber": 999
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/passports/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("TestDevice"))
                .andExpect(jsonPath("$.model").value("ModelX"))
                .andExpect(jsonPath("$.serialPrefix").value("AB"))
                .andExpect(jsonPath("$.fromSerialNumber").value(100))
                .andExpect(jsonPath("$.toSerialNumber").value(999))
                .andExpect(jsonPath("$.warrantyMonths").value(24))
                .andReturn();

        createdPassportId = mapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        assertNotNull(createdPassportId);
        assertTrue(passportRepository.existsById(createdPassportId));
    }

    @Test
    @Order(3)
    void createPassport_AsUser_Forbidden403() throws Exception {
        mvc.perform(post("/api/v1/passports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ShouldFail",
                                  "model": "Nope",
                                  "serialPrefix": "ZZ",
                                  "warrantyMonths": 12,
                                  "fromSerialNumber": 1,
                                  "toSerialNumber": 10
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void getPassports_AsAdmin_ReturnsCustomPage200() throws Exception {
        mvc.perform(get("/api/v1/passports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.items[0].id").value(createdPassportId));
    }

    @Test
    @Order(5)
    void updatePassport_AsAdmin_Success200() throws Exception {
        mvc.perform(put("/api/v1/passports/" + createdPassportId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "UpdatedName",
                                  "warrantyMonths": 36
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdPassportId))
                .andExpect(jsonPath("$.name").value("UpdatedName"))
                .andExpect(jsonPath("$.warrantyMonths").value(36));
    }

    @Test
    @Order(6)
    void getPassportBySerialId_PublicEndpoint_Success200() throws Exception {
        mvc.perform(get("/api/v1/passports/getBySerialId/AB150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdPassportId))
                .andExpect(jsonPath("$.name").value("UpdatedName"))
                .andExpect(jsonPath("$.model").value("ModelX"));
    }

    @Test
    @Order(7)
    void getPassportBySerialId_PublicEndpoint_NotFound400() throws Exception {
        MvcResult res = mvc.perform(get("/api/v1/passports/getBySerialId/XX999"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse err = mapper.readValue(res.getResponse().getContentAsString(), ErrorResponse.class);
        assertTrue(err.getError().startsWith("Passport not found for serial number: "));
        assertEquals(ErrorCode.Failed.getCode(), err.getErrorCode());
    }

    @Test
    @Order(8)
    void deletePassport_AsAdmin_Success200_ThenCannotResolveSerial() throws Exception {
        mvc.perform(delete("/api/v1/passports/" + createdPassportId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertFalse(passportRepository.existsById(createdPassportId));

        MvcResult res = mvc.perform(get("/api/v1/passports/getBySerialId/AB150"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse err = mapper.readValue(res.getResponse().getContentAsString(), ErrorResponse.class);
        assertEquals(ErrorCode.Failed.getCode(), err.getErrorCode());
    }
}
