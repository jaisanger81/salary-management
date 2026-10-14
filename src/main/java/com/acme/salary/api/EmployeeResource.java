package com.acme.salary.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class EmployeeResource {
    @Inject ProducerTemplate producer;

    @GET @Path("/employees")
    public ApiModels.EmployeePage searchEmployees(@QueryParam("query") String query,
            @QueryParam("country") String country,@QueryParam("department") String department,
            @QueryParam("location") String location,@QueryParam("status") String status,
            @QueryParam("page") Integer page,@QueryParam("size") Integer size) {
        int p=page==null?0:page,s=size==null?25:size;
        if(p<0||p>1_000_000)throw new IllegalArgumentException("page must be between 0 and 1000000");
        if(s<1||s>100)throw new IllegalArgumentException("size must be between 1 and 100");
        return producer.requestBody("direct:employees-search",new ApiModels.EmployeeQuery(query,country,department,location,status,p,s),ApiModels.EmployeePage.class);
    }
    @POST @Path("/employees") @Consumes(MediaType.APPLICATION_JSON)
    public ApiModels.EmployeeDto createEmployee(ApiModels.EmployeeWriteRequest request){return producer.requestBody("direct:employee-save",new ApiModels.EmployeeWriteCommand(null,request),ApiModels.EmployeeDto.class);}
    @GET @Path("/employees/{id}") public ApiModels.EmployeeDto getEmployee(@PathParam("id") UUID id){return producer.requestBody("direct:employee-detail",new ApiModels.EmployeeIdCommand(id),ApiModels.EmployeeDto.class);}
    @PUT @Path("/employees/{id}") @Consumes(MediaType.APPLICATION_JSON)
    public ApiModels.EmployeeDto editEmployee(@PathParam("id") UUID id,ApiModels.EmployeeWriteRequest request){return producer.requestBody("direct:employee-save",new ApiModels.EmployeeWriteCommand(id,request),ApiModels.EmployeeDto.class);}
    @GET @Path("/employees/{id}/salary-history") public List<ApiModels.SalaryChangeDto> getSalaryHistory(@PathParam("id") UUID id){return producer.requestBody("direct:salary-history",new ApiModels.SalaryChangeIdCommand(id),List.class);}
    @PUT @Path("/employees/{id}/salary") @Consumes(MediaType.APPLICATION_JSON)
    public ApiModels.EmployeeDto updateSalary(@PathParam("id") UUID id,ApiModels.SalaryUpdateRequest request){if(request==null)throw new IllegalArgumentException("Salary request body is required");return producer.requestBody("direct:salary-update",new ApiModels.SalaryChangeCommand(id,request),ApiModels.EmployeeDto.class);}
    @GET @Path("/summary") public ApiModels.SummaryDto summary(){return producer.requestBody("direct:salary-summary",null,ApiModels.SummaryDto.class);}
    @GET @Path("/metadata") public ApiModels.MetadataDto metadata(){return producer.requestBody("direct:employee-metadata",null,ApiModels.MetadataDto.class);}
    @GET @Path("/compensation-requests") public List<ApiModels.CompensationRequestDto> compensation(@QueryParam("status") String status){return producer.requestBody("direct:compensation-list",status,List.class);}
    @POST @Path("/compensation-requests") @Consumes(MediaType.APPLICATION_JSON) public ApiModels.CompensationRequestDto createCompensation(ApiModels.CompensationRequestInput input){return producer.requestBody("direct:compensation-create",input,ApiModels.CompensationRequestDto.class);}
    @GET @Path("/employees/export") public List<ApiModels.EmployeeDto> exportEmployees(){return producer.requestBody("direct:employees-export",null,List.class);}
    @POST @Path("/compensation-requests/{id}/decision") @Consumes(MediaType.APPLICATION_JSON) public ApiModels.CompensationRequestDto decideCompensation(@PathParam("id") UUID id,ApiModels.DecisionInput input){return producer.requestBody("direct:compensation-decision",new ApiModels.CompensationDecisionCommand(id,input),ApiModels.CompensationRequestDto.class);}
    @GET @Path("/quality-issues") public List<ApiModels.QualityIssueDto> qualityIssues(){return producer.requestBody("direct:issues-list",null,List.class);}
    @PUT @Path("/quality-issues/{id}") @Consumes(MediaType.APPLICATION_JSON) public ApiModels.QualityIssueDto updateQualityIssue(@PathParam("id") String id,ApiModels.QualityIssueUpdate input){if(input==null)throw new IllegalArgumentException("Issue update is required");return producer.requestBody("direct:issue-update",new Object[]{id,input.status()},ApiModels.QualityIssueDto.class);}
}
