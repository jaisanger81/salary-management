-- Deterministic fictional dataset. Safe to re-run: employee_number conflicts are skipped.
-- Keep this data synthetic; never seed real employee compensation into the demo.
INSERT INTO employee (
    id, employee_number, first_name, last_name, email, department,
    country_code, currency_code, annual_salary
)
SELECT
    md5('salary-demo-employee-' || n)::uuid,
    'EMP' || lpad(n::text, 5, '0'),
    (ARRAY['Aarav','Maya','Noah','Sofia','Liam','Aisha','Ethan','Mei','Olivia','Arjun'])[((n - 1) % 10) + 1],
    (ARRAY['Sharma','Patel','Johnson','Garcia','Kim','Brown','Singh','Chen','Miller','Khan'])[((n * 7 - 1) % 10) + 1],
    'employee' || lpad(n::text, 5, '0') || '@example.test',
    (ARRAY['Engineering','People','Finance','Operations','Product','Sales'])[((n - 1) % 6) + 1],
    (ARRAY['US','GB','IN','DE','CA','AU'])[((n - 1) % 6) + 1],
    (ARRAY['USD','GBP','INR','EUR','CAD','AUD'])[((n - 1) % 6) + 1],
    (45000 + ((n * 7919) % 125000))::numeric(14, 2)
FROM generate_series(1, 10000) AS n
ON CONFLICT (employee_number) DO NOTHING;
