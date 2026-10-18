**Problem Statement: Lack of Transparency in Welfare and Opportunity Eligibility:**

Many students and individuals struggle to identify and access scholarships, internships, government welfare schemes, and other opportunities because eligibility criteria are complex, scattered across different sources, and difficult to understand. Manual verification can be time-consuming and inconsistent, potentially causing eligible applicants to miss valuable opportunities.
Proposed solution — RuleWeaver X: A configurable, explainable eligibility platform that evaluates applicants against clearly defined rules, provides transparent reasons for decisions, and maintains policy versions and evaluation history to support consistency and accountability.

**Social impact:**
- Improves access to scholarships and other opportunities.
- Reduces confusion about eligibility requirements.
- Makes decisions easier to understand and review.
- Helps organizations handle applications more consistently.**
  
# RuleWeaver — Dynamic Rule Engine & Management System

**Repository:** `DBMS_OOPS_PROJECT`  
**Root Package:** `com.ruleweaver`  
**Technology stack:**
| Layer                  | Technology                                       |
| ---------------------- | ------------------------------------------------ |
| UI                     | JavaFX 20.0.1,AtlantaFX            |
| Language               | Java 17                                          |
| Application Logic      | Decision Engine, Repositories, Policy Versioning |
| Database               | MongoDB Atlas                                    |
| Database Access        | MongoDB Java Driver                              |
| SQL Connectivity       | JDBC, MongoDB JDBC Driver 4.0.0                  |
| SQL Query Layer        | MongoDB Atlas SQL / Data Federation              |
| Build and Dependencies | Apache Maven                                     |
| Testing                | JUnit 5                                          | 

---

## 📖 About The Project

**RuleWeaver** is an Object-Oriented Database Application designed to automate complex eligibility assessment, rule management, and policy enforcement across dynamic datasets.

In traditional software systems, business rules (such as loan approvals, grant eligibility, or insurance qualification) are hardcoded into application logic. RuleWeaver solves this by decoupling business rules from code logic through a dynamic rule engine. Policies and evaluation conditions are stored dynamically in MongoDB and evaluated at runtime against incoming applicant data.

---

## ✨ Key Features

* **Dynamic Policy Engine:** Define, edit, and evaluate complex conditional logic without re-compiling or re-deploying application code.
* **NoSQL Persistence Layer:** Scalable document-based storage for flexible applicant structures, policy rules, and evaluation histories using MongoDB.
* **Modular Layered Architecture:** Strict separation of concerns between presentation (JavaFX), business logic (Services/Engine), domain models, and data access (Repositories).
* **Robust Domain Model:** Built using clean Object-Oriented Software Engineering principles (Encapsulation, Polymorphism, Abstraction, and Single Responsibility).
* **Automated Eligibility Processing:** Evaluates applicants against active policies and outputs pass/fail statuses with granular rule trace logs.
* **Transparent Decision Explanations:** Provides rule-level evaluation details to help users understand why an applicant passed or failed specific eligibility criteria.
* **Evaluation History:** Stores decision records for reviewing previous eligibility assessments and supporting traceability.

---

## 🏗️ System Architecture

The project strictly follows standard Maven directory layout and Java package hierarchy:

com.ruleweaver
├── config/        # MongoDB client instantiation and configuration settings
├── repository/    # Data Access Objects (DAO) handling CRUD operations on MongoDB
├── model/         # Core business domain entities (Applicant, Policy, Condition, etc.)
├── service/       # Business logic operations, rule processing, and domain services
└── Main.java      # Application bootstrap & smoke-testing entry point

