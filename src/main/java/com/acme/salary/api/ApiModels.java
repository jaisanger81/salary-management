package com.acme.salary.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() { }

    public record EmployeeQuery(String query, String country, String department, String location,
                                String status, int page, int size) { }
    public record EmployeeWriteRequest(String firstName, String lastName, String email, String department,
                                       String countryCode, String currencyCode, BigDecimal annualSalary,
                                       String jobTitle, String managerName, String location, String employmentStatus) { }
    public record SalaryUpdateRequest(BigDecimal annualSalary, String currencyCode, String reason) { }
    public record EmployeeDto(UUID id, String employeeNumber, String firstName, String lastName,
                              String email, String department, String countryCode, String currencyCode,
                              BigDecimal annualSalary, OffsetDateTime updatedAt, String jobTitle,
                              String managerName, String location, String employmentStatus) { }
    public record EmployeePage(List<EmployeeDto> items, long total, int page, int size) { }
    public record CurrencyTotal(String currencyCode, long employeeCount, BigDecimal annualSalaryTotal) { }
    public record DepartmentTotal(String department, long employeeCount, BigDecimal annualSalaryAverage) { }
    public record SummaryDto(long headcount, List<CurrencyTotal> salariesByCurrency,
                             long activeCount, long leaveCount, long inactiveCount,
                             List<DepartmentTotal> departments, long salaryChangeCount) { }
    public record SalaryChangeDto(UUID id, BigDecimal previousSalary, BigDecimal newSalary,
                                  String previousCurrency, String newCurrency,
                                  OffsetDateTime changedAt, String changedBy, String reason) { }
    public record MetadataDto(List<String> countries, List<String> departments, List<String> locations) { }
    public record CompensationRequestDto(UUID id, UUID employeeId, String employeeNumber,
            String firstName, String lastName, String department, String requestType,
            BigDecimal currentSalary, BigDecimal proposedSalary, String currencyCode,
            LocalDate effectiveDate, String justification, String requester, String status,
            OffsetDateTime createdAt, OffsetDateTime decidedAt, String decidedBy) { }
    public record CompensationRequestInput(UUID employeeId, String requestType, BigDecimal proposedSalary,
            LocalDate effectiveDate, String justification, String requester) { }
    public record DecisionInput(boolean approved, String decidedBy) { }
    public record QualityIssueDto(String id, String category, String issueDescription,
            String severity, String owner, String status, OffsetDateTime updatedAt) { }
    public record QualityIssueUpdate(String status) { }
    public record ErrorDto(String error) { }
    public record SalaryChangeCommand(UUID employeeId, SalaryUpdateRequest request) { }
    public record EmployeeIdCommand(UUID employeeId) { }
    public record SalaryChangeIdCommand(UUID employeeId) { }
    public record EmployeeWriteCommand(UUID employeeId, EmployeeWriteRequest request) { }
    public record CompensationDecisionCommand(UUID requestId, DecisionInput decision) { }
}
