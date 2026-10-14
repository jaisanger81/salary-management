CREATE TABLE employee (
    id UUID PRIMARY KEY,
    employee_number VARCHAR(16) NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    department VARCHAR(80) NOT NULL,
    country_code CHAR(2) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    annual_salary NUMERIC(14, 2) NOT NULL CHECK (annual_salary >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT employee_currency_code_format CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT employee_country_code_format CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE TABLE salary_change (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id),
    previous_salary NUMERIC(14, 2) NOT NULL CHECK (previous_salary >= 0),
    new_salary NUMERIC(14, 2) NOT NULL CHECK (new_salary >= 0),
    previous_currency CHAR(3) NOT NULL,
    new_currency CHAR(3) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(120) NOT NULL
);

CREATE INDEX employee_country_department_idx ON employee(country_code, department);
CREATE INDEX employee_name_idx ON employee(last_name, first_name);
CREATE INDEX salary_change_employee_time_idx ON salary_change(employee_id, changed_at DESC);
