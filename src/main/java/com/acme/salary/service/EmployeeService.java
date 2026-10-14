package com.acme.salary.service;

import com.acme.salary.api.ApiModels;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class EmployeeService {
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");
    @Inject DataSource dataSource;

    public ApiModels.EmployeePage search(ApiModels.EmployeeQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        if (query.query() != null && !query.query().isBlank()) {
            where.append(" AND (first_name ILIKE ? OR last_name ILIKE ? OR email ILIKE ? OR employee_number ILIKE ? OR job_title ILIKE ? OR location ILIKE ?)");
            String term = "%" + query.query().trim() + "%";
            for (int i = 0; i < 6; i++) parameters.add(term);
        }
        addFilter(where, parameters, "country_code", query.country());
        addFilter(where, parameters, "department", query.department());
        addFilter(where, parameters, "location", query.location());
        addFilter(where, parameters, "employment_status", query.status());
        try (Connection c = dataSource.getConnection()) {
            long total;
            try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM employee" + where)) {
                bind(ps, parameters);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); total = rs.getLong(1); }
            }
            List<ApiModels.EmployeeDto> items = new ArrayList<>();
            String sql = "SELECT * FROM employee" + where + " ORDER BY last_name, first_name, employee_number LIMIT ? OFFSET ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, parameters); ps.setInt(parameters.size()+1, query.size());
                ps.setLong(parameters.size()+2, (long)query.page()*query.size());
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) items.add(employeeFrom(rs)); }
            }
            return new ApiModels.EmployeePage(items,total,query.page(),query.size());
        } catch (SQLException e) { throw new IllegalStateException("Unable to search employees",e); }
    }

    public ApiModels.EmployeeDto getEmployee(UUID id) {
        try (Connection c=dataSource.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT * FROM employee WHERE id=?")) {
            ps.setObject(1,id); try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new EmployeeNotFoundException(id); return employeeFrom(rs); }
        } catch(SQLException e) { throw new IllegalStateException("Unable to load employee",e); }
    }

    public ApiModels.EmployeeDto saveEmployee(UUID id, ApiModels.EmployeeWriteRequest r) {
        validateEmployee(r);
        try(Connection c=dataSource.getConnection()) {
            try(PreparedStatement emailCheck=c.prepareStatement("SELECT 1 FROM employee WHERE lower(email)=lower(?) AND (? IS NULL OR id<>?) LIMIT 1")) {
                emailCheck.setString(1,r.email().trim());
                emailCheck.setObject(2,id);
                emailCheck.setObject(3,id);
                try(ResultSet duplicate=emailCheck.executeQuery()) { if(duplicate.next()) throw new IllegalArgumentException("That email already exists"); }
            }
            if(id==null) {
                UUID newId=UUID.randomUUID();
                String number="AC"+newId.toString().replace("-","").substring(0,8).toUpperCase(Locale.ROOT);
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO employee(id,employee_number,first_name,last_name,email,department,country_code,currency_code,annual_salary,job_title,manager_name,location,employment_status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    bindEmployee(ps,newId,number,r); ps.executeUpdate();
                }
                return getEmployee(newId);
            }
            ApiModels.EmployeeDto old=getEmployee(id);
            try(PreparedStatement ps=c.prepareStatement("UPDATE employee SET first_name=?,last_name=?,email=?,department=?,country_code=?,currency_code=?,annual_salary=?,job_title=?,manager_name=?,location=?,employment_status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
                ps.setString(1,r.firstName().trim());ps.setString(2,r.lastName().trim());ps.setString(3,r.email().trim());ps.setString(4,r.department().trim());ps.setString(5,r.countryCode().trim().toUpperCase(Locale.ROOT));ps.setString(6,r.currencyCode().trim().toUpperCase(Locale.ROOT));ps.setBigDecimal(7,r.annualSalary().setScale(2,RoundingMode.UNNECESSARY));ps.setString(8,r.jobTitle().trim());ps.setString(9,blank(r.managerName()));ps.setString(10,r.location().trim());ps.setString(11,r.employmentStatus());ps.setObject(12,id);ps.executeUpdate();
            }
            if(old.annualSalary().compareTo(r.annualSalary())!=0 || !old.currencyCode().equalsIgnoreCase(r.currencyCode())) insertSalaryChange(c,id,old.annualSalary(),r.annualSalary(),old.currencyCode(),r.currencyCode(),"Employee profile edit","demo-hr");
            return getEmployee(id);
        } catch(SQLException e) { throw new IllegalStateException("Unable to save employee",e); }
    }

    public List<ApiModels.SalaryChangeDto> getSalaryHistory(UUID id) {
        getEmployee(id); List<ApiModels.SalaryChangeDto> out=new ArrayList<>();
        try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("SELECT * FROM salary_change WHERE employee_id=? ORDER BY changed_at DESC,id DESC")){ps.setObject(1,id);try(ResultSet rs=ps.executeQuery()){while(rs.next())out.add(new ApiModels.SalaryChangeDto(rs.getObject("id",UUID.class),rs.getBigDecimal("previous_salary"),rs.getBigDecimal("new_salary"),rs.getString("previous_currency").trim(),rs.getString("new_currency").trim(),offset(rs.getTimestamp("changed_at")),rs.getString("changed_by"),rs.getString("reason")));}return out;}catch(SQLException e){throw new IllegalStateException("Unable to load salary history",e);}
    }

    public ApiModels.EmployeeDto updateSalary(UUID id,ApiModels.SalaryUpdateRequest r){
        if(r==null||r.annualSalary()==null)throw new IllegalArgumentException("annualSalary is required");
        BigDecimal amount=validSalary(r.annualSalary());String currency=currency(r.currencyCode());
        try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{ApiModels.EmployeeDto old;try(PreparedStatement ps=c.prepareStatement("SELECT * FROM employee WHERE id=? FOR UPDATE")){ps.setObject(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new EmployeeNotFoundException(id);old=employeeFrom(rs);}}
            if(old.annualSalary().compareTo(amount)!=0||!old.currencyCode().equals(currency)){try(PreparedStatement ps=c.prepareStatement("UPDATE employee SET annual_salary=?,currency_code=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){ps.setBigDecimal(1,amount);ps.setString(2,currency);ps.setObject(3,id);ps.executeUpdate();}insertSalaryChange(c,id,old.annualSalary(),amount,old.currencyCode(),currency,r.reason()==null?"Salary update":r.reason(),"demo-hr");}
            c.commit();}catch(RuntimeException|SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(SQLException e){throw new IllegalStateException("Unable to update salary",e);}return getEmployee(id);
    }

    public ApiModels.SummaryDto getSummary(){
        List<ApiModels.CurrencyTotal> totals=new ArrayList<>();List<ApiModels.DepartmentTotal> departments=new ArrayList<>();long head=0,active=0,leave=0,inactive=0,changes=0;
        try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("SELECT currency_code,count(*) employee_count,sum(annual_salary) salary_total FROM employee GROUP BY currency_code ORDER BY currency_code")){try(ResultSet rs=ps.executeQuery()){while(rs.next()){long n=rs.getLong("employee_count");head+=n;totals.add(new ApiModels.CurrencyTotal(rs.getString("currency_code").trim(),n,rs.getBigDecimal("salary_total")));}}
            try(PreparedStatement p=c.prepareStatement("SELECT employment_status,count(*) FROM employee GROUP BY employment_status");ResultSet r=p.executeQuery()){while(r.next()){switch(r.getString(1)){case "Active"->active=r.getLong(2);case "On leave"->leave=r.getLong(2);case "Inactive"->inactive=r.getLong(2);}}}
            try(PreparedStatement p=c.prepareStatement("SELECT department,count(*) n,avg(annual_salary) avg FROM employee WHERE currency_code='INR' GROUP BY department ORDER BY department");ResultSet r=p.executeQuery()){while(r.next())departments.add(new ApiModels.DepartmentTotal(r.getString(1),r.getLong(2),r.getBigDecimal(3)));}
            try(PreparedStatement p=c.prepareStatement("SELECT count(*) FROM salary_change");ResultSet r=p.executeQuery()){r.next();changes=r.getLong(1);}
            return new ApiModels.SummaryDto(head,totals,active,leave,inactive,departments,changes);
        }catch(SQLException e){throw new IllegalStateException("Unable to calculate summary",e);}
    }

    public ApiModels.MetadataDto getMetadata(){return new ApiModels.MetadataDto(distinct("country_code"),distinct("department"),distinct("location"));}
    private List<String> distinct(String col){if(!List.of("country_code","department","location").contains(col))throw new IllegalArgumentException("Unsupported filter");List<String> out=new ArrayList<>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT DISTINCT "+col+" FROM employee ORDER BY "+col);ResultSet r=p.executeQuery()){while(r.next())out.add(r.getString(1).trim());return out;}catch(SQLException e){throw new IllegalStateException("Unable to load metadata",e);}}

    public List<ApiModels.CompensationRequestDto> listCompensation(String status){List<ApiModels.CompensationRequestDto> out=new ArrayList<>();String sql="SELECT r.*,e.employee_number,e.first_name,e.last_name,e.department FROM compensation_request r JOIN employee e ON e.id=r.employee_id"+(status==null||status.isBlank()?"":" WHERE r.status=?")+" ORDER BY r.created_at DESC";try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql)){if(status!=null&&!status.isBlank())p.setString(1,status);try(ResultSet r=p.executeQuery()){while(r.next())out.add(compensationFrom(r));}return out;}catch(SQLException e){throw new IllegalStateException("Unable to load compensation requests",e);}}
    public ApiModels.CompensationRequestDto createCompensation(ApiModels.CompensationRequestInput in){if(in==null||in.employeeId()==null||in.requestType()==null||in.requestType().isBlank()||in.effectiveDate()==null||in.justification()==null||in.justification().isBlank())throw new IllegalArgumentException("Employee, request type, effective date and justification are required");BigDecimal proposed=validSalary(in.proposedSalary());ApiModels.EmployeeDto e=getEmployee(in.employeeId());try(Connection c=dataSource.getConnection()){try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM compensation_request WHERE employee_id=? AND status='Pending'")){p.setObject(1,e.id());try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("This employee already has a pending salary request");}}UUID id=UUID.randomUUID();try(PreparedStatement p=c.prepareStatement("INSERT INTO compensation_request(id,employee_id,request_type,current_salary,proposed_salary,currency_code,effective_date,justification,requester) VALUES(?,?,?,?,?,?,?,?,?)")){p.setObject(1,id);p.setObject(2,e.id());p.setString(3,in.requestType().trim());p.setBigDecimal(4,e.annualSalary());p.setBigDecimal(5,proposed);p.setString(6,e.currencyCode());p.setDate(7,Date.valueOf(in.effectiveDate()));p.setString(8,in.justification().trim());p.setString(9,in.requester()==null||in.requester().isBlank()?"Priya Sharma":in.requester().trim());p.executeUpdate();}return listCompensation(null).stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();}catch(SQLException ex){throw new IllegalStateException("Unable to create compensation request",ex);}}
    public ApiModels.CompensationRequestDto decideCompensation(UUID id,ApiModels.DecisionInput decision){if(decision==null)throw new IllegalArgumentException("Decision is required");try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{ApiModels.CompensationRequestDto request;try(PreparedStatement p=c.prepareStatement("SELECT r.*,e.employee_number,e.first_name,e.last_name,e.department FROM compensation_request r JOIN employee e ON e.id=r.employee_id WHERE r.id=? FOR UPDATE OF r")){p.setObject(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Compensation request not found");request=compensationFrom(r);}}if(!"Pending".equals(request.status()))throw new IllegalArgumentException("Compensation request is no longer pending");String status=decision.approved()?"Approved":"Rejected";try(PreparedStatement p=c.prepareStatement("UPDATE compensation_request SET status=?,decided_at=CURRENT_TIMESTAMP,decided_by=? WHERE id=?")){p.setString(1,status);p.setString(2,decision.decidedBy()==null||decision.decidedBy().isBlank()?"demo-hr":decision.decidedBy());p.setObject(3,id);p.executeUpdate();}if(decision.approved()){try(PreparedStatement p=c.prepareStatement("UPDATE employee SET annual_salary=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){p.setBigDecimal(1,request.proposedSalary());p.setObject(2,request.employeeId());p.executeUpdate();}insertSalaryChange(c,request.employeeId(),request.currentSalary(),request.proposedSalary(),request.currencyCode(),request.currencyCode(),request.justification(),decision.decidedBy()==null?"demo-hr":decision.decidedBy());}c.commit();}catch(RuntimeException|SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(SQLException e){throw new IllegalStateException("Unable to decide compensation request",e);}return listCompensation(null).stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();}

    public List<ApiModels.EmployeeDto> exportEmployees(){
        List<ApiModels.EmployeeDto> out=new ArrayList<>();
        try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM employee ORDER BY employee_number");ResultSet r=p.executeQuery()){
            while(r.next())out.add(employeeFrom(r));return out;
        }catch(SQLException e){throw new IllegalStateException("Unable to export employees",e);}
    }

    public List<ApiModels.QualityIssueDto> listIssues(){List<ApiModels.QualityIssueDto> out=new ArrayList<>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM data_quality_issue ORDER BY id");ResultSet r=p.executeQuery()){while(r.next())out.add(new ApiModels.QualityIssueDto(r.getString("id"),r.getString("category"),r.getString("issue_description"),r.getString("severity"),r.getString("owner"),r.getString("status"),offset(r.getTimestamp("updated_at"))));return out;}catch(SQLException e){throw new IllegalStateException("Unable to load quality issues",e);}}
    public ApiModels.QualityIssueDto updateIssue(String id,String status){if(!List.of("Open","In review","Resolved").contains(status))throw new IllegalArgumentException("Unsupported issue status");try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE data_quality_issue SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){p.setString(1,status);p.setString(2,id);if(p.executeUpdate()==0)throw new IllegalArgumentException("Quality issue not found");return listIssues().stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();}catch(SQLException e){throw new IllegalStateException("Unable to update quality issue",e);}}

    private ApiModels.CompensationRequestDto compensationFrom(ResultSet r)throws SQLException{return new ApiModels.CompensationRequestDto(r.getObject("id",UUID.class),r.getObject("employee_id",UUID.class),r.getString("employee_number"),r.getString("first_name"),r.getString("last_name"),r.getString("department"),r.getString("request_type"),r.getBigDecimal("current_salary"),r.getBigDecimal("proposed_salary"),r.getString("currency_code").trim(),r.getDate("effective_date").toLocalDate(),r.getString("justification"),r.getString("requester"),r.getString("status"),offset(r.getTimestamp("created_at")),r.getTimestamp("decided_at")==null?null:offset(r.getTimestamp("decided_at")),r.getString("decided_by"));}
    private ApiModels.EmployeeDto employeeFrom(ResultSet r)throws SQLException{return new ApiModels.EmployeeDto(r.getObject("id",UUID.class),r.getString("employee_number"),r.getString("first_name"),r.getString("last_name"),r.getString("email"),r.getString("department"),r.getString("country_code").trim(),r.getString("currency_code").trim(),r.getBigDecimal("annual_salary"),offset(r.getTimestamp("updated_at")),r.getString("job_title"),r.getString("manager_name"),r.getString("location"),r.getString("employment_status"));}
    private void validateEmployee(ApiModels.EmployeeWriteRequest r){if(r==null||blank(r.firstName()).isBlank()||blank(r.lastName()).isBlank()||blank(r.email()).isBlank()||blank(r.department()).isBlank()||blank(r.jobTitle()).isBlank()||blank(r.location()).isBlank())throw new IllegalArgumentException("Name, email, department, job title and location are required");if(!r.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException("Valid email is required");if(!List.of("Active","On leave","Inactive").contains(r.employmentStatus()))throw new IllegalArgumentException("Unsupported employment status");validSalary(r.annualSalary());currency(r.currencyCode());if(r.countryCode()==null||!COUNTRY.matcher(r.countryCode().trim().toUpperCase(Locale.ROOT)).matches())throw new IllegalArgumentException("countryCode must be a two-letter code");}
    private void bindEmployee(PreparedStatement p,UUID id,String number,ApiModels.EmployeeWriteRequest r)throws SQLException{p.setObject(1,id);p.setString(2,number);p.setString(3,r.firstName().trim());p.setString(4,r.lastName().trim());p.setString(5,r.email().trim());p.setString(6,r.department().trim());p.setString(7,r.countryCode().trim().toUpperCase(Locale.ROOT));p.setString(8,currency(r.currencyCode()));p.setBigDecimal(9,validSalary(r.annualSalary()));p.setString(10,r.jobTitle().trim());p.setString(11,blank(r.managerName()));p.setString(12,r.location().trim());p.setString(13,r.employmentStatus());}
    private BigDecimal validSalary(BigDecimal x){if(x==null)throw new IllegalArgumentException("annualSalary is required");if(x.signum()<0)throw new IllegalArgumentException("annualSalary must not be negative");if(x.stripTrailingZeros().scale()>2)throw new IllegalArgumentException("annualSalary must have at most 2 decimal places");if(x.precision()-x.scale()>12)throw new IllegalArgumentException("annualSalary exceeds the supported precision");return x.setScale(2,RoundingMode.UNNECESSARY);}
    private String currency(String x){if(x==null||!CURRENCY.matcher(x.trim().toUpperCase(Locale.ROOT)).matches())throw new IllegalArgumentException("currencyCode must be a three-letter code");return x.trim().toUpperCase(Locale.ROOT);}
    private void insertSalaryChange(Connection c,UUID id,BigDecimal old,BigDecimal next,String oldCurrency,String nextCurrency,String reason,String actor)throws SQLException{try(PreparedStatement p=c.prepareStatement("INSERT INTO salary_change(id,employee_id,previous_salary,new_salary,previous_currency,new_currency,changed_by,reason) VALUES(?,?,?,?,?,?,?,?)")){p.setObject(1,UUID.randomUUID());p.setObject(2,id);p.setBigDecimal(3,old);p.setBigDecimal(4,next);p.setString(5,oldCurrency.trim().toUpperCase(Locale.ROOT));p.setString(6,nextCurrency.trim().toUpperCase(Locale.ROOT));p.setString(7,actor);p.setString(8,reason==null||reason.isBlank()?"Salary update":reason.trim());p.executeUpdate();}}
    private void addFilter(StringBuilder where,List<Object> values,String column,String value){if(value!=null&&!value.isBlank()){where.append(" AND ").append(column).append("=?");values.add(value.trim());}}
    private void bind(PreparedStatement p,List<Object> values)throws SQLException{for(int i=0;i<values.size();i++)p.setObject(i+1,values.get(i));}
    private OffsetDateTime offset(Timestamp t){return t.toInstant().atOffset(ZoneOffset.UTC);}
    private String blank(String s){return s==null?"":s;}
}
