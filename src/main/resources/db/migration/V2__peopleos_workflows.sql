ALTER TABLE employee
    ADD COLUMN job_title VARCHAR(120) NOT NULL DEFAULT 'Employee',
    ADD COLUMN manager_name VARCHAR(120) NOT NULL DEFAULT '',
    ADD COLUMN location VARCHAR(120) NOT NULL DEFAULT '',
    ADD COLUMN employment_status VARCHAR(20) NOT NULL DEFAULT 'Active'
        CHECK (employment_status IN ('Active', 'On leave', 'Inactive'));

UPDATE employee SET location = CASE country_code
    WHEN 'IN' THEN 'India' WHEN 'US' THEN 'United States' WHEN 'GB' THEN 'United Kingdom'
    WHEN 'SG' THEN 'Singapore' WHEN 'DE' THEN 'Germany' WHEN 'CA' THEN 'Canada'
    WHEN 'AU' THEN 'Australia' ELSE country_code END;

ALTER TABLE salary_change ADD COLUMN reason VARCHAR(500) NOT NULL DEFAULT 'Salary update';

CREATE TABLE compensation_request (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id),
    request_type VARCHAR(60) NOT NULL,
    current_salary NUMERIC(14, 2) NOT NULL CHECK (current_salary >= 0),
    proposed_salary NUMERIC(14, 2) NOT NULL CHECK (proposed_salary > 0),
    currency_code CHAR(3) NOT NULL,
    effective_date DATE NOT NULL,
    justification VARCHAR(2000) NOT NULL,
    requester VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Approved', 'Rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMPTZ,
    decided_by VARCHAR(120),
    CONSTRAINT compensation_request_currency_format CHECK (currency_code ~ '^[A-Z]{3}$')
);
CREATE INDEX compensation_request_status_idx ON compensation_request(status, created_at DESC);

CREATE TABLE data_quality_issue (
    id VARCHAR(20) PRIMARY KEY,
    category VARCHAR(80) NOT NULL,
    issue_description VARCHAR(500) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('High', 'Medium', 'Low')),
    owner VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Open' CHECK (status IN ('Open', 'In review', 'Resolved')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
