package com.sitemaster.seed;

import com.sitemaster.attendance.AttendanceService;
import com.sitemaster.attendance.OvertimeRepository;
import com.sitemaster.attendance.dto.AttendanceRequest;
import com.sitemaster.attendance.dto.AttendanceResponse;
import com.sitemaster.client.ClientService;
import com.sitemaster.client.dto.ClientRequest;
import com.sitemaster.clientinvoice.ClientInvoiceService;
import com.sitemaster.clientinvoice.dto.ClientInvoiceRequest;
import com.sitemaster.clientinvoice.dto.InvoiceLineRequest;
import com.sitemaster.clientinvoice.dto.ReceiptRequest;
import com.sitemaster.common.config.AppProperties;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.common.enums.EquipmentStatus;
import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.IssueSeverity;
import com.sitemaster.common.enums.IssueType;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.common.enums.Urgency;
import com.sitemaster.common.enums.WeatherCondition;
import com.sitemaster.dailyreport.DailyReportService;
import com.sitemaster.dailyreport.dto.DailyReportRequest;
import com.sitemaster.dailyreport.dto.MaterialUsageRequest;
import com.sitemaster.employee.EmployeeRepository;
import com.sitemaster.employee.EmployeeService;
import com.sitemaster.employee.dto.EmployeeCreateRequest;
import com.sitemaster.equipment.EquipmentService;
import com.sitemaster.equipment.dto.EquipmentConditionRequest;
import com.sitemaster.equipment.dto.EquipmentRequest;
import com.sitemaster.expense.ExpenseService;
import com.sitemaster.expense.dto.ExpenseForm;
import com.sitemaster.expense.dto.ExpensePaymentRequest;
import com.sitemaster.hr.HrRecordsService;
import com.sitemaster.hr.dto.ContractRequest;
import com.sitemaster.hr.dto.TrainingRequest;
import com.sitemaster.issue.IssueService;
import com.sitemaster.issue.dto.IssueRequest;
import com.sitemaster.leave.LeaveService;
import com.sitemaster.leave.LeaveTypeRepository;
import com.sitemaster.leave.dto.LeaveApplication;
import com.sitemaster.material.MaterialService;
import com.sitemaster.material.dto.MaterialRequestBody;
import com.sitemaster.materialrequest.MaterialRequestService;
import com.sitemaster.materialrequest.dto.DeliveryRequest;
import com.sitemaster.materialrequest.dto.MaterialRequestCreate;
import com.sitemaster.payroll.PayrollService;
import com.sitemaster.pettycash.PettyCashService;
import com.sitemaster.pettycash.dto.FundRequest;
import com.sitemaster.pettycash.dto.TopUpRequest;
import com.sitemaster.project.ProjectPlanService;
import com.sitemaster.project.ProjectRepository;
import com.sitemaster.project.ProjectService;
import com.sitemaster.project.ProjectTeamService;
import com.sitemaster.project.dto.MilestoneRequest;
import com.sitemaster.project.dto.PhaseRequest;
import com.sitemaster.project.dto.ProjectRequest;
import com.sitemaster.purchase.InvoicePaymentService;
import com.sitemaster.purchase.PurchaseOrderService;
import com.sitemaster.purchase.dto.CreatePurchaseOrderRequest;
import com.sitemaster.purchase.dto.InvoiceUploadForm;
import com.sitemaster.purchase.dto.PaymentRequest;
import com.sitemaster.purchase.dto.PurchaseOrderResponse;
import com.sitemaster.settings.WorkCalendar;
import com.sitemaster.supplier.Supplier;
import com.sitemaster.supplier.SupplierApplicationService;
import com.sitemaster.supplier.SupplierRepository;
import com.sitemaster.supplier.dto.SupplierApplicationForm;
import com.sitemaster.task.TaskService;
import com.sitemaster.task.dto.TaskProgressRequest;
import com.sitemaster.task.dto.TaskRequest;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds a realistic demo company on an empty database by running the real
 * workflows through the services, signed in as the right person at each step.
 * Every business rule, notification and audit entry therefore happens exactly
 * as it would through the UI. All dates are relative to today, so the demo is
 * always current: four projects (planned, active, delayed, completed), a full
 * material purchase cycle, daily reports, last month's attendance and payroll,
 * leave, expenses, petty cash, client invoices and HR records.
 * <p>
 * Runs in one transaction: if anything fails, no partial demo data is left.
 */
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    static final String ADMIN = "admin@sitemaster.lk";
    static final String PM = "pm@sitemaster.lk";
    static final String ENGINEER = "engineer@sitemaster.lk";
    static final String WORKER = "worker@sitemaster.lk";
    static final String HR = "hr@sitemaster.lk";
    static final String ACCOUNTANT = "accountant@sitemaster.lk";
    static final String SUPPLIER = "supplier@sitemaster.lk";
    static final String CARPENTER = "pradeep@sitemaster.lk";
    static final String ELECTRICIAN = "ajith@sitemaster.lk";
    static final String LABOURER = "roshan@sitemaster.lk";
    static final String STEEL_FIXER = "ishara@sitemaster.lk";
    static final String STEEL_SUPPLIER = "sales@ceylonsteel.lk";

    private final RunAs as;
    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final WorkCalendar calendar;
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final SupplierRepository suppliers;
    private final ProjectRepository projects;
    private final OvertimeRepository overtimeRepository;
    private final LeaveTypeRepository leaveTypes;
    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final SupplierApplicationService applicationService;
    private final MaterialService materialService;
    private final EquipmentService equipmentService;
    private final ProjectService projectService;
    private final ProjectTeamService teamService;
    private final ProjectPlanService planService;
    private final TaskService taskService;
    private final IssueService issueService;
    private final MaterialRequestService requestService;
    private final PurchaseOrderService orderService;
    private final InvoicePaymentService invoicePaymentService;
    private final DailyReportService reportService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final HrRecordsService hrRecordsService;
    private final ExpenseService expenseService;
    private final PettyCashService pettyCashService;
    private final ClientInvoiceService invoiceService;

    private LocalDate today;
    private long refCounter = 1000;

    @SuppressWarnings("java:S107")
    public DemoDataSeeder(RunAs as, AppProperties properties, PasswordEncoder passwordEncoder, Clock clock,
                          WorkCalendar calendar, UserRepository users, EmployeeRepository employees,
                          SupplierRepository suppliers,
                          ProjectRepository projects, OvertimeRepository overtimeRepository,
                          LeaveTypeRepository leaveTypes, ClientService clientService, EmployeeService employeeService,
                          SupplierApplicationService applicationService, MaterialService materialService,
                          EquipmentService equipmentService, ProjectService projectService,
                          ProjectTeamService teamService, ProjectPlanService planService, TaskService taskService,
                          IssueService issueService, MaterialRequestService requestService,
                          PurchaseOrderService orderService, InvoicePaymentService invoicePaymentService,
                          DailyReportService reportService, AttendanceService attendanceService,
                          LeaveService leaveService, PayrollService payrollService, HrRecordsService hrRecordsService,
                          ExpenseService expenseService, PettyCashService pettyCashService,
                          ClientInvoiceService invoiceService) {
        this.as = as;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.calendar = calendar;
        this.users = users;
        this.employees = employees;
        this.suppliers = suppliers;
        this.projects = projects;
        this.overtimeRepository = overtimeRepository;
        this.leaveTypes = leaveTypes;
        this.clientService = clientService;
        this.employeeService = employeeService;
        this.applicationService = applicationService;
        this.materialService = materialService;
        this.equipmentService = equipmentService;
        this.projectService = projectService;
        this.teamService = teamService;
        this.planService = planService;
        this.taskService = taskService;
        this.issueService = issueService;
        this.requestService = requestService;
        this.orderService = orderService;
        this.invoicePaymentService = invoicePaymentService;
        this.reportService = reportService;
        this.attendanceService = attendanceService;
        this.leaveService = leaveService;
        this.payrollService = payrollService;
        this.hrRecordsService = hrRecordsService;
        this.expenseService = expenseService;
        this.pettyCashService = pettyCashService;
        this.invoiceService = invoiceService;
    }

    /** True on a fresh database: the demo accounts exist and no project has been created yet. */
    public boolean shouldRun() {
        return projects.count() == 0 && users.findByEmailIgnoreCase(ADMIN).isPresent()
                && users.findByEmailIgnoreCase(PM).isPresent();
    }

    @Transactional
    public void seed() {
        today = LocalDate.now(clock);
        Map<String, Long> clientIds = seedClients();
        Map<String, Long> materialIds = seedMaterials();
        seedSuppliers();
        seedEmployees();
        Demo demo = seedProjects(clientIds);
        seedPlans(demo);
        seedEquipment(demo);
        seedIssues(demo);
        seedPurchasing(demo, materialIds);
        seedDailyReports(demo, materialIds);
        seedExpensesAndPettyCash(demo);
        seedAttendance(demo);
        seedLeave();
        seedPayroll();
        seedHrRecords();
        seedClientInvoices(demo);
        finishProjects(demo);
        seedTodayCheckIns(demo);
        log.info("Demo data created: 4 projects, purchasing, attendance, payroll, finance and HR records");
    }

    // ----- master data --------------------------------------------------------------------

    private Map<String, Long> seedClients() {
        return as.user(ADMIN, () -> Map.of(
                "kandy", client("Hill Capital Developers (Pvt) Ltd", "Mahinda Ratnayake", "projects@hillcapital.lk",
                        "0812234567", "45 Peradeniya Road, Kandy"),
                "colombo", client("Ocean Gate Holdings", "Shanika de Mel", "facilities@oceangate.lk",
                        "0112345678", "12 Galle Face Terrace, Colombo 03"),
                "negombo", client("Dr. Nalaka and Mrs. Iresha Abeysekera", "Nalaka Abeysekera", "nalaka.abeysekera@gmail.com",
                        "0771234567", "8 Lewis Place, Negombo"),
                "kurunegala", client("Wayamba Agro Exports", "Thilak Senanayake", "admin@wayamba-agro.lk",
                        "0372223344", "Puttalam Road, Kurunegala")));
    }

    private Long client(String name, String contact, String email, String phone, String address) {
        ClientRequest r = new ClientRequest();
        r.setName(name);
        r.setContactPerson(contact);
        r.setEmail(email);
        r.setPhone(phone);
        r.setAddress(address);
        return clientService.create(r).id();
    }

    private Map<String, Long> seedMaterials() {
        return as.user(ADMIN, () -> Map.of(
                "cement", material("Cement OPC 50kg", "Cement", "bag", "2450.00"),
                "sand", material("River sand", "Sand", "m3", "18500.00"),
                "metal", material("Aggregate 20mm", "Aggregate", "m3", "14000.00"),
                "steel", material("Steel bar 12mm", "Steel", "kg", "345.00"),
                "blocks", material("Cement block 6in", "Bricks and Blocks", "piece", "95.00"),
                "wire", material("Electrical wire 2.5mm", "Electrical", "roll", "9800.00"),
                "paint", material("Emulsion paint white", "Paint", "litre", "1650.00"),
                "pipe", material("PVC pipe 4in", "Plumbing", "piece", "2900.00")));
    }

    private Long material(String name, String category, String unit, String price) {
        MaterialRequestBody r = new MaterialRequestBody();
        r.setName(name);
        r.setCategory(category);
        r.setUnit(unit);
        r.setUnitPrice(new BigDecimal(price));
        r.setDescription("Standard catalogue item");
        return materialService.create(r).id();
    }

    /** Links the demo supplier login to a company, and runs the public application workflow for a second. */
    private void seedSuppliers() {
        User supplierUser = users.findByEmailIgnoreCase(SUPPLIER).orElseThrow();
        Supplier lanka = new Supplier(supplierUser, "Lanka Building Supplies (Pvt) Ltd", "PV 104522");
        lanka.updateContact(supplierUser.getFullName(), SUPPLIER, "0112456789", "220 High Level Road, Nugegoda",
                List.of("Cement", "Sand", "Aggregate", "Bricks and Blocks"));
        suppliers.save(lanka);

        as.anonymous(() -> applicationService.submit(application("Ceylon Steel Traders", "PV 087311", "Rohan Wijesuriya",
                STEEL_SUPPLIER, "0114567890", "77 Sri Sangaraja Mawatha, Colombo 10", List.of("Steel", "Hardware"))));
        as.anonymous(() -> applicationService.submit(application("Colombo Timber Mart", "PV 112908", "Asela Gunawardena",
                "info@colombotimber.lk", "0112890123", "15 Panchikawatta Road, Colombo 10", List.of("Timber"))));
        as.run(ADMIN, () -> applicationService.search(null, page()).content().stream()
                .filter(a -> a.companyName().equals("Ceylon Steel Traders"))
                .findFirst()
                .ifPresent(a -> applicationService.approve(a.id(), "Registration and references checked.")));
        setDemoPassword(STEEL_SUPPLIER);
    }

    private SupplierApplicationForm application(String company, String regNo, String contact, String email, String phone,
                                                String address, List<String> categories) {
        SupplierApplicationForm f = new SupplierApplicationForm();
        f.setCompanyName(company);
        f.setRegistrationNo(regNo);
        f.setContactPerson(contact);
        f.setEmail(email);
        f.setPhone(phone);
        f.setAddress(address);
        f.setMaterialCategories(categories);
        f.setRegistrationDocument(SeedFiles.pdf("business-registration.pdf", company,
                "Business registration " + regNo));
        return f;
    }

    private void seedEmployees() {
        LocalDate joined = today.minusYears(2).withDayOfMonth(1);
        as.run(HR, () -> {
            employee(PM, null, "Nimal Perera", "0771112233", "198512345678", 1985, EmploymentType.PERMANENT,
                    null, "250000.00", joined.minusYears(3));
            employee(ENGINEER, null, "Kasun Silva", "0772223344", "199023456789", 1990, EmploymentType.PERMANENT,
                    null, "180000.00", joined);
            employee(WORKER, "Mason", "Saman Kumara", "0773334455", "882345678V", 1988, EmploymentType.DAILY_WAGE,
                    "3500.00", null, joined);
            employee(HR, null, "Dilani Fernando", "0774445566", "199134567890", 1991, EmploymentType.PERMANENT,
                    null, "160000.00", joined.minusYears(1));
            employee(ACCOUNTANT, null, "Ruwan Jayasinghe", "0775556677", "198745678901", 1987, EmploymentType.PERMANENT,
                    null, "170000.00", joined.minusYears(1));
            newWorker(CARPENTER, "Pradeep Bandara", "Carpenter", "0716667788", "912345670V", 1991, "3200.00", joined);
            newWorker(ELECTRICIAN, "Ajith Gunasekara", "Electrician", "0717778899", "852345671V", 1985, "3800.00", joined);
            newWorker(LABOURER, "Roshan Dissanayake", "Labourer", "0718889900", "972345672V", 1997, "2800.00",
                    today.minusMonths(8).withDayOfMonth(1));

            EmployeeCreateRequest contract = details(new EmployeeCreateRequest(), "Ishara Herath", "0719990011",
                    "199456789012", 1994, "Steel Fixer", today.minusMonths(6).withDayOfMonth(1));
            contract.setEmail(STEEL_FIXER);
            contract.setEmploymentType(EmploymentType.CONTRACT);
            contract.setContractAmount(new BigDecimal("540000.00"));
            contract.setContractStartDate(today.minusMonths(3).withDayOfMonth(1));
            contract.setContractEndDate(today.plusMonths(3).withDayOfMonth(1).minusDays(1));
            employeeService.create(contract);
        });
        for (String email : List.of(CARPENTER, ELECTRICIAN, LABOURER, STEEL_FIXER)) {
            setDemoPassword(email);
        }
    }

    private void employee(String email, String trade, String name, String phone, String nic, int birthYear,
                          EmploymentType type, String dailyRate, String salary, LocalDate joined) {
        EmployeeCreateRequest r = details(new EmployeeCreateRequest(), name, phone, nic, birthYear, trade, joined);
        r.setExistingUserId(users.findByEmailIgnoreCase(email).orElseThrow().getId());
        r.setEmploymentType(type);
        r.setDailyRate(dailyRate == null ? null : new BigDecimal(dailyRate));
        r.setMonthlySalary(salary == null ? null : new BigDecimal(salary));
        employeeService.create(r);
    }

    private void newWorker(String email, String name, String trade, String phone, String nic, int birthYear,
                           String dailyRate, LocalDate joined) {
        EmployeeCreateRequest r = details(new EmployeeCreateRequest(), name, phone, nic, birthYear, trade, joined);
        r.setEmail(email);
        r.setEmploymentType(EmploymentType.DAILY_WAGE);
        r.setDailyRate(new BigDecimal(dailyRate));
        employeeService.create(r);
    }

    private static EmployeeCreateRequest details(EmployeeCreateRequest r, String name, String phone, String nic,
                                                 int birthYear, String trade, LocalDate joined) {
        r.setFullName(name);
        r.setPhone(phone);
        r.setNic(nic);
        r.setDateOfBirth(LocalDate.of(birthYear, 6, 15));
        r.setAddress("No. 10, Temple Road, Maharagama");
        r.setJoinDate(joined);
        r.setTrade(trade);
        r.setEmergencyContactName("Family contact");
        r.setEmergencyContactPhone("0112233445");
        return r;
    }

    /** Accounts created by workflows get a random password; demo accounts all use the seed password. */
    private void setDemoPassword(String email) {
        users.findByEmailIgnoreCase(email)
                .ifPresent(u -> u.changePasswordHash(passwordEncoder.encode(properties.seed().defaultPassword())));
    }

    // ----- projects -----------------------------------------------------------------------

    /** Ids the later steps need. */
    private static final class Demo {
        Long kandy;
        Long galle;
        Long negombo;
        Long kurunegala;
        Long kandyStructure;
        Long galleFitOut;
        Long negomboBuild;
    }

    private Demo seedProjects(Map<String, Long> clients) {
        Long pm = userId(PM);
        Demo d = new Demo();
        as.run(ADMIN, () -> {
            d.kandy = project("Kandy Road Apartments", "Twelve-unit apartment block with basement parking.",
                    clients.get("kandy"), pm, "Kandy Road, Peradeniya", "7.2700", "80.5950",
                    today.minusDays(150), today.plusDays(300), "85000000.00");
            d.galle = project("Galle Face Office Fit-out", "Interior fit-out of three office floors.",
                    clients.get("colombo"), pm, "Galle Face, Colombo 03", "6.9271", "79.8450",
                    today.minusDays(80), today.plusDays(40), "25000000.00");
            d.negombo = project("Negombo Beach Villa", "Two-storey beach house with pool.",
                    clients.get("negombo"), pm, "Lewis Place, Negombo", "7.2083", "79.8358",
                    today.minusDays(400), today.minusDays(20), "30000000.00");
            d.kurunegala = project("Kurunegala Warehouse", "Steel-frame storage warehouse with loading bays.",
                    clients.get("kurunegala"), pm, "Puttalam Road, Kurunegala", "7.4863", "80.3647",
                    today.plusDays(30), today.plusDays(400), "60000000.00");
            for (Long id : List.of(d.kandy, d.galle, d.negombo)) {
                projectService.changeStatus(id, "ACTIVE");
            }
        });
        as.run(PM, () -> {
            for (String email : List.of(ENGINEER, WORKER, CARPENTER, ELECTRICIAN, LABOURER)) {
                teamService.add(d.kandy, userId(email));
            }
            teamService.add(d.galle, userId(ENGINEER));
            teamService.add(d.galle, userId(STEEL_FIXER));
            teamService.add(d.negombo, userId(ENGINEER));
        });
        return d;
    }

    private Long project(String name, String description, Long clientId, Long managerId, String location, String lat,
                         String lng, LocalDate start, LocalDate end, String budget) {
        ProjectRequest r = new ProjectRequest();
        r.setName(name);
        r.setDescription(description);
        r.setClientId(clientId);
        r.setManagerId(managerId);
        r.setLocation(location);
        r.setLatitude(new BigDecimal(lat));
        r.setLongitude(new BigDecimal(lng));
        r.setStartDate(start);
        r.setEndDate(end);
        r.setBudget(new BigDecimal(budget));
        return projectService.create(r).id();
    }

    private void seedPlans(Demo d) {
        Long engineer = userId(ENGINEER);
        Long saman = userId(WORKER);
        Long pradeep = userId(CARPENTER);
        Long ajith = userId(ELECTRICIAN);
        as.run(PM, () -> {
            Long foundation = phase(d.kandy, "Foundation", today.minusDays(150), today.minusDays(60), "20000000.00", 1);
            d.kandyStructure = phase(d.kandy, "Structure", today.minusDays(59), today.plusDays(90), "40000000.00", 2);
            phase(d.kandy, "Finishing", today.plusDays(91), today.plusDays(300), "20000000.00", 3);
            Long excavation = task(d.kandy, foundation, "Excavate and level basement", engineer, Urgency.HIGH, today.minusDays(120));
            Long footings = task(d.kandy, foundation, "Cast column footings", saman, Urgency.HIGH, today.minusDays(80));
            planService.changePhaseStatus(foundation, "IN_PROGRESS");
            planService.changePhaseStatus(d.kandyStructure, "IN_PROGRESS");
            Long columns = task(d.kandy, d.kandyStructure, "Cast ground floor columns", saman, Urgency.HIGH, today.plusDays(10));
            task(d.kandy, d.kandyStructure, "Install first floor formwork", pradeep, Urgency.NORMAL, today.plusDays(20));
            task(d.kandy, d.kandyStructure, "Fix rebar for first floor slab", engineer, Urgency.NORMAL, today.plusDays(30));
            Long conduits = task(d.kandy, d.kandyStructure, "Lay electrical conduits, ground floor", ajith, Urgency.LOW,
                    today.plusDays(45));
            milestone(d.kandy, foundation, "Foundation complete", today.minusDays(60), true);
            milestone(d.kandy, d.kandyStructure, "First floor slab cast", today.plusDays(35), false);
            progress(excavation, TaskStatus.DONE, 100);
            progress(footings, TaskStatus.DONE, 100);
            planService.changePhaseStatus(foundation, "COMPLETED");
            progress(columns, TaskStatus.IN_PROGRESS, 60);
            progress(conduits, TaskStatus.IN_PROGRESS, 20);

            Long design = phase(d.galle, "Design and demolition", today.minusDays(80), today.minusDays(30), "6000000.00", 1);
            d.galleFitOut = phase(d.galle, "Fit-out", today.minusDays(29), today.plusDays(40), "18000000.00", 2);
            planService.changePhaseStatus(design, "IN_PROGRESS");
            planService.changePhaseStatus(design, "COMPLETED");
            planService.changePhaseStatus(d.galleFitOut, "IN_PROGRESS");
            task(d.galle, d.galleFitOut, "Install glass partitions, level 4", engineer, Urgency.URGENT, today.plusDays(7));
            task(d.galle, d.galleFitOut, "Ceiling grid and lighting, level 5", userId(STEEL_FIXER), Urgency.NORMAL,
                    today.plusDays(25));

            d.negomboBuild = phase(d.negombo, "Construction", today.minusDays(400), today.minusDays(20), "28000000.00", 1);
            planService.changePhaseStatus(d.negomboBuild, "IN_PROGRESS");
            Long handover = task(d.negombo, d.negomboBuild, "Snagging and handover", engineer, Urgency.HIGH, today.minusDays(25));
            progress(handover, TaskStatus.DONE, 100);
            planService.changePhaseStatus(d.negomboBuild, "COMPLETED");
        });
        as.run(PM, () -> phase(d.kurunegala, "Site preparation", today.plusDays(30), today.plusDays(90), "8000000.00", 1));
    }

    private Long phase(Long projectId, String name, LocalDate start, LocalDate end, String budget, int order) {
        PhaseRequest r = new PhaseRequest();
        r.setName(name);
        r.setDescription(name + " works");
        r.setStartDate(start);
        r.setEndDate(end);
        r.setBudget(new BigDecimal(budget));
        r.setSortOrder(order);
        return planService.createPhase(projectId, r).id();
    }

    private Long task(Long projectId, Long phaseId, String title, Long assignee, Urgency priority, LocalDate due) {
        TaskRequest r = new TaskRequest();
        r.setPhaseId(phaseId);
        r.setTitle(title);
        r.setDescription(title + ".");
        r.setAssigneeId(assignee);
        r.setPriority(priority);
        r.setDueDate(due);
        return taskService.create(projectId, r).id();
    }

    private void progress(Long taskId, TaskStatus status, int percent) {
        TaskProgressRequest r = new TaskProgressRequest();
        r.setStatus(status);
        r.setProgress(percent);
        taskService.updateProgress(taskId, r);
    }

    private void milestone(Long projectId, Long phaseId, String name, LocalDate due, boolean done) {
        MilestoneRequest r = new MilestoneRequest();
        r.setName(name);
        r.setDueDate(due);
        r.setPhaseId(phaseId);
        Long id = planService.createMilestone(projectId, r).id();
        if (done) {
            planService.completeMilestone(id, true);
        }
    }

    private void seedEquipment(Demo d) {
        as.run(ADMIN, () -> {
            Long excavator = equipment("EXC-01", "Excavator JCB 3CX", "Excavator");
            Long mixer = equipment("MIX-01", "Concrete mixer 350L", "Mixer");
            equipment("GEN-02", "Generator 30kVA", "Generator");
            Long crane = equipment("CRN-01", "Tower crane 6t", "Crane");
            equipmentService.assign(excavator, d.kandy);
            equipmentService.assign(mixer, d.kandy);
            EquipmentConditionRequest service = new EquipmentConditionRequest();
            service.setStatus(EquipmentStatus.UNDER_MAINTENANCE);
            service.setNotes("Slewing ring inspection due.");
            service.setLastServiceDate(today.minusDays(170));
            service.setNextServiceDate(today.minusDays(5));
            equipmentService.updateCondition(crane, service);
        });
    }

    private Long equipment(String code, String name, String category) {
        EquipmentRequest r = new EquipmentRequest();
        r.setCode(code);
        r.setName(name);
        r.setCategory(category);
        return equipmentService.create(r).id();
    }

    private void seedIssues(Demo d) {
        as.run(ENGINEER, () -> {
            issue(d.kandy, IssueType.ISSUE, "Water seepage in basement wall",
                    "Seepage along the north basement wall after heavy rain. Needs waterproofing review.",
                    IssueSeverity.HIGH, today.plusDays(7));
            issue(d.galle, IssueType.RISK, "Glass panel delivery may slip",
                    "Supplier reports shipping delays; partitions on level 4 depend on this delivery.",
                    IssueSeverity.MEDIUM, today.plusDays(14));
        });
    }

    private void issue(Long projectId, IssueType type, String title, String description, IssueSeverity severity,
                       LocalDate due) {
        IssueRequest r = new IssueRequest();
        r.setType(type);
        r.setTitle(title);
        r.setDescription(description);
        r.setSeverity(severity);
        r.setDueDate(due);
        issueService.report(projectId, r);
    }

    // ----- purchasing, site reports, expenses -----------------------------------------------------

    private void seedPurchasing(Demo d, Map<String, Long> materials) {
        Long lanka = suppliers.findByUserId(userId(SUPPLIER)).orElseThrow().getId();
        Long steelCo = suppliers.findByUserId(userId(STEEL_SUPPLIER)).orElseThrow().getId();

        Long cement = as.user(ENGINEER, () -> materialRequest(d, materials.get("cement"), "200", Urgency.HIGH, 3));
        Long steel = as.user(ENGINEER, () -> materialRequest(d, materials.get("steel"), "1500", Urgency.NORMAL, 10));
        Long sand = as.user(ENGINEER, () -> materialRequest(d, materials.get("sand"), "12", Urgency.NORMAL, 14));
        as.user(ENGINEER, () -> materialRequest(d, materials.get("blocks"), "4000", Urgency.LOW, 21));
        as.run(PM, () -> {
            requestService.approve(cement, "Needed for the column pour.", true);
            requestService.approve(steel, null, true);
            requestService.approve(sand, null, true);
        });

        PurchaseOrderResponse cementPo = as.user(ACCOUNTANT, () -> orderService.create(cement, po(lanka, "2380.00")));
        PurchaseOrderResponse steelPo = as.user(ACCOUNTANT, () -> orderService.create(steel, po(steelCo, "330.00")));
        as.run(SUPPLIER, () -> {
            orderService.accept(cementPo.id());
            orderService.dispatch(cementPo.id(), today);
        });
        PurchaseOrderResponse invoiced = as.user(SUPPLIER, () -> {
            InvoiceUploadForm form = new InvoiceUploadForm();
            form.setInvoiceNumber("LBS/INV/2041");
            form.setInvoiceDate(today);
            form.setAmount(cementPo.totalAmount());
            form.setFile(SeedFiles.pdf("LBS-INV-2041.pdf", "Lanka Building Supplies", "Invoice LBS/INV/2041",
                    "200 bags Cement OPC 50kg"));
            return orderService.uploadInvoice(cementPo.id(), form);
        });
        as.run(ACCOUNTANT, () -> {
            PaymentRequest pay = new PaymentRequest();
            pay.setAmount(invoiced.invoice().amount());
            pay.setMethod(PaymentMethod.BANK_TRANSFER);
            pay.setReferenceNo(nextReference("BT"));
            pay.setPaymentDate(today);
            pay.setNotes("Paid on delivery note.");
            invoicePaymentService.pay(invoiced.invoice().id(), pay);
        });
        as.run(ENGINEER, () -> {
            DeliveryRequest delivery = new DeliveryRequest();
            delivery.setDeliveredQuantity(new BigDecimal("200"));
            delivery.setNotes("All bags received dry.");
            requestService.confirmDelivery(cement, delivery);
        });
        as.run(STEEL_SUPPLIER, () -> {
            orderService.accept(steelPo.id());
            orderService.dispatch(steelPo.id(), today.plusDays(3));
        });
    }

    private Long materialRequest(Demo d, Long materialId, String quantity, Urgency urgency, int neededInDays) {
        MaterialRequestCreate r = new MaterialRequestCreate();
        r.setPhaseId(d.kandyStructure);
        r.setMaterialId(materialId);
        r.setQuantity(new BigDecimal(quantity));
        r.setUrgency(urgency);
        r.setRequiredBy(today.plusDays(neededInDays));
        r.setNotes("For the structure phase.");
        return requestService.create(d.kandy, r).id();
    }

    private static CreatePurchaseOrderRequest po(Long supplierId, String unitPrice) {
        CreatePurchaseOrderRequest r = new CreatePurchaseOrderRequest();
        r.setSupplierId(supplierId);
        r.setUnitPrice(new BigDecimal(unitPrice));
        r.setNotes("Deliver to site gate 2 between 8am and 4pm.");
        return r;
    }

    private void seedDailyReports(Demo d, Map<String, Long> materials) {
        List<Long> reports = new ArrayList<>();
        as.run(ENGINEER, () -> {
            reports.add(report(d.kandy, today.minusDays(3), 4, WeatherCondition.SUNNY,
                    "Formwork for ground floor columns C1-C8 completed and checked.", null, materials.get("cement"), "35"));
            reports.add(report(d.kandy, today.minusDays(2), 4, WeatherCondition.RAINY,
                    "Rain stopped concreting at noon; rebar cages for C9-C12 tied.", "Two hours lost to rain.",
                    materials.get("cement"), "20"));
            reports.add(report(d.kandy, today.minusDays(1), 3, WeatherCondition.CLOUDY,
                    "Columns C9-C12 cast. Curing started on C1-C8.", null, materials.get("cement"), "40"));
        });
        as.run(PM, () -> {
            reportService.approve(reports.get(0), "Good progress.");
            reportService.approve(reports.get(1), null);
        });
    }

    private Long report(Long projectId, LocalDate date, int workers, WeatherCondition weather, String work,
                        String problems, Long materialId, String quantity) {
        DailyReportRequest r = new DailyReportRequest();
        r.setReportDate(date);
        r.setWorkerCount(workers);
        r.setWeather(weather);
        r.setWorkDone(work);
        r.setProblems(problems);
        MaterialUsageRequest used = new MaterialUsageRequest();
        used.setMaterialId(materialId);
        used.setQuantity(new BigDecimal(quantity));
        r.setMaterialsUsed(List.of(used));
        return reportService.submit(projectId, r).id();
    }

    private void seedExpensesAndPettyCash(Demo d) {
        as.run(ACCOUNTANT, () -> {
            FundRequest fund = new FundRequest();
            fund.setCustodianId(userId(ENGINEER));
            fund.setFloatLimit(new BigDecimal("50000.00"));
            fund.setActive(true);
            pettyCashService.saveFund(d.kandy, fund);
            TopUpRequest topUp = new TopUpRequest();
            topUp.setAmount(new BigDecimal("30000.00"));
            topUp.setDate(today.minusDays(7));
            topUp.setDescription("Opening float from main account");
            pettyCashService.topUp(d.kandy, topUp);
        });
        Long scaffolding = as.user(ENGINEER, () -> expense(d, ExpenseCategory.LABOUR, "4500.00", 6,
                "Mahesh Scaffolding", "Scaffolding crew for one day, north elevation.", false));
        Long diesel = as.user(ENGINEER, () -> expense(d, ExpenseCategory.EQUIPMENT, "12000.00", 3,
                "Ceypetco Peradeniya", "Diesel for site generator, 40 litres.", true));
        as.user(ENGINEER, () -> expense(d, ExpenseCategory.OVERHEAD, "3200.00", 1, "Dialog Broadband",
                "Site office internet for the month.", false));
        as.run(PM, () -> {
            expenseService.approve(scaffolding, null, true);
            expenseService.approve(diesel, "Keep fuel log updated.", true);
        });
        as.run(ACCOUNTANT, () -> {
            expenseService.pay(scaffolding, expensePayment(true, PaymentMethod.CASH, null));
            expenseService.pay(diesel, expensePayment(false, PaymentMethod.BANK_TRANSFER, nextReference("BT")));
        });
    }

    private Long expense(Demo d, ExpenseCategory category, String amount, int daysAgo, String payee, String description,
                         boolean withReceipt) {
        ExpenseForm f = new ExpenseForm();
        f.setPhaseId(d.kandyStructure);
        f.setCategory(category);
        f.setAmount(new BigDecimal(amount));
        f.setExpenseDate(today.minusDays(daysAgo));
        f.setPayee(payee);
        f.setDescription(description);
        if (withReceipt) {
            f.setReceipt(SeedFiles.pdf("receipt.pdf", payee, description, "Amount " + amount));
        }
        return expenseService.create(d.kandy, f).id();
    }

    private ExpensePaymentRequest expensePayment(boolean pettyCash, PaymentMethod method, String reference) {
        ExpensePaymentRequest r = new ExpensePaymentRequest();
        r.setFromPettyCash(pettyCash);
        r.setMethod(method);
        r.setReferenceNo(reference);
        r.setPaymentDate(today);
        return r;
    }

    // ----- attendance, leave, payroll -----------------------------------------------------------

    /** Last month and this month up to yesterday, every working day, on the Kandy site. */
    private void seedAttendance(Demo d) {
        LocalDate from = YearMonth.from(today).minusMonths(1).atDay(1);
        LocalDate to = today.minusDays(1);
        Map<String, Long> crew = Map.of(WORKER, employeeIdOf(WORKER), CARPENTER, employeeIdOf(CARPENTER),
                ELECTRICIAN, employeeIdOf(ELECTRICIAN), LABOURER, employeeIdOf(LABOURER));
        Long engineerEmployee = employeeIdOf(ENGINEER);
        List<Long> overtimeIds = new ArrayList<>();
        boolean firstAbsence = true;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (!calendar.isWorkingDay(day)) {
                continue;
            }
            final LocalDate date = day;
            final boolean absentToday = firstAbsence && day.getDayOfWeek() == DayOfWeek.MONDAY;
            if (absentToday) {
                firstAbsence = false;
            }
            as.run(ENGINEER, () -> crew.forEach((email, employeeId) -> {
                AttendanceStatus status = AttendanceStatus.PRESENT;
                if (absentToday && email.equals(LABOURER)) {
                    status = AttendanceStatus.ABSENT;
                } else if (date.getDayOfMonth() == 15 && email.equals(CARPENTER)) {
                    status = AttendanceStatus.HALF_DAY;
                }
                boolean overtime = email.equals(WORKER) && date.getDayOfWeek() == DayOfWeek.WEDNESDAY;
                AttendanceResponse saved = attendanceService.mark(attendance(employeeId, d.kandy, date, status,
                        overtime ? "2" : null));
                if (overtime) {
                    overtimeRepository.findByAttendanceId(saved.id()).ifPresent(o -> overtimeIds.add(o.getId()));
                }
            }));
            as.run(HR, () -> attendanceService.mark(attendance(engineerEmployee, d.kandy, date, AttendanceStatus.PRESENT,
                    null)));
        }
        // HR approves all but the most recent overtime, which stays pending for the demo.
        as.run(HR, () -> overtimeIds.stream().limit(Math.max(0, overtimeIds.size() - 1))
                .forEach(id -> attendanceService.decideOvertime(id, true, "Column pour ran late.")));
    }

    private static AttendanceRequest attendance(Long employeeId, Long projectId, LocalDate date, AttendanceStatus status,
                                                String overtimeHours) {
        AttendanceRequest r = new AttendanceRequest();
        r.setEmployeeId(employeeId);
        r.setProjectId(projectId);
        r.setDate(date);
        r.setStatus(status);
        if (status == AttendanceStatus.PRESENT) {
            r.setCheckIn(LocalTime.of(7, 55));
            r.setCheckOut(overtimeHours == null ? LocalTime.of(17, 5) : LocalTime.of(19, 5));
        } else if (status == AttendanceStatus.HALF_DAY) {
            r.setCheckIn(LocalTime.of(8, 0));
            r.setCheckOut(LocalTime.of(12, 30));
        }
        if (overtimeHours != null) {
            r.setOvertimeHours(new BigDecimal(overtimeHours));
            r.setOvertimeReason("Concrete pour finished after hours.");
        }
        return r;
    }

    private void seedLeave() {
        Long annual = leaveTypes.findAll().stream().filter(t -> t.getCode().equals("ANNUAL")).findFirst().orElseThrow().getId();
        Long casual = leaveTypes.findAll().stream().filter(t -> t.getCode().equals("CASUAL")).findFirst().orElseThrow().getId();
        LocalDate workerFrom = nextWorkingDay(today.plusDays(7));
        as.run(WORKER, () -> leaveService.apply(leave(annual, workerFrom, nextWorkingDay(workerFrom.plusDays(1)),
                "Family wedding in Matara.")));
        LocalDate engineerDay = nextWorkingDay(today.plusDays(14));
        Long engineerLeave = as.user(ENGINEER, () -> leaveService.apply(leave(casual, engineerDay, engineerDay,
                "Personal matter at the bank.")).id());
        as.run(HR, () -> leaveService.approve(engineerLeave, "Enjoy the day."));
    }

    private static LeaveApplication leave(Long typeId, LocalDate from, LocalDate to, String reason) {
        LeaveApplication a = new LeaveApplication();
        a.setLeaveTypeId(typeId);
        a.setFromDate(from);
        a.setToDate(to);
        a.setReason(reason);
        return a;
    }

    /** Last month's payroll: HR sends the summary, the Accountant calculates and pays. */
    private void seedPayroll() {
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
        Long runId = as.user(HR, () -> payrollService.sendSummary(lastMonth).id());
        as.run(ACCOUNTANT, () -> {
            payrollService.calculate(runId);
            payrollService.payAll(runId, PaymentMethod.BANK_TRANSFER,
                    "SAL-" + lastMonth.getYear() + "-" + String.format("%02d", lastMonth.getMonthValue()));
        });
    }

    private void seedHrRecords() {
        as.run(HR, () -> {
            contract(STEEL_FIXER, "CTR-2026-014", "Steel fixing, Galle Face fit-out", today.minusDays(60),
                    today.plusDays(20), "540000.00");
            contract(ENGINEER, "CTR-2024-002", "Site engineer employment contract", today.minusYears(2),
                    today.plusYears(1), null);
            training(WORKER, "Working at heights", "National Institute of Occupational Safety", true,
                    today.minusDays(200), today.plusDays(165));
            training(ENGINEER, "First aid at work", "Sri Lanka Red Cross", true, today.minusDays(740),
                    today.minusDays(10));
            training(ELECTRICIAN, "NVQ Level 4 Electrician", "VTA Sri Lanka", false, today.minusYears(3), null);
        });
    }

    private void contract(String email, String number, String title, LocalDate start, LocalDate end, String value) {
        ContractRequest r = new ContractRequest();
        r.setEmployeeId(employeeIdOf(email));
        r.setContractNo(number);
        r.setTitle(title);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setContractValue(value == null ? null : new BigDecimal(value));
        hrRecordsService.createContract(r);
    }

    private void training(String email, String title, String provider, boolean safety, LocalDate completed,
                          LocalDate expires) {
        TrainingRequest r = new TrainingRequest();
        r.setEmployeeId(employeeIdOf(email));
        r.setTitle(title);
        r.setProvider(provider);
        r.setSafety(safety);
        r.setCompletedOn(completed);
        r.setExpiresOn(expires);
        hrRecordsService.createTraining(r);
    }

    // ----- billing and wrap-up -------------------------------------------------------------------

    private void seedClientInvoices(Demo d) {
        as.run(ACCOUNTANT, () -> {
            Long first = invoiceService.create(invoice(d.kandy, today.minusDays(45), today.minusDays(15),
                    "Progress billing 1: foundation works", "15000000.00")).id();
            invoiceService.issue(first);
            invoiceService.receive(first, receipt("10000000.00", today.minusDays(20)));
            Long second = invoiceService.create(invoice(d.kandy, today.minusDays(5), today.plusDays(25),
                    "Progress billing 2: ground floor structure", "12500000.00")).id();
            invoiceService.issue(second);
            Long villa = invoiceService.create(invoice(d.negombo, today.minusDays(60), today.minusDays(30),
                    "Final account: villa construction", "29500000.00")).id();
            invoiceService.issue(villa);
            invoiceService.receive(villa, receipt("29500000.00", today.minusDays(35)));
            invoiceService.create(invoice(d.galle, today, today.plusDays(30), "Design and demolition stage",
                    "4000000.00"));
        });
    }

    private static ClientInvoiceRequest invoice(Long projectId, LocalDate issued, LocalDate due, String line,
                                                String amount) {
        ClientInvoiceRequest r = new ClientInvoiceRequest();
        r.setProjectId(projectId);
        r.setIssueDate(issued);
        r.setDueDate(due);
        r.setTaxRate(BigDecimal.ZERO);
        r.setNotes("Please pay to Commercial Bank, A/C 1000 2345 6789, SiteMaster Constructions.");
        InvoiceLineRequest l = new InvoiceLineRequest();
        l.setDescription(line);
        l.setQuantity(BigDecimal.ONE);
        l.setUnitPrice(new BigDecimal(amount));
        r.setLines(List.of(l));
        return r;
    }

    private ReceiptRequest receipt(String amount, LocalDate date) {
        ReceiptRequest r = new ReceiptRequest();
        r.setAmount(new BigDecimal(amount));
        r.setPaymentDate(date);
        r.setMethod(PaymentMethod.BANK_TRANSFER);
        r.setReferenceNo(nextReference("CL"));
        return r;
    }

    private void finishProjects(Demo d) {
        as.run(PM, () -> {
            projectService.changeStatus(d.galle, "DELAYED");
            projectService.changeStatus(d.negombo, "COMPLETED");
            projectService.changeCover(d.negombo, SeedFiles.png("negombo-villa.png", "Negombo Beach Villa",
                    new Color(249, 115, 22)));
            projectService.changeCover(d.kandy, SeedFiles.png("kandy-apartments.png", "Kandy Road Apartments",
                    new Color(30, 64, 120)));
        });
    }

    /** Two workers have checked in today and wait for the engineer to verify them. */
    private void seedTodayCheckIns(Demo d) {
        as.run(WORKER, () -> attendanceService.checkIn(d.kandy));
        as.run(CARPENTER, () -> attendanceService.checkIn(d.kandy));
    }

    // ----- helpers -----------------------------------------------------------------------------

    private Long userId(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private Long employeeIdOf(String email) {
        return employees.findByUserId(userId(email))
                .orElseThrow(() -> new IllegalStateException("No employee for " + email))
                .getId();
    }

    private LocalDate nextWorkingDay(LocalDate date) {
        LocalDate day = date;
        while (!calendar.isWorkingDay(day)) {
            day = day.plusDays(1);
        }
        return day;
    }

    private String nextReference(String prefix) {
        refCounter++;
        return prefix + "-" + today.getYear() + "-" + refCounter;
    }

    private static PageQuery page() {
        PageQuery query = new PageQuery();
        query.setSize(100);
        return query;
    }
}
