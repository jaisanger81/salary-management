package com.acme.salary.integration;

import com.acme.salary.api.ApiModels;
import com.acme.salary.service.EmployeeService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class EmployeeRoutes extends RouteBuilder {
    @Inject
    EmployeeService employeeService;

    @Override
    public void configure() {
        from("direct:employees-search")
                .routeId("employees-search")
                .process(exchange -> exchange.getMessage().setBody(
                        employeeService.search(exchange.getMessage().getBody(ApiModels.EmployeeQuery.class))));

        from("direct:employee-detail")
                .routeId("employee-detail")
                .process(exchange -> exchange.getMessage().setBody(
                        employeeService.getEmployee(exchange.getMessage().getBody(ApiModels.EmployeeIdCommand.class).employeeId())));

        from("direct:salary-history")
                .routeId("salary-history")
                .process(exchange -> exchange.getMessage().setBody(
                        employeeService.getSalaryHistory(exchange.getMessage().getBody(ApiModels.SalaryChangeIdCommand.class).employeeId())));

        from("direct:salary-update")
                .routeId("salary-update")
                .process(exchange -> {
                    ApiModels.SalaryChangeCommand command = exchange.getMessage().getBody(ApiModels.SalaryChangeCommand.class);
                    exchange.getMessage().setBody(employeeService.updateSalary(command.employeeId(), command.request()));
                });

        from("direct:salary-summary")
                .routeId("salary-summary")
                .process(exchange -> exchange.getMessage().setBody(employeeService.getSummary()));

        from("direct:employee-save")
                .routeId("employee-save")
                .process(exchange -> {
                    ApiModels.EmployeeWriteCommand command = exchange.getMessage().getBody(ApiModels.EmployeeWriteCommand.class);
                    exchange.getMessage().setBody(employeeService.saveEmployee(command.employeeId(), command.request()));
                });

        from("direct:compensation-list")
                .routeId("compensation-list")
                .process(exchange -> exchange.getMessage().setBody(employeeService.listCompensation(exchange.getMessage().getBody(String.class))));

        from("direct:compensation-create")
                .routeId("compensation-create")
                .process(exchange -> exchange.getMessage().setBody(employeeService.createCompensation(exchange.getMessage().getBody(ApiModels.CompensationRequestInput.class))));

        from("direct:compensation-decision")
                .routeId("compensation-decision")
                .process(exchange -> {
                    ApiModels.CompensationDecisionCommand command = exchange.getMessage().getBody(ApiModels.CompensationDecisionCommand.class);
                    exchange.getMessage().setBody(employeeService.decideCompensation(command.requestId(), command.decision()));
                });

        from("direct:issues-list")
                .routeId("issues-list")
                .process(exchange -> exchange.getMessage().setBody(employeeService.listIssues()));

        from("direct:issue-update")
                .routeId("issue-update")
                .process(exchange -> {
                    Object[] command = exchange.getMessage().getBody(Object[].class);
                    exchange.getMessage().setBody(employeeService.updateIssue((String) command[0], (String) command[1]));
                });

        from("direct:employees-export")
                .routeId("employees-export")
                .process(exchange -> exchange.getMessage().setBody(employeeService.exportEmployees()));

        from("direct:employee-metadata")
                .routeId("employee-metadata")
                .process(exchange -> exchange.getMessage().setBody(employeeService.getMetadata()));
    }
}
