UPDATE employee SET
    department = CASE mod(substring(employee_number from 4)::integer, 8)
        WHEN 0 THEN 'Engineering' WHEN 1 THEN 'Operations' WHEN 2 THEN 'Sales' WHEN 3 THEN 'Finance'
        WHEN 4 THEN 'Human Resources' WHEN 5 THEN 'Marketing' WHEN 6 THEN 'Product' ELSE 'Customer Success' END,
    job_title = CASE mod(substring(employee_number from 4)::integer, 10)
        WHEN 0 THEN 'Software Engineer' WHEN 1 THEN 'Senior Analyst' WHEN 2 THEN 'Product Manager'
        WHEN 3 THEN 'Finance Associate' WHEN 4 THEN 'Operations Lead' WHEN 5 THEN 'HR Partner'
        WHEN 6 THEN 'Sales Manager' WHEN 7 THEN 'QA Engineer' WHEN 8 THEN 'Data Analyst' ELSE 'Engineering Manager' END,
    employment_status = CASE WHEN mod(substring(employee_number from 4)::integer, 417)=0 THEN 'Inactive'
        WHEN mod(substring(employee_number from 4)::integer, 73)=0 THEN 'On leave' ELSE 'Active' END,
    manager_name = CASE WHEN mod(substring(employee_number from 4)::integer, 3)=0 THEN 'Priya Sharma' ELSE '' END,
    location = CASE country_code WHEN 'IN' THEN 'India' WHEN 'US' THEN 'United States'
        WHEN 'GB' THEN 'United Kingdom' WHEN 'SG' THEN 'Singapore' WHEN 'DE' THEN 'Germany'
        WHEN 'CA' THEN 'Canada' WHEN 'AU' THEN 'Australia' ELSE location END;

INSERT INTO data_quality_issue(id,category,issue_description,severity,owner) VALUES
('DQ-001','Employee data','Missing cost center on employee record','High','HR Operations'),
('DQ-002','Compensation','Salary outside configured job-level band','High','Total Rewards'),
('DQ-003','Location','Work location missing for payroll entity','Medium','Regional HR'),
('DQ-004','Employee data','Manager field not populated','Low','HR Operations'),
('DQ-005','Compensation','Currency confirmation required for local salary','Medium','Payroll')
ON CONFLICT(id) DO NOTHING;

INSERT INTO compensation_request(id,employee_id,request_type,current_salary,proposed_salary,currency_code,effective_date,justification,requester,status)
SELECT v.request_id,e.id,v.request_type,e.annual_salary,v.proposed,e.currency_code,v.effective_date,v.justification,v.requester,v.status
FROM (VALUES
('00000000-0000-0000-0000-000000002601'::uuid,'EMP00001','Merit increase',156600.00::numeric,DATE '2026-11-01','Strong performance and expanded scope','Nikhil Rao','Pending'),
('00000000-0000-0000-0000-000000002602'::uuid,'EMP00002','Promotion',205000.00::numeric,DATE '2026-11-01','Promotion to Principal Product Manager','Sarah Miller','Pending'),
('00000000-0000-0000-0000-000000002603'::uuid,'EMP00004','Market adjustment',119000.00::numeric,DATE '2026-11-15','Market benchmark alignment','Wei Chen','Pending'),
('00000000-0000-0000-0000-000000002604'::uuid,'EMP00005','Merit increase',175000.00::numeric,DATE '2026-11-01','Annual performance review','Nikhil Rao','Pending'),
('00000000-0000-0000-0000-000000002605'::uuid,'EMP00006','Correction',92000.00::numeric,DATE '2026-11-01','Compensation data correction','Priya Sharma','Pending'),
('00000000-0000-0000-0000-000000002606'::uuid,'EMP00009','Retention adjustment',133000.00::numeric,DATE '2026-11-01','Critical role retention proposal','Nikhil Rao','Pending'),
('00000000-0000-0000-0000-000000002591'::uuid,'EMP00007','Merit increase',103000.00::numeric,DATE '2026-10-01','Annual review','A. Patel','Approved'),
('00000000-0000-0000-0000-000000002592'::uuid,'EMP00008','Market adjustment',89000.00::numeric,DATE '2026-10-01','Market review','P. Sharma','Rejected')
) AS v(request_id,employee_number,request_type,proposed,effective_date,justification,requester,status)
JOIN employee e ON e.employee_number=v.employee_number
ON CONFLICT(id) DO NOTHING;
