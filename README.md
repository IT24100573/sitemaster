# SiteMaster – Construction Management System

SiteMaster is a role-based web application for a construction company. It covers the
whole life of a building project: planning, the site team's daily work, materials and
purchasing, attendance and payroll, expenses, client billing, reports and dashboards.

| Layer     | Technology |
|-----------|------------|
| Backend   | Java 21, Spring Boot 3, Spring Security (session cookie + CSRF), Spring Data JPA, Bean Validation |
| Database  | PostgreSQL 16, Flyway migrations (V1–V7) |
| Files     | Local disk storage, type checked with Apache Tika |
| Documents | OpenPDF (payslips, invoices, PDF reports), Apache POI (Excel reports) |
| Frontend  | React 18, TypeScript, Vite, Tailwind CSS, React Query, React Hook Form + Zod, Recharts, Leaflet |
| Tests     | JUnit 5, AssertJ, Mockito |

---

## 1. Quick start

### Prerequisites
- JDK 21 and Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16 running on `localhost:5432`

### Database
```bash
psql -U postgres -c "CREATE DATABASE sitemaster;"
```
The defaults expect user `postgres` with password `1234`. Flyway creates every table on
first start.

### Backend
```bash
cd backend
mvn spring-boot:run          # http://localhost:8080
```

### Frontend
```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173 (calls /api on port 8080 through the Vite proxy)
```

Open http://localhost:5173 and sign in with one of the demo accounts below.

---

## 2. Demo accounts and demo data

All accounts use the password **`Password@123`**.

| Role | Email | Name |
|------|-------|------|
| Admin | admin@sitemaster.lk | Anjali Wickramasinghe |
| Project Manager | pm@sitemaster.lk | Nimal Perera |
| Site Engineer | engineer@sitemaster.lk | Kasun Silva |
| Worker (Mason) | worker@sitemaster.lk | Saman Kumara |
| HR Manager | hr@sitemaster.lk | Dilani Fernando |
| Accountant | accountant@sitemaster.lk | Ruwan Jayasinghe |
| Supplier | supplier@sitemaster.lk | Chaminda Rajapaksha (Lanka Building Supplies) |
| Worker (Carpenter) | pradeep@sitemaster.lk | Pradeep Bandara |
| Worker (Electrician) | ajith@sitemaster.lk | Ajith Gunasekara |
| Worker (Labourer) | roshan@sitemaster.lk | Roshan Dissanayake |
| Worker (Steel Fixer, contract) | ishara@sitemaster.lk | Ishara Herath |
| Supplier | sales@ceylonsteel.lk | Rohan Wijesuriya (Ceylon Steel Traders) |

On a fresh database the backend builds a complete demo company. It does this by running
the real workflows through the services, signed in as the right person at each step, so
every rule, notification and audit entry happens just as it would through the UI. All
dates are relative to the day you start the application, so the data always looks current.

| Area | What you will find |
|------|--------------------|
| Projects | **Kandy Road Apartments** (active, 3 phases, tasks, milestones), **Galle Face Office Fit-out** (delayed), **Negombo Beach Villa** (completed, shown in the public gallery), **Kurunegala Warehouse** (planned). All four appear on the public sites map. |
| Purchasing | Cement: requested → approved → ordered → invoiced → paid → delivered (stock on site). Steel: ordered and dispatched by the second supplier. Sand: approved, waiting for an order. Blocks: pending approval. |
| Site work | Three daily reports (two approved, one waiting for the PM), two open issues/risks, equipment assigned and one crane under maintenance. |
| Workforce | Attendance for last month and this month, overtime (one item pending HR), a pending leave request and an approved one, contracts (one expiring soon), training records (one expired). Two workers have checked in today and are waiting for verification. |
| Payroll | Last month's payroll run sent by HR, calculated and paid by the Accountant, with payslips for everyone. |
| Finance | Petty cash fund for the Kandy site, three expenses (paid by petty cash, paid by bank, pending), client invoices (partly paid and overdue, issued, paid, draft). |
| Suppliers | One approved application and one pending application for the Admin. |

Seeding rules:
- Demo accounts are created when the `users` table is empty.
- The demo company is created when no project exists yet. If you used earlier phases and
  already have projects, drop and recreate the database to get the full demo:
  ```bash
  psql -U postgres -c "DROP DATABASE sitemaster;" -c "CREATE DATABASE sitemaster;"
  ```
- Set `SEED_ENABLED=false` to start with an empty system.
- If creating the demo company fails for any reason, the error is logged and nothing
  partial is kept; the application still starts with the accounts.

---

## 3. Running the tests

```bash
cd backend
mvn test
```

The unit tests need no database and finish in a few seconds.

| Test class | What it checks |
|------------|----------------|
| `SalaryStrategyTest` | Daily wage, permanent and contract pay rules and the strategy resolver |
| `PayslipDecoratorTest` | Each decorator adds one line; running totals; EPF on base pay only |
| `PayslipCalculatorTest` | Strategy + Decorator together using System Settings rates |
| `MaterialRequestTest` | Budget check, own-request block, rejection reason, order → paid → delivered |
| `LeaveAndExpenseRequestTest` | Leave balance deduction, receipt threshold, expense payment rules |
| `AttendanceTest` | Hours worked, 16-hour limit, corrections clear verification, overtime limits |
| `PettyCashFundTest` | Float limit, insufficient balance, cash count adjustment |
| `InvoiceWorkflowTest` | Client invoice tax, partial payments and overdue status; purchase order and supplier invoice workflow |
| `ValidatorsTest` | Strong password, Sri Lankan NIC and phone validators |
| `ReportExportTest` | Exporter factory; CSV quoting and formula injection guard; real PDF and XLSX output |
| `WorkCalendarTest` | Sundays, holidays, working days per month, settings singleton |
| `DashboardAndBudgetTest` | Dashboard factory and phase budget check across cost sources |

---

## 4. Roles

| Role | Main work |
|------|-----------|
| **Admin** | Users, roles and permissions, System Settings, clients, suppliers and applications, projects, materials catalogue, equipment, audit log, all reports |
| **Project Manager** | Own projects: team, phases, milestones, tasks, issues, documents; approves material requests, daily reports and expenses; project budget view |
| **Site Engineer** | Daily reports, material requests, delivery confirmation, attendance and overtime for the site team, expenses, issues |
| **Worker** | Own tasks, check-in/check-out, own attendance, leave and payslips |
| **HR Manager** | Employees and documents, shifts, attendance for any site, overtime approval, leave, contracts, training, monthly attendance summary for payroll |
| **Accountant** | Purchase orders, supplier invoices and payments, payroll calculation and payment, expenses, petty cash, client invoices, project finance, reports |
| **Supplier** | Company profile, purchase orders (accept, decline, dispatch), invoice upload, payment status |

The public site (no sign-in) shows company information, completed projects, a map of
sites and the supplier application form.

---

## 5. OOP concepts and design patterns

### OOP concepts
- **Encapsulation** – entities protect their own state and rules, e.g. `PettyCashFund`
  only changes its balance through `topUp`, `spend` and `adjustTo`; `Attendance`
  computes hours itself.
- **Inheritance** – every entity extends `BaseEntity` (id, audit dates, version);
  `MaterialRequest`, `ExpenseRequest` and `LeaveRequest` extend `BaseRequest`; all business
  exceptions extend `ApplicationException`.
- **Polymorphism** – `BaseRequest.approve()` is overridden by each request type (budget
  check, leave balance); `ApplicationException.getStatus()` gives each error its HTTP code;
  report exporters and dashboards are used through their interfaces.
- **Abstraction** – services are interfaces with `*Impl` classes; `PhaseCostSource`,
  `ReportGenerator`, `ReportExporter`, `RoleDashboard`, `SalaryCalculationStrategy`.

### Design patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `settings/SystemSettingsManager` | One shared, immutable settings snapshot (company details, working hours, holidays, rates), reloaded when the Admin saves |
| **Observer** | `workflow/WorkflowEventPublisher` → `AuditLogObserver`, `NotificationObserver` | Services publish one event; auditing and notifications react without the services knowing about them |
| **Strategy** | `payroll/strategy/*` | Base pay differs by employment type: daily wage, permanent salary, contract share |
| **Decorator** | `payroll/decorator/*` | A payslip is built in layers: base pay → overtime → site allowance → EPF deduction |
| **Factory Method** | `dashboard/DashboardFactory`, `report/export/ReportExporterFactory` | Pick the right dashboard for a role, and the right exporter (PDF, Excel, CSV) for a format |

---

## 6. Main workflows

1. **Material purchase** – Site Engineer requests → PM approves (phase budget checked,
   overrun must be acknowledged) → Accountant creates PO → Supplier accepts, dispatches and
   uploads an invoice → Accountant pays → Site Engineer confirms delivery → site stock goes up.
2. **Daily report** – Site Engineer submits (weather, workers, materials used, photos) →
   PM approves → stock goes down and project progress is recalculated.
3. **Payroll** – Attendance is verified → HR sends the monthly summary (month is locked) →
   Accountant calculates payslips (Strategy + Decorator) → pays → workers download PDF payslips.
4. **Expense** – Site Engineer records (receipt above the threshold) → PM approves →
   Accountant pays from petty cash or by bank.
5. **Client billing** – Accountant drafts an invoice → issues it (PDF) → records receipts;
   overdue invoices are highlighted.

A project can only be archived when no material request, purchase order, expense or
client invoice is still open.

---

## 7. Configuration

All settings have defaults; override them with environment variables.

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5432`, `sitemaster` | Database location |
| `DB_USERNAME`, `DB_PASSWORD` | `postgres`, `1234` | Database login |
| `UPLOAD_DIR` | `~/sitemaster-uploads` | Where uploaded files are stored |
| `FRONTEND_URL` | `http://localhost:5173` | Used in password reset links |
| `SEED_ENABLED` | `true` | Create demo accounts and demo company |
| `SEED_PASSWORD` | `Password@123` | Password for all demo accounts |
| `MAX_FAILED_ATTEMPTS`, `LOCK_DURATION` | `5`, `15m` | Account lock after failed sign-ins |
| `APP_TIME_ZONE` | `Asia/Colombo` | Time zone for dates |
| `VITE_API_TARGET` (frontend) | `http://localhost:8080` | Backend address for the dev proxy |

Business settings (company profile, working hours, holidays, leave days, EPF/ETF rates,
overtime multiplier, site allowance, trades, material categories and units, receipt
threshold) are edited by the Admin under **Settings** and stored in the database.

Password reset links are written to the backend log (no mail server is required).

---

## 8. Project structure

```
sitemaster/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/sitemaster/
│       │   ├── auth, security, user, role, profile      sign-in, sessions, permissions
│       │   ├── settings, audit, notification, workflow  settings singleton, observer
│       │   ├── project, task, document, issue, client   planning and project work
│       │   ├── material, materialrequest, equipment,
│       │   │   dailyreport, purchase, supplier          site operations and purchasing
│       │   ├── employee, attendance, leave, hr, payroll workforce and payroll
│       │   ├── expense, pettycash, clientinvoice,
│       │   │   finance, budget, report, dashboard       finance, reports, dashboards
│       │   ├── common, validation, file, lookup         shared building blocks
│       │   └── seed                                     demo accounts and demo company
│       ├── main/resources/db/migration                  Flyway V1–V7
│       └── test/java/com/sitemaster                     unit tests
└── frontend/
    └── src/
        ├── api          one module per backend area
        ├── components   layout and UI components
        ├── config       roles and navigation
        ├── context      auth and toasts
        ├── hooks        shared hooks
        ├── pages        screens grouped by area
        ├── routes       route guards
        ├── types        TypeScript types
        └── validation   Zod schemas and messages
```

---

## 9. Troubleshooting

| Problem | Fix |
|---------|-----|
| `password authentication failed for user "postgres"` | Set `DB_PASSWORD` to your PostgreSQL password |
| No projects after start-up | The database already had projects; drop and recreate it (section 2) |
| Upload rejected | Images: JPG/PNG up to 2 MB; documents: PDF/JPG/PNG up to 10 MB |
| Signed out unexpectedly | Sessions end after 30 minutes without activity |
| Port 8080 or 5173 in use | Stop the other process or change `server.port` / the Vite port |
