package bd.bubt.shikkhasetu.blackbox;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * BLACK-BOX tests: only the HTTP API is used, exactly like the mobile app does.
 * Techniques: equivalence classes and boundary values (loan period, password
 * length, required fields) and a role/permission matrix.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiBlackBoxTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Value("${app.seed.coordinator-email}") String coordinatorEmail;
    @Value("${app.seed.coordinator-password}") String coordinatorPassword;

    // ---------- helpers ----------

    private ResultActions call(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request);
    }

    private JsonNode bodyOf(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return bodyOf(call(post("/api/auth/login"), null, body).andExpect(status().isOk())).get("token").asText();
    }

    private String coordinatorToken() throws Exception {
        return login(coordinatorEmail, coordinatorPassword);
    }

    private String newStudentToken() throws Exception {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        String body = "{\"name\":\"Test Student\",\"email\":\"" + email + "\",\"password\":\"secret123\"}";
        call(post("/api/auth/register"), null, body).andExpect(status().isCreated());
        return login(email, "secret123");
    }

    private long newItem(String token, String mode) throws Exception {
        String body = "{\"title\":\"Black-box item\",\"category\":\"CALCULATOR\",\"mode\":\"" + mode
                + "\",\"condition\":\"GOOD\"}";
        return bodyOf(call(post("/api/items"), token, body).andExpect(status().isCreated())).get("id").asLong();
    }

    private long newRequest(String studentToken, long itemId, int loanDays) throws Exception {
        String body = "{\"itemId\":" + itemId + ",\"loanDays\":" + loanDays + "}";
        return bodyOf(call(post("/api/requests"), studentToken, body).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    // ---------- authentication ----------

    @Test
    void health_isPublic() throws Exception {
        call(get("/api/health"), null, null).andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withoutToken_is401() throws Exception {
        call(get("/api/items"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withInvalidToken_is401() throws Exception {
        call(get("/api/items"), "not-a-real-token", null).andExpect(status().isUnauthorized());
    }

    @Test
    void login_withWrongPassword_is401() throws Exception {
        String body = "{\"email\":\"" + coordinatorEmail + "\",\"password\":\"wrong-password\"}";
        call(post("/api/auth/login"), null, body).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_invalidatesTheToken() throws Exception {
        String token = newStudentToken();
        call(post("/api/auth/logout"), token, null).andExpect(status().isOk());
        call(get("/api/me"), token, null).andExpect(status().isUnauthorized());
    }

    @Test
    void selfRegistration_alwaysCreatesAStudent() throws Exception {
        String token = newStudentToken();
        call(get("/api/me"), token, null).andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void register_duplicateEmail_is409() throws Exception {
        String body = "{\"name\":\"X\",\"email\":\"" + coordinatorEmail + "\",\"password\":\"secret123\"}";
        call(post("/api/auth/register"), null, body).andExpect(status().isConflict());
    }

    // Password length: invalid class < 6, boundary 5 | 6.
    @ParameterizedTest
    @CsvSource({ "12345, 400", "123456, 201" })
    void register_passwordLengthBoundary(String password, int expectedStatus) throws Exception {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        String body = "{\"name\":\"B\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        call(post("/api/auth/register"), null, body).andExpect(status().is(expectedStatus));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\":\"a@test.local\",\"password\":\"secret123\"}",           // name missing
            "{\"name\":\"A\",\"password\":\"secret123\"}",                         // e-mail missing
            "{\"name\":\"A\",\"email\":\"not-an-email\",\"password\":\"secret123\"}", // e-mail malformed
            "{\"name\":\"A\",\"email\":\"a@test.local\"}" })                       // password missing
    void register_missingOrInvalidField_is400(String body) throws Exception {
        call(post("/api/auth/register"), null, body).andExpect(status().isBadRequest());
    }

    // ---------- items ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"category\":\"BOOK\",\"mode\":\"LOAN\",\"condition\":\"GOOD\"}",                    // title missing
            "{\"title\":\" \",\"category\":\"BOOK\",\"mode\":\"LOAN\",\"condition\":\"GOOD\"}",   // title blank
            "{\"title\":\"T\",\"mode\":\"LOAN\",\"condition\":\"GOOD\"}",                          // category missing
            "{\"title\":\"T\",\"category\":\"LAPTOP\",\"mode\":\"LOAN\",\"condition\":\"GOOD\"}", // category unknown
            "{\"title\":\"T\",\"category\":\"BOOK\",\"condition\":\"GOOD\"}" })                    // mode missing
    void createItem_missingOrInvalidField_is400(String body) throws Exception {
        call(post("/api/items"), coordinatorToken(), body).andExpect(status().isBadRequest());
    }

    @Test
    void search_filtersByTextCategoryAndMode() throws Exception {
        String coordinator = coordinatorToken();
        String unique = "Zeta" + UUID.randomUUID().toString().substring(0, 6);
        String body = "{\"title\":\"" + unique + " calculator\",\"category\":\"CALCULATOR\",\"mode\":\"LOAN\","
                + "\"condition\":\"GOOD\"}";
        call(post("/api/items"), coordinator, body).andExpect(status().isCreated());
        String student = newStudentToken();

        call(get("/api/items").param("q", unique.toLowerCase()), student, null)
                .andExpect(jsonPath("$.length()").value(1));
        call(get("/api/items").param("q", unique).param("category", "CALCULATOR").param("mode", "LOAN"),
                student, null).andExpect(jsonPath("$.length()").value(1));
        call(get("/api/items").param("q", unique).param("category", "BOOK"), student, null)
                .andExpect(jsonPath("$.length()").value(0));
        call(get("/api/items").param("q", unique).param("mode", "DONATION"), student, null)
                .andExpect(jsonPath("$.length()").value(0));
        call(get("/api/items").param("category", "LAPTOP"), student, null).andExpect(status().isBadRequest());
    }

    // ---------- requests: loan period boundary values (valid range 1..30) ----------

    @ParameterizedTest
    @CsvSource({ "0, 400", "1, 201", "30, 201", "31, 400", "-5, 400" })
    void loanPeriod_boundaryValues(int loanDays, int expectedStatus) throws Exception {
        long itemId = newItem(coordinatorToken(), "LOAN");
        String body = "{\"itemId\":" + itemId + ",\"loanDays\":" + loanDays + "}";
        call(post("/api/requests"), newStudentToken(), body).andExpect(status().is(expectedStatus));
    }

    @Test
    void loanRequest_withoutLoanPeriod_is400() throws Exception {
        long itemId = newItem(coordinatorToken(), "LOAN");
        call(post("/api/requests"), newStudentToken(), "{\"itemId\":" + itemId + "}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void donationRequest_needsNoLoanPeriod() throws Exception {
        long itemId = newItem(coordinatorToken(), "DONATION");
        call(post("/api/requests"), newStudentToken(), "{\"itemId\":" + itemId + "}")
                .andExpect(status().isCreated());
    }

    @Test
    void request_forUnknownItem_is404_andWithoutItemId_is400() throws Exception {
        String student = newStudentToken();
        call(post("/api/requests"), student, "{\"itemId\":99999999,\"loanDays\":5}")
                .andExpect(status().isNotFound());
        call(post("/api/requests"), student, "{\"loanDays\":5}").andExpect(status().isBadRequest());
    }

    @Test
    void duplicateActiveRequest_is409() throws Exception {
        long itemId = newItem(coordinatorToken(), "LOAN");
        String student = newStudentToken();
        newRequest(student, itemId, 5);
        call(post("/api/requests"), student, "{\"itemId\":" + itemId + ",\"loanDays\":5}")
                .andExpect(status().isConflict());
    }

    // ---------- permissions ----------

    @Test
    void student_cannotUseCoordinatorActions() throws Exception {
        String coordinator = coordinatorToken();
        String student = newStudentToken();
        String otherStudent = newStudentToken();
        long itemId = newItem(coordinator, "LOAN");
        long requestId = newRequest(student, itemId, 5);
        long offeredId = newItem(student, "DONATION"); // waits for review

        call(post("/api/requests/" + requestId + "/approve"), student, null).andExpect(status().isForbidden());
        call(post("/api/requests/" + requestId + "/approve"), otherStudent, null).andExpect(status().isForbidden());
        call(post("/api/requests/" + requestId + "/reject"), student, null).andExpect(status().isForbidden());
        call(post("/api/items/" + itemId + "/allocate-fcfs"), student, null).andExpect(status().isForbidden());
        call(post("/api/requests/" + requestId + "/handover"), student, "{\"pickupCode\":\"123456\"}")
                .andExpect(status().isForbidden());
        call(post("/api/requests/" + requestId + "/return"), student, "{\"condition\":\"GOOD\"}")
                .andExpect(status().isForbidden());
        call(post("/api/items/" + offeredId + "/review"), student, "{\"approve\":true}")
                .andExpect(status().isForbidden());
        call(get("/api/requests"), student, null).andExpect(status().isForbidden());
    }

    @Test
    void student_cannotReadOrCancelAnotherStudentsRequest() throws Exception {
        long itemId = newItem(coordinatorToken(), "LOAN");
        String owner = newStudentToken();
        String stranger = newStudentToken();
        long requestId = newRequest(owner, itemId, 5);

        call(get("/api/requests/" + requestId), stranger, null).andExpect(status().isForbidden());
        call(post("/api/requests/" + requestId + "/cancel"), stranger, null).andExpect(status().isForbidden());
        call(get("/api/requests/" + requestId), owner, null).andExpect(status().isOk());
    }

    @Test
    void coordinator_cannotRequestItems() throws Exception {
        String coordinator = coordinatorToken();
        long itemId = newItem(coordinator, "LOAN");
        call(post("/api/requests"), coordinator, "{\"itemId\":" + itemId + ",\"loanDays\":5}")
                .andExpect(status().isForbidden());
    }

    @Test
    void pickupCode_isVisibleToTheRequesterOnly() throws Exception {
        String coordinator = coordinatorToken();
        String student = newStudentToken();
        long requestId = newRequest(student, newItem(coordinator, "LOAN"), 5);

        call(post("/api/requests/" + requestId + "/approve"), coordinator, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.pickupCode").isEmpty());
        call(get("/api/requests/" + requestId), student, null)
                .andExpect(jsonPath("$.pickupCode").isNotEmpty());
    }

    // ---------- end-to-end through the API ----------

    @Test
    void loanScenario_endToEnd() throws Exception {
        String coordinator = coordinatorToken();
        String student = newStudentToken();
        long itemId = newItem(coordinator, "LOAN");
        long requestId = newRequest(student, itemId, 7);

        call(post("/api/requests/" + requestId + "/approve"), coordinator, null).andExpect(status().isOk());
        String code = bodyOf(call(get("/api/requests/" + requestId), student, null)).get("pickupCode").asText();

        call(post("/api/requests/" + requestId + "/handover"), coordinator, "{\"pickupCode\":\"wrong\"}")
                .andExpect(status().isBadRequest());
        call(post("/api/requests/" + requestId + "/handover"), coordinator, "{\"pickupCode\":\"" + code + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HANDED_OVER"))
                .andExpect(jsonPath("$.item.status").value("ON_LOAN"))
                .andExpect(jsonPath("$.dueDate").isNotEmpty());
        // the code is single-use
        call(post("/api/requests/" + requestId + "/handover"), coordinator, "{\"pickupCode\":\"" + code + "\"}")
                .andExpect(status().isConflict());
        call(post("/api/requests/" + requestId + "/return"), coordinator, "{\"condition\":\"FAIR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.item.status").value("AVAILABLE"));

        call(get("/api/notifications"), student, null).andExpect(jsonPath("$.length()").value(3));
        call(get("/api/dashboard"), student, null).andExpect(jsonPath("$.completedLoans").isNumber());
    }
}
