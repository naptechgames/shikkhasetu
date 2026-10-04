// Revised proposal (v2): Flutter Android app instead of the React web frontend.
// The original ShikkhaSetu_Project_Proposal_CSE327.docx / generate_proposal.cjs are NOT changed.
// Run:  node generate_proposal_v2_mobile.cjs
const fs = require('fs');
const {Document,Packer,Paragraph,TextRun,Table,TableRow,TableCell,WidthType,HeadingLevel,AlignmentType,Footer,Header,PageNumber,ShadingType} = require('C:/Users/musab/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/docx');
const out = 'ShikkhaSetu_Project_Proposal_CSE327_v2_Mobile';
const blue='173B59', gray='526270';
const p=(text,opts={})=>new Paragraph({spacing:{after:110,line:270},children:[new TextRun(text)],...opts});
const h=(text)=>p(text,{heading:HeadingLevel.HEADING_1,spacing:{before:170,after:100},keepNext:true});
const sub=(text)=>p(text,{heading:HeadingLevel.HEADING_2,spacing:{before:120,after:70},keepNext:true});
const table=(headers,rows,widths)=>new Table({width:{size:9360,type:WidthType.DXA},columnWidths:widths,rows:[headers,...rows].map((r,i)=>new TableRow({tableHeader:i===0,cantSplit:true,children:r.map((v,j)=>new TableCell({width:{size:widths[j],type:WidthType.DXA},margins:{top:85,bottom:85,left:110,right:110},shading:i===0?{fill:blue,type:ShadingType.CLEAR}:undefined,children:[new Paragraph({spacing:{after:0,line:240},children:[new TextRun({text:v,size:20,bold:i===0,color:i===0?'FFFFFF':'24313C'})]})]}))}))});
const page=(children)=>({properties:{page:{size:{width:11906,height:16838},margin:{top:1050,bottom:1000,left:1273,right:1273}}},headers:{default:new Header({children:[p('',{spacing:{after:120},children:[new TextRun({text:'BUBT  |  CSE 327: SOFTWARE ENGINEERING',size:17,color:gray})]})]})},footers:{default:new Footer({children:[new Paragraph({alignment:AlignmentType.RIGHT,children:[new TextRun({text:'ShikkhaSetu (revised proposal v2)  |  ',size:18,color:gray}),new TextRun({children:[PageNumber.CURRENT],size:18,color:gray})]})]})},children});
const cover=[
 p('',{alignment:AlignmentType.CENTER,spacing:{before:650,after:130},children:[new TextRun({text:'BANGLADESH UNIVERSITY OF BUSINESS AND TECHNOLOGY',bold:true,size:28,color:blue})]}),
 p('(BUBT)',{alignment:AlignmentType.CENTER}),
 p('Department of Computer Science and Engineering',{alignment:AlignmentType.CENTER}),
 p('',{alignment:AlignmentType.CENTER,spacing:{before:650,after:180},children:[new TextRun({text:'PROJECT PROPOSAL (REVISED, VERSION 2)',bold:true,size:26,color:gray})]}),
 p('',{alignment:AlignmentType.CENTER,spacing:{after:180},children:[new TextRun({text:'ShikkhaSetu',bold:true,size:48,color:blue})]}),
 p('',{alignment:AlignmentType.CENTER,children:[new TextRun({text:'A Community-Based Educational Resource Lending and Donation System (Android Mobile Application)',size:27})],spacing:{after:420,line:330}}),
 p('Course: Software Engineering (CSE 327)',{alignment:AlignmentType.CENTER}),
 p('Credits: 3.00  |  Prerequisite: CSE 317',{alignment:AlignmentType.CENTER}),
 sub('Submitted To'),p('Teacher Name: ____________________________________'),p('Designation: ______________________________________'),
 sub('Submitted By'),p('Student Name(s): __________________________________'),p('Student ID(s): _____________________________________'),p('Intake / Section: ___________________________________'),p('Semester: _________________________________________'),p('Submission Date: ___________________________________')
];
const changes=[h('0. Summary of Changes from Version 1'),
 p('This document replaces the first proposal. The problem, objectives, users, functional requirements, design patterns and test strategy are unchanged. The delivery platform and some technical details were revised as follows.'),
 table(['Topic','Version 1','Version 2 (this document)','Reason'],[
 ['Client','Responsive web application (React)','Android mobile application (Flutter, Dart)','New requirement: the product must be a mobile app.'],
 ['Backend','Java, Spring Boot','Java 17, Spring Boot 3 (unchanged)','Supports transactions, row locking and JUnit testing needed for the allocation rule.'],
 ['Database','MySQL','Embedded H2 (file) by default; MySQL through a configuration profile','No separate database server has to be installed for the demonstration. The same JPA code runs on both.'],
 ['Data model','Separate Allocation and Loan entities','Approval, pickup code, due date and return are stored on the Request entity','Fewer tables and simpler code for a small team; no information is lost.'],
 ['Photo of an item','Optional photo','Not included in this version','Deferred to keep the core workflow complete (see scope).'],
 ['Authentication','Not specified','Random session token, BCrypt password hash, role check in the service layer','Simple to explain and to test.'],
 ['Environment testing','Mobile layouts and major browsers','Android phone on the same Wi-Fi network as the backend','Follows from the platform change.']
 ],[1500,2200,2900,2760])
];
const overview=[h('1. Project Overview'),
 p('ShikkhaSetu is a proposed Android mobile application, supported by a server backend, for a university club to coordinate the donation and temporary lending of textbooks, scientific calculators and other educational resources. Students will offer items and submit requests, while a coordinator will verify availability, approve allocations and record handovers and returns. The project will apply software engineering methods to a bounded social problem and deliver a tested prototype with supporting documentation.'),
 h('2. Problem Statement and Motivation'),
 p('Students may need learning materials for a limited period while other students have suitable items that are no longer in use. Informal exchanges can make it difficult to discover available resources, avoid conflicting commitments and track borrowed items. The project addresses this coordination problem through a shared inventory and a traceable request-to-return workflow. Initial interviews with students and a club coordinator will be used to validate these assumptions before the requirements are finalized.'),
 h('3. Objectives'),
 table(['Objective','Expected outcome'],[
 ['Improve access','Allow students to find and request available learning resources through one searchable catalogue on their phone.'],
 ['Support responsible reuse','Enable permanent donations and temporary loans with recorded handovers.'],
 ['Ensure reliable allocation','Prevent multiple active allocations of the same physical item.'],
 ['Apply course concepts','Demonstrate suitable GoF patterns, systematic testing, quality measurement and maintainable design.'],
 ['Evaluate social benefit','Measure completed exchanges and unique recipients.']
 ],[2500,6860]),
 h('4. Users and Project Scope'),
 p('Students may act as both resource providers and recipients. A club coordinator will moderate listings, approve requests, confirm handovers and manage returns. The initial deployment will serve one campus and one participating club through an Android application that talks to one shared backend.'),
 p('The core scope includes authentication, resource listings, search and filter, requests, allocation, loan tracking, in-app notifications and a basic impact dashboard. Online payments, commercial sales, courier integration, AI-based eligibility scoring, multi-campus operation, item photos, push notifications and an iOS release are outside the initial scope. E-mail notification is represented by an adapter with a stub provider; a real e-mail service is optional.')
];
const design=[h('5. Functional Requirements and Workflow'),
 table(['ID','Proposed requirement'],[
 ['FR-01','Register and authenticate users; enforce student and coordinator permissions on the backend.'],
 ['FR-02','Create and moderate listings with category, condition and donation or loan mode; give each physical item a unique identifier.'],
 ['FR-03','Search and filter resources, then submit or cancel a request.'],
 ['FR-04','Approve or reject requests and reserve items atomically to prevent conflicting allocations.'],
 ['FR-05','Record handover with a single-use pickup code; record loan due dates, returns and item condition.'],
 ['FR-06','Display status notifications and counts of completed donations, loans, overdue items and unique recipients.']
 ],[1100,8260]),
 p('Loan workflow: Available → Reserved → On Loan → Available after confirmed return. Donation workflow: Available → Reserved → Donated. Cancellation before handover releases a reservation. Overdue is derived from the due date of an unreturned loan. Actions will be recorded in an audit log.'),
 h('6. Architecture and Proposed Technologies'),
 p('A three-layer architecture will separate the mobile user interface, the application/domain services and the relational database. The stack is Flutter (Dart) for the Android client, Java 17 with Spring Boot for the backend REST API, and a relational database accessed through JPA: embedded H2 for the local demonstration and MySQL through a configuration profile. Git will support version control; JUnit, API tests and Flutter tests will support verification.'),
 p('The mobile app contains no business rules. Every permission and allocation rule is enforced by the backend, so a modified or outdated app cannot bypass them. During the demonstration the backend runs on a laptop and phones connect over the same Wi-Fi network.'),
 p('Core entities are User, ResourceItem, Request (including approval, pickup code, due date and return), Notification, AuditLog and AuthToken. Request approval uses one database transaction with a row lock on the item, backed by a version column, to protect item availability.'),
 h('7. Planned Application of Design Patterns'),
 table(['Pattern','Proposed application'],[
 ['Factory Method','Loan and donation workflow creators construct the appropriate request-processing implementation.'],
 ['Strategy','Interchangeable allocation policies support first-come-first-served selection or coordinator selection.'],
 ['Observer','Domain events such as approval and return trigger notification and audit-log handlers.'],
 ['Adapter','A common notification interface isolates an optional external e-mail provider.']
 ],[2300,7060]),
 p('Composite, Singleton and State were considered and are not used; the reasons are documented with the design. Pattern selection and alternatives are documented rather than adding unnecessary complexity.')
];
const quality=[h('8. Development Methodology'),
 p('The project will follow a sequential waterfall structure: requirements analysis, system design, implementation, testing, delivery and maintenance planning. Each phase will produce a reviewable artifact. Necessary changes after a phase review will be logged with their scope, reason and impact on subsequent work. This revised proposal is itself such a logged change.'),
 h('9. Testing and Quality Assurance'),
 table(['Test activity','Planned verification'],[
 ['Unit testing','Validate request rules, due-date logic, allocation policies and valid status transitions (JUnit); data parsing and API client (Flutter test).'],
 ['Integration testing','Verify approval, database updates and notification creation; test transaction rollback on failure.'],
 ['Black-box testing','Use equivalence classes and boundary values for loan periods, required fields and access permissions through the HTTP API.'],
 ['White-box / basis path','Draw the approval control-flow graph, calculate cyclomatic complexity and test its independent paths.'],
 ['Validation / system testing','Run complete donation and loan scenarios; test simultaneous approvals; restart the server and confirm that data is kept.'],
 ['Environment / debugging','Install the APK on an Android phone, use it over Wi-Fi, simulate an unreachable server, and use logs plus regression tests to verify repairs.']
 ],[2700,6660]),
 sub('Proposed acceptance criteria'),
 p('Two concurrent approvals for one item must produce exactly one active allocation. Unauthorized users must be unable to approve requests or access private request records. Donation and loan workflows must pass their end-to-end tests. Recorded inventory and allocations must survive a server restart. No unresolved critical defect may remain at delivery.'),
 sub('Quality measures and course metrics'),
 p('The report will record requirement-to-test traceability, test pass rate, branch coverage for core services, defects by severity and correction effort. Function-point estimation will use identified inputs, outputs, inquiries, internal files and external interfaces; bang metrics will be considered using the method taught in class. Design coupling, source-code complexity and maintenance effort will also be discussed. No measurement will be reported as achieved before testing.'),
 h('10. Reliability, Privacy and Maintenance'),
 p('Password hashing, server-side validation, role-based authorization and restricted access to personal records will be implemented. A pickup code is visible only to the requesting student. Financial circumstances will not be collected or displayed. Consenting participants and synthetic data will be used for evaluation where appropriate. Dependency licenses will be recorded.'),
 p('The demonstration uses plain HTTP inside a local network; a public deployment would require HTTPS and is outside the scope. Maintenance will cover defect reporting, diagnosis, repair, regression testing and release notes. A backup/restore guide will be delivered. The platform will coordinate exchanges; disputes and damaged or lost items will remain subject to club policy.')
];
const delivery=[h('11. Feasibility, Risks and Sustainability'),
 p('The scope is designed for a small student team over approximately ten weeks, subject to the actual course schedule. Development uses open-source tools (Flutter SDK, JDK 17, Android SDK command-line tools) and a local demonstration environment; Android Studio and an emulator are not required. Hosting, e-mail service and club participation will require confirmation before any public pilot. A participating club is the proposed operational owner; its agreement has not yet been obtained.'),
 table(['Risk','Mitigation'],[
 ['Limited participation','Validate demand early and use consented volunteers or clearly labelled synthetic data for demonstration.'],
 ['Lost items or delayed returns','Record condition and due dates, show overdue loans and provide coordinator review under club policy.'],
 ['Conflicting allocations / data loss','Use transactional allocation, concurrency tests, immediate disk writes and file backups.'],
 ['Phone cannot reach the backend','Document network and firewall setup; the server address can be changed inside the app.'],
 ['Scope or schedule overrun','Prioritize the full core workflow and defer optional integrations.']
 ],[2800,6560]),
 h('12. Tentative Work Plan'),
 table(['Week','Activity and deliverable'],[
 ['1–2','Stakeholder interviews, feasibility review, scope definition and Software Requirements Specification (SRS).'],
 ['3–4','Use-case, class and sequence diagrams; ERD, screen prototype and pattern/design rationale.'],
 ['5–7','Implement backend API and the Flutter app: authentication, catalogue, requests, allocation, handover, returns and notifications.'],
 ['8–9','Execute test plan, repair defects, test on Android phones, conduct user validation and collect quality/impact observations.'],
 ['10','Prepare final report, user and maintenance guides, demonstration and project handover.']
 ],[1100,8260]),
 h('13. Expected Deliverables and Social Impact'),
 p('Deliverables will include the installable Android application (APK), backend and mobile source code, SRS, UML diagrams and ERD, design-pattern rationale, test cases and results, defect log, metrics summary, user manual, setup/backup instructions and final presentation.'),
 p('A proposed pilot of 20–30 consenting students, if feasible, will assess resource access and usability. Social impact will be reported using confirmed handovers, completed loans and donations and unique recipients. Potential benefits include easier access to study materials and increased reuse; actual results will depend on participation and will not be assumed in advance.'),
 h('14. Curriculum Reference'),
 p('Bangladesh University of Business and Technology (BUBT). Software Engineering (CSE 327), 3.00 credits; prerequisite: CSE 317. Curriculum excerpt supplied for this proposal. The project plan maps to its design patterns, testing, quality, metrics, architecture, risk, delivery and maintenance topics.'),
 p('',{children:[new TextRun({text:'https://classic.bubt.edu.bd/department/curriculam/51#curriculum_165',size:18,color:gray})]})
];
const doc = new Document({creator:'',title:'ShikkhaSetu — CSE 327 Project Proposal (v2, mobile)',description:'BUBT Software Engineering course project proposal, revised for an Android mobile app',styles:{default:{document:{run:{font:'Calibri',size:22,color:'24313C'},paragraph:{spacing:{after:110,line:270}}}},paragraphStyles:[{id:'Heading1',name:'Heading 1',basedOn:'Normal',next:'Normal',quickFormat:true,run:{font:'Calibri',size:27,bold:true,color:blue}},{id:'Heading2',name:'Heading 2',basedOn:'Normal',next:'Normal',quickFormat:true,run:{font:'Calibri',size:23,bold:true,color:blue}}]},sections:[cover,changes,overview,design,quality,delivery].map(page)});
Packer.toBuffer(doc).then(b=>{fs.writeFileSync(out+'.docx',b);console.log(out+'.docx generated');});
