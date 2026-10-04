const fs = require('fs');
const {Document,Packer,Paragraph,TextRun,Table,TableRow,TableCell,WidthType,HeadingLevel,AlignmentType,Footer,Header,PageNumber,ShadingType} = require('C:/Users/musab/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/docx');
const out = 'ShikkhaSetu_Project_Proposal_CSE327';
const blue='173B59', gray='526270';
const p=(text,opts={})=>new Paragraph({spacing:{after:110,line:270},children:[new TextRun(text)],...opts});
const h=(text)=>p(text,{heading:HeadingLevel.HEADING_1,spacing:{before:170,after:100},keepNext:true});
const sub=(text)=>p(text,{heading:HeadingLevel.HEADING_2,spacing:{before:120,after:70},keepNext:true});
const table=(headers,rows,widths)=>new Table({width:{size:9360,type:WidthType.DXA},columnWidths:widths,rows:[headers,...rows].map((r,i)=>new TableRow({tableHeader:i===0,cantSplit:true,children:r.map((v,j)=>new TableCell({width:{size:widths[j],type:WidthType.DXA},margins:{top:85,bottom:85,left:110,right:110},shading:i===0?{fill:blue,type:ShadingType.CLEAR}:undefined,children:[new Paragraph({spacing:{after:0,line:240},children:[new TextRun({text:v,size:20,bold:i===0,color:i===0?'FFFFFF':'24313C'})]})]}))}))});
const page=(children)=>({properties:{page:{size:{width:11906,height:16838},margin:{top:1050,bottom:1000,left:1273,right:1273}}},headers:{default:new Header({children:[p('BUBT  |  CSE 327: SOFTWARE ENGINEERING',{spacing:{after:120},children:[new TextRun({text:'BUBT  |  CSE 327: SOFTWARE ENGINEERING',size:17,color:gray})]})]})},footers:{default:new Footer({children:[new Paragraph({alignment:AlignmentType.RIGHT,children:[new TextRun({text:'ShikkhaSetu  |  ',size:18,color:gray}),new TextRun({children:[PageNumber.CURRENT],size:18,color:gray})]})]})},children});
const cover=[
 p('BANGLADESH UNIVERSITY OF\u00a0BUSINESS AND TECHNOLOGY',{alignment:AlignmentType.CENTER,spacing:{before:650,after:130},children:[new TextRun({text:'BANGLADESH UNIVERSITY OF BUSINESS AND TECHNOLOGY',bold:true,size:28,color:blue})]}),
 p('(BUBT)',{alignment:AlignmentType.CENTER}),
 p('Department of Computer Science and Engineering',{alignment:AlignmentType.CENTER}),
 p('PROJECT PROPOSAL',{alignment:AlignmentType.CENTER,spacing:{before:650,after:180},children:[new TextRun({text:'PROJECT PROPOSAL',bold:true,size:26,color:gray})]}),
 p('ShikkhaSetu',{alignment:AlignmentType.CENTER,spacing:{after:180},children:[new TextRun({text:'ShikkhaSetu',bold:true,size:48,color:blue})]}),
 p('A Community-Based Educational Resource\u00a0Lending and Donation System',{alignment:AlignmentType.CENTER,children:[new TextRun({text:'A Community-Based Educational Resource Lending and Donation System',size:27})],spacing:{after:420,line:330}}),
 p('Course: Software Engineering (CSE 327)',{alignment:AlignmentType.CENTER}),
 p('Credits: 3.00  |  Prerequisite: CSE 317',{alignment:AlignmentType.CENTER}),
 sub('Submitted To'),p('Teacher Name: ____________________________________'),p('Designation: ______________________________________'),
 sub('Submitted By'),p('Student Name(s): __________________________________'),p('Student ID(s): _____________________________________'),p('Intake / Section: ___________________________________'),p('Semester: _________________________________________'),p('Submission Date: ___________________________________')
];
const overview=[h('1. Project Overview'),
 p('ShikkhaSetu is a proposed web application for a university club to coordinate the donation and temporary lending of textbooks, scientific calculators and other educational resources. Students will offer items and submit requests, while a coordinator will verify availability, approve allocations and record handovers and returns. The project will apply software engineering methods to a bounded social problem and deliver a tested prototype with supporting documentation.'),
 h('2. Problem Statement and Motivation'),
 p('Students may need learning materials for a limited period while other students have suitable items that are no longer in use. Informal exchanges can make it difficult to discover available resources, avoid conflicting commitments and track borrowed items. The project addresses this coordination problem through a shared inventory and a traceable request-to-return workflow. Initial interviews with students and a club coordinator will be used to validate these assumptions before the requirements are finalized.'),
 h('3. Objectives'),
 table(['Objective','Expected outcome'],[
 ['Improve access','Allow students to find and request available learning resources through one searchable catalogue.'],
 ['Support responsible reuse','Enable permanent donations and temporary loans with recorded handovers.'],
 ['Ensure reliable allocation','Prevent multiple active allocations of the same physical item.'],
 ['Apply course concepts','Demonstrate suitable GoF patterns, systematic testing, quality measurement and maintainable design.'],
 ['Evaluate social benefit','Measure completed exchanges, unique recipients and request-to-handover time.']
 ],[2500,6860]),
 h('4. Users and Project Scope'),
 p('Students may act as both resource providers and recipients. A club coordinator will moderate listings, approve requests, confirm handovers and manage returns. The initial deployment will serve one campus and one participating club through a responsive web interface.'),
 p('The core scope includes authentication, resource listings, search, requests, allocation, loan tracking, in-app notifications and a basic dashboard. Online payments, commercial sales, courier integration, AI-based eligibility scoring and multi-campus operation are outside the initial scope. Email notifications will be optional, subject to time and service availability.')
];
const design=[h('5. Functional Requirements and Workflow'),
 table(['ID','Proposed requirement'],[
 ['FR-01','Register and authenticate users; enforce student and coordinator permissions.'],
 ['FR-02','Create and moderate listings with category, condition, optional photo and donation or loan mode; give each physical item a unique identifier.'],
 ['FR-03','Search and filter resources, then submit or cancel a pending request.'],
 ['FR-04','Approve or reject requests and reserve items atomically to prevent conflicting allocations.'],
 ['FR-05','Record handover with a single-use pickup code; record loan due dates, returns and item condition.'],
 ['FR-06','Display status notifications and counts of completed donations, loans, overdue items and unique recipients.']
 ],[1100,8260]),
 p('Loan workflow: Available → Reserved → On Loan → Available after confirmed return. Donation workflow: Available → Reserved → Donated. Cancellation before handover releases a reservation. Overdue is derived from the due date of an unreturned loan. Coordinator actions will be recorded in an audit log.'),
 h('6. Architecture and Proposed Technologies'),
 p('A three-layer architecture will separate the responsive user interface, application/domain services and relational database. The proposed stack is React for the frontend, Java with Spring Boot for the backend and MySQL for persistence. Git will support version control; JUnit and API tests will support verification. These choices may be adjusted to team experience and instructor requirements.'),
 p('Core entities will include User, ResourceItem, Category, Request, Allocation, Loan, Notification and AuditLog. Request approval will use a database transaction and a uniqueness or locking rule to protect item availability.'),
 h('7. Planned Application of Design Patterns'),
 table(['Pattern','Proposed application'],[
 ['Factory Method','Loan and donation workflow creators will construct the appropriate request-processing implementation.'],
 ['Strategy','Interchangeable allocation policies will support first-come-first-served selection or coordinator selection.'],
 ['Observer','Domain events such as approval and return will trigger notification handlers.'],
 ['Adapter','A common notification interface will isolate an optional external email provider.']
 ],[2300,7060]),
 p('Composite will be evaluated if nested resource categories are required. Singleton will be considered for immutable application configuration only where justified. Pattern selection and alternatives will be documented rather than adding unnecessary complexity.')
];
const quality=[h('8. Development Methodology'),
 p('The project will follow a sequential waterfall structure: requirements analysis, system design, implementation, testing, delivery and maintenance planning. Each phase will produce a reviewable artifact. Necessary changes after a phase review will be logged with their scope, reason and impact on subsequent work.'),
 h('9. Testing and Quality Assurance'),
 table(['Test activity','Planned verification'],[
 ['Unit testing','Validate request rules, due-date logic, allocation policies and valid status transitions.'],
 ['Integration testing','Verify approval, database updates and notification creation; test transaction rollback on failure.'],
 ['Black-box testing','Use equivalence classes and boundary values for loan periods, required fields and access permissions.'],
 ['White-box / basis path','Draw the approval control-flow graph, calculate cyclomatic complexity and test its independent paths.'],
 ['Validation / system testing','Run complete donation and loan scenarios with representative users; test simultaneous approvals and backup restoration.'],
 ['Environment / debugging','Check mobile layouts and major browsers, simulate network failures, and use logs plus regression tests to verify repairs.']
 ],[2700,6660]),
 sub('Proposed acceptance criteria'),
 p('Two concurrent approvals for one item must produce exactly one active allocation. Unauthorized users must be unable to approve requests or access private request records. Donation and loan workflows must pass their end-to-end tests. A backup must restore the recorded inventory and allocations. No unresolved critical defect may remain at delivery.'),
 sub('Quality measures and course metrics'),
 p('The report will record requirement-to-test traceability, test pass rate, branch coverage for core services, defects by severity and correction effort. Function-point estimation will use identified inputs, outputs, inquiries, internal files and external interfaces; bang metrics will be considered using the method taught in class. Design coupling, source-code complexity and maintenance effort will also be discussed. No measurement will be reported as achieved before testing.'),
 h('10. Reliability, Privacy and Maintenance'),
 p('Password hashing, server-side validation, role-based authorization and restricted access to personal records will be implemented. Financial circumstances will not be displayed publicly. Consenting participants and synthetic data will be used for evaluation where appropriate. Dependency licenses and image permissions will be recorded.'),
 p('Maintenance will cover defect reporting, diagnosis, repair, regression testing and release notes. A backup/restore guide and upgrade procedure will be delivered. Observed downtime and repair effort will support discussion of availability, reliability and maintenance costs. The platform will coordinate exchanges; disputes and damaged or lost items will remain subject to club policy.')
];
const delivery=[h('11. Feasibility, Risks and Sustainability'),
 p('The scope is designed for a small student team over approximately ten weeks, subject to the actual course schedule. Development will use open-source tools and a local demonstration environment. Hosting, email service and club participation will require confirmation before any public pilot. A participating club is the proposed operational owner; its agreement has not yet been obtained.'),
 table(['Risk','Mitigation'],[
 ['Limited participation','Validate demand early and use consented volunteers or clearly labelled synthetic data for demonstration.'],
 ['Lost items or delayed returns','Record condition and due dates, issue reminders and provide coordinator review under club policy.'],
 ['Conflicting allocations / data loss','Use transactional allocation, concurrency tests and verified backups.'],
 ['Scope or schedule overrun','Prioritize the full core workflow and defer optional integrations.']
 ],[2800,6560]),
 h('12. Tentative Work Plan'),
 table(['Week','Activity and deliverable'],[
 ['1–2','Stakeholder interviews, feasibility review, scope definition and Software Requirements Specification (SRS).'],
 ['3–4','Use-case, class and sequence diagrams; ERD, interface prototype and pattern/design rationale.'],
 ['5–7','Implement authentication, catalogue, requests, allocation, handover, returns and notifications.'],
 ['8–9','Execute test plan, repair defects, conduct user validation and collect quality/impact observations.'],
 ['10','Prepare final report, user and maintenance guides, demonstration and project handover.']
 ],[1100,8260]),
 h('13. Expected Deliverables and Social Impact'),
 p('Deliverables will include the working web prototype, source code, SRS, UML diagrams and ERD, design-pattern rationale, test cases and results, defect log, metrics summary, user manual, deployment/backup instructions and final presentation.'),
 p('A proposed pilot of 20–30 consenting students, if feasible, will assess resource access and usability. Social impact will be reported using confirmed handovers, completed loans and donations, unique recipients and median request-to-handover time. Potential benefits include easier access to study materials and increased reuse; actual results will depend on participation and will not be assumed in advance.'),
 h('14. Curriculum Reference'),
 p('Bangladesh University of Business and Technology (BUBT). Software Engineering (CSE 327), 3.00 credits; prerequisite: CSE 317. Curriculum excerpt supplied for this proposal. The project plan maps to its design patterns, testing, quality, metrics, architecture, risk, delivery and maintenance topics.'),
 p('https://classic.bubt.edu.bd/department/curriculam/51#curriculum_165',{children:[new TextRun({text:'https://classic.bubt.edu.bd/department/curriculam/51#curriculum_165',size:18,color:gray})]})
];
const doc = new Document({creator:'',title:'ShikkhaSetu — CSE 327 Project Proposal',description:'BUBT Software Engineering course project proposal',styles:{default:{document:{run:{font:'Calibri',size:22,color:'24313C'},paragraph:{spacing:{after:110,line:270}}}},paragraphStyles:[{id:'Heading1',name:'Heading 1',basedOn:'Normal',next:'Normal',quickFormat:true,run:{font:'Calibri',size:27,bold:true,color:blue}},{id:'Heading2',name:'Heading 2',basedOn:'Normal',next:'Normal',quickFormat:true,run:{font:'Calibri',size:23,bold:true,color:blue}}]},sections:[cover,overview,design,quality,delivery].map(page)});
Packer.toBuffer(doc).then(b=>{fs.writeFileSync(out+'.docx',b);console.log(out+'.docx generated');});
