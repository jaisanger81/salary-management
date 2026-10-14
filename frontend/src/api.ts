import type { CompensationRequest, Employee, EmployeeInput, EmployeePage, Metadata, QualityIssue, SalaryChange, Summary } from './types'

const API_BASE = import.meta.env.VITE_API_URL ?? ''
const API_PREFIX = API_BASE.endsWith('/api/v1') ? API_BASE : `${API_BASE}/api/v1`
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_PREFIX}${path}`, { ...init, headers: { 'Content-Type': 'application/json', ...init?.headers } })
  if (!response.ok) { let message=`Request failed (${response.status})`;try{const body=await response.json() as {error?:string};if(body.error)message=body.error}catch{}throw new Error(message) }
  return response.json() as Promise<T>
}
export interface EmployeeFilters { query: string; country: string; department: string; location?: string; status?: string; page: number; size: number }
export function getEmployees(f: EmployeeFilters): Promise<EmployeePage> {
  const p=new URLSearchParams({page:String(f.page),size:String(f.size)});if(f.query.trim())p.set('query',f.query.trim());if(f.country)p.set('country',f.country);if(f.department)p.set('department',f.department);if(f.location)p.set('location',f.location);if(f.status)p.set('status',f.status);return request(`/employees?${p}`)
}
export const getSummary=():Promise<Summary>=>request('/summary')
export const getMetadata=():Promise<Metadata>=>request('/metadata')
export const getEmployee=(id:string):Promise<Employee>=>request(`/employees/${id}`)
export const getSalaryHistory=(id:string):Promise<SalaryChange[]>=>request(`/employees/${id}/salary-history`)
export const updateSalary=(id:string,annualSalary:number,currencyCode:string,reason='Salary update'):Promise<Employee>=>request(`/employees/${id}/salary`,{method:'PUT',body:JSON.stringify({annualSalary,currencyCode,reason})})
export const saveEmployee=(input:EmployeeInput,id?:string):Promise<Employee>=>request(id?`/employees/${id}`:'/employees',{method:id?'PUT':'POST',body:JSON.stringify(input)})
export const getCompensation=(status=''):Promise<CompensationRequest[]>=>request(`/compensation-requests${status?`?status=${encodeURIComponent(status)}`:''}`)
export const createCompensation=(body:{employeeId:string;requestType:string;proposedSalary:number;effectiveDate:string;justification:string;requester:string}):Promise<CompensationRequest>=>request('/compensation-requests',{method:'POST',body:JSON.stringify(body)})
export const decideCompensation=(id:string,approved:boolean):Promise<CompensationRequest>=>request(`/compensation-requests/${id}/decision`,{method:'POST',body:JSON.stringify({approved,decidedBy:'Priya Sharma'})})
export const getQualityIssues=():Promise<QualityIssue[]>=>request('/quality-issues')
export const updateQualityIssue=(id:string,status:QualityIssue['status']):Promise<QualityIssue>=>request(`/quality-issues/${encodeURIComponent(id)}`,{method:'PUT',body:JSON.stringify({status})})
export function getAllEmployees():Promise<Employee[]> {
  return request('/employees/export')
}
