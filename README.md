# RuleWeaver — Dynamic Rule Engine & Management System

**Repository:** `DBMS_OOPS_PROJECT`  
**Root Package:** `com.ruleweaver`  
**Tech Stack:** Java 17+, Maven, MongoDB, JavaFX, JUnit 5  

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

---

## 🏗️ System Architecture

The project strictly follows standard Maven directory layout and Java package hierarchy:

```text
com.ruleweaver
├── config/        # MongoDB client instantiation and configuration settings
├── repository/    # Data Access Objects (DAO) handling CRUD operations on MongoDB
├── model/         # Core business domain entities (Applicant, Policy, Condition, etc.)
├── service/       # Business logic operations, rule processing, and domain services
└── Main.java      # Application bootstrap & smoke-testing entry point
