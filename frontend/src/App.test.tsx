import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import App from './App'

const api = vi.hoisted(() => ({
  createCompensation: vi.fn(), decideCompensation: vi.fn(), getAllEmployees: vi.fn(), getCompensation: vi.fn(),
  getEmployees: vi.fn(), getMetadata: vi.fn(), getQualityIssues: vi.fn(), getSalaryHistory: vi.fn(),
  getSummary: vi.fn(), saveEmployee: vi.fn(), updateQualityIssue: vi.fn(),
}))
vi.mock('./api', () => api)
afterEach(cleanup)

const employee = {
  id: 'employee-1', employeeNumber: 'EMP00001', firstName: 'Aarav', lastName: 'Sharma',
  email: 'employee00001@example.test', department: 'Engineering', countryCode: 'IN', currencyCode: 'INR',
  annualSalary: 75000, updatedAt: '2025-01-15T00:00:00Z', jobTitle: 'Software Engineer',
  managerName: 'Priya Sharma', location: 'India', employmentStatus: 'Active',
} as const
const request = {
  id: 'request-1', employeeId: employee.id, employeeNumber: employee.employeeNumber,
  firstName: employee.firstName, lastName: employee.lastName, department: employee.department,
  requestType: 'Merit increase', currentSalary: 75000, proposedSalary: 81000, currencyCode: 'INR',
  effectiveDate: '2026-11-01', justification: 'Strong performance', requester: 'Priya Sharma',
  status: 'Pending', createdAt: '2026-10-14T00:00:00Z', decidedAt: null, decidedBy: null,
} as const

beforeEach(() => {
  vi.clearAllMocks()
  api.getEmployees.mockResolvedValue({ items: [employee], total: 1, page: 0, size: 10 })
  api.getMetadata.mockResolvedValue({ countries: ['IN'], departments: ['Engineering'], locations: ['India'] })
  api.getSummary.mockResolvedValue({ headcount: 1, salariesByCurrency: [{ currencyCode: 'INR', employeeCount: 1, annualSalaryTotal: 75000 }], activeCount: 1, leaveCount: 0, inactiveCount: 0, departments: [{ department: 'Engineering', employeeCount: 1, annualSalaryAverage: 75000 }], salaryChangeCount: 0 })
  api.getCompensation.mockResolvedValue([request])
  api.getQualityIssues.mockResolvedValue([])
  api.getSalaryHistory.mockResolvedValue([])
  api.getAllEmployees.mockResolvedValue([employee])
  api.saveEmployee.mockResolvedValue(employee)
  api.createCompensation.mockResolvedValue(request)
  api.decideCompensation.mockResolvedValue({ ...request, status: 'Approved' })
  api.updateQualityIssue.mockResolvedValue({})
})

describe('PeopleOS HR dashboard', () => {
  it('shows the global dashboard and its navigation and workforce metrics', async () => {
    render(<App />)
    expect(await screen.findByText('Good morning, Priya')).toBeInTheDocument()
    expect(screen.getByText('Global headcount')).toBeInTheDocument()
    expect(screen.getByText(/GLOBAL PEOPLE OPERATIONS/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Employee directory/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Approvals/ })).toHaveTextContent('1')
  })

  it('shows employee filters and opens the employee profile', async () => {
    render(<App />)
    fireEvent.click(screen.getAllByRole('button', { name: /Employee directory/ })[0])
    expect(await screen.findByText('Employee directory', { selector: 'h1' })).toBeInTheDocument()
    expect(await screen.findByText('Aarav Sharma')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'View Aarav Sharma' }))
    expect(await screen.findByRole('dialog')).toHaveTextContent('Software Engineer')
    expect(screen.getAllByText('employee00001@example.test')).toHaveLength(2)
  })

  it('supports compensation decisions from the approval queue', async () => {
    render(<App />)
    fireEvent.click(screen.getAllByRole('button', { name: /Approvals/ })[0])
    expect(await screen.findByText('Aarav Sharma')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Approve' }))
    await waitFor(() => expect(api.decideCompensation).toHaveBeenCalledWith('request-1', true))
    expect(await screen.findByRole('status')).toHaveTextContent('request-1 approved.')
  })
})
