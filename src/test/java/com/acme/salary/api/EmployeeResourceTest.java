package com.acme.salary.api;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class EmployeeResourceTest {
    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Inject
    DataSource dataSource;

    @BeforeEach
    void seedFixture() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement deleteRequests = connection.prepareStatement("DELETE FROM compensation_request WHERE employee_id = ?")) {
                deleteRequests.setObject(1, EMPLOYEE_ID);
                deleteRequests.executeUpdate();
            }
            try (PreparedStatement deleteHistory = connection.prepareStatement("DELETE FROM salary_change WHERE employee_id = ?")) {
                deleteHistory.setObject(1, EMPLOYEE_ID);
                deleteHistory.executeUpdate();
            }
            try (PreparedStatement employee = connection.prepareStatement("""
                    INSERT INTO employee (id, employee_number, first_name, last_name, email, department, country_code, currency_code, annual_salary)
                    VALUES (?, 'TEST00001', 'Riya', 'Rao', 'riya.rao@example.test', 'Engineering', 'IN', 'INR', 125000.00)
                    ON CONFLICT (employee_number) DO UPDATE SET annual_salary = EXCLUDED.annual_salary,
                    currency_code = EXCLUDED.currency_code, id = EXCLUDED.id
                    """)) {
                employee.setObject(1, EMPLOYEE_ID);
                employee.executeUpdate();
            }
        }
    }

    @AfterEach
    void removeFixture() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement requests = connection.prepareStatement("DELETE FROM compensation_request WHERE employee_id = ?")) {
                requests.setObject(1, EMPLOYEE_ID);
                requests.executeUpdate();
            }
            try (PreparedStatement history = connection.prepareStatement("DELETE FROM salary_change WHERE employee_id = ?")) {
                history.setObject(1, EMPLOYEE_ID);
                history.executeUpdate();
            }
            try (PreparedStatement employee = connection.prepareStatement("DELETE FROM employee WHERE id = ?")) {
                employee.setObject(1, EMPLOYEE_ID);
                employee.executeUpdate();
            }
        }
    }

    @Test
    void searchesEmployeesAndUsesBoundedPagination() {
        given().queryParam("query", "TEST00001").queryParam("page", 0).queryParam("size", 10)
                .when().get("/api/v1/employees")
                .then().statusCode(200).body("total", greaterThanOrEqualTo(1)).body("items[0].employeeNumber", equalTo("TEST00001"));
        given().queryParam("size", 101).when().get("/api/v1/employees")
                .then().statusCode(400).body("error", equalTo("size must be between 1 and 100"));
    }

    @Test
    void updatesSalaryAndMakesHistoryAuditable() {
        given().contentType("application/json").body("{\"annualSalary\":\"130000.50\",\"currencyCode\":\"INR\"}")
                .when().put("/api/v1/employees/" + EMPLOYEE_ID + "/salary")
                .then().statusCode(200).body("annualSalary", equalTo(130000.5f)).body("currencyCode", equalTo("INR"));
        given().when().get("/api/v1/employees/" + EMPLOYEE_ID + "/salary-history")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].previousSalary", equalTo(125000.0f));
    }

    @Test
    void rejectsInvalidSalaryWithoutChangingEmployee() {
        given().contentType("application/json").body("{\"annualSalary\":-1,\"currencyCode\":\"INR\"}")
                .when().put("/api/v1/employees/" + EMPLOYEE_ID + "/salary")
                .then().statusCode(400).body("error", equalTo("annualSalary must not be negative"));
        given().when().get("/api/v1/employees/" + EMPLOYEE_ID)
                .then().statusCode(200).body("annualSalary", equalTo(125000.0f));
    }

    @Test
    void summaryReturnsCurrenciesSeparately() {
        given().when().get("/api/v1/summary")
                .then().statusCode(200).body("headcount", greaterThanOrEqualTo(1))
                .body("salariesByCurrency.currencyCode", hasItem("INR"));
    }

    @Test
    void compensationApprovalUpdatesSalaryAndAuditTrail() {
        String requestId = given().contentType("application/json")
                .body("{\"employeeId\":\"" + EMPLOYEE_ID + "\",\"requestType\":\"Merit increase\",\"proposedSalary\":\"140000.00\",\"effectiveDate\":\"2026-11-01\",\"justification\":\"Expanded role\",\"requester\":\"Riya Rao\"}")
                .when().post("/api/v1/compensation-requests")
                .then().statusCode(200).body("status", equalTo("Pending")).extract().path("id");

        given().contentType("application/json").body("{\"approved\":true,\"decidedBy\":\"Test HR\"}")
                .when().post("/api/v1/compensation-requests/" + requestId + "/decision")
                .then().statusCode(200).body("status", equalTo("Approved"));
        given().when().get("/api/v1/employees/" + EMPLOYEE_ID)
                .then().statusCode(200).body("annualSalary", equalTo(140000.0f));
        given().when().get("/api/v1/employees/" + EMPLOYEE_ID + "/salary-history")
                .then().statusCode(200).body("size()", equalTo(1)).body("[0].reason", equalTo("Expanded role"));
    }
}
