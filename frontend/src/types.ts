export interface Employee {
  id: string; employeeNumber: string; firstName: string; lastName: string; email: string
  department: string; countryCode: string; currencyCode: string; annualSalary: number; updatedAt: string
  jobTitle: string; managerName: string; location: string; employmentStatus: 'Active' | 'On leave' | 'Inactive'
}
export interface EmployeePage { items: Employee[]; total: number; page: number; size: number }
export interface CurrencyTotal { currencyCode: string; employeeCount: number; annualSalaryTotal: number }
export interface DepartmentTotal { department: string; employeeCount: number; annualSalaryAverage: number }
export interface Summary {
  headcount: number; salariesByCurrency: CurrencyTotal[]; activeCount: number; leaveCount: number
  inactiveCount: number; departments: DepartmentTotal[]; salaryChangeCount: number
}
export interface SalaryChange {
  id: string; previousSalary: number; newSalary: number; previousCurrency: string; newCurrency: string
  changedAt: string; changedBy: string; reason: string
}
export interface Metadata { countries: string[]; departments: string[]; locations: string[] }
export interface CompensationRequest {
  id: string; employeeId: string; employeeNumber: string; firstName: string; lastName: string
  department: string; requestType: string; currentSalary: number; proposedSalary: number; currencyCode: string
  effectiveDate: string; justification: string; requester: string; status: 'Pending' | 'Approved' | 'Rejected'
  createdAt: string; decidedAt: string | null; decidedBy: string | null
}
export interface QualityIssue {
  id: string; category: string; issueDescription: string; severity: 'High' | 'Medium' | 'Low'
  owner: string; status: 'Open' | 'In review' | 'Resolved'; updatedAt: string
}
export interface EmployeeInput {
  firstName: string; lastName: string; email: string; department: string; countryCode: string
  currencyCode: string; annualSalary: number; jobTitle: string; managerName: string
  location: string; employmentStatus: Employee['employmentStatus']
}
