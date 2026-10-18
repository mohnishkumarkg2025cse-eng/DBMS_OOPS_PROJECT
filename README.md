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
## Bidirectional Database Synchronization

RuleWeaver X supports synchronization of applicant data between MongoDB Atlas and MySQL.

### Synchronization flow

- **MongoDB → MySQL:** MongoDB Change Streams detect database changes and propagate applicant updates to MySQL.
- **MySQL → MongoDB:** MySQL outbox events are processed by a background worker, which applies corresponding changes to MongoDB.
- **Loop prevention:** Synchronization-origin metadata helps prevent changes written by one synchronization direction from being repeatedly propagated back.
- **Deletion handling:** Deletions are represented using soft-delete metadata so that deleted applicants can be excluded from normal application listings.

### Configuration

Configure the MongoDB connection using the project's supported configuration mechanism. Configure MySQL credentials through environment variables:

```powershell
$env:MYSQL_USER = "your_mysql_username"
$env:MYSQL_PASSWORD = "your_mysql_password"
```

Start the application with:

```powershell
mvn javafx:run
```

Set the credentials in the same PowerShell session used to launch the application. Do not commit database passwords, connection secrets, or local configuration files.

### Verification

Run the automated test suite with:

```powershell
mvn clean test
```

Verify database synchronization separately by creating, updating, and deleting a test applicant, then checking the corresponding records in both databases.

### Current limitations

Synchronization reliability improvements, including more robust retry and recovery handling and protection against concurrent worker execution, remain planned work.


## 🏗️ System Architecture

### Project Structure

The project follows the standard Maven directory layout and organizes Java classes into packages based on their responsibilities.

```text
src/
└── main/
    ├── java/
    │   └── com/
    │       └── ruleweaver/
    │           ├── config/       # MongoDB and MySQL configuration
    │           ├── model/        # Domain entities (Applicant, Policy, etc.)
    │           ├── repository/   # Data access and persistence operations
    │           ├── service/      # Business logic and domain services
    │           ├── sync/         # Bidirectional MongoDB–MySQL synchronization
    │           ├── engine/       # Rule evaluation and decision logic
    │           ├── ui/           # JavaFX interface and application context
    │           └── Main.java     # Application entry point (if located here)
    └── test/
        └── java/                 # Automated tests
```

**Architecture overview**

- **Config:** Database connections and application configuration.
- **Model:** Core domain objects such as applicants, policies, and evaluation results.
- **Repository:** Database access and persistence operations.
- **Service:** Business operations and domain-level workflows.
- **Sync:** Background workers and change listeners that synchronize applicant data between MongoDB Atlas and MySQL.
- **Engine:** Policy evaluation and decision-making logic.
- **UI:** JavaFX views, navigation, and application interaction.
- **Tests:** Automated checks for core logic and application behavior.

