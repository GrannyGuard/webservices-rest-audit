# OpenMRS REST Web Services Module – Security Audit

## Software Security & Compliance

This repository is used as part of the **Software Security & Compliance** course. The objective of this project is to perform a security and compliance audit on the OpenMRS REST Web Services Module.

Software security and compliance focus on protecting software against misuse, vulnerabilities, and data breaches while ensuring compliance with applicable laws, regulations, and industry standards. Since healthcare systems often process sensitive personal and medical information, security plays a critical role in maintaining confidentiality, integrity, and availability.

During this project, we analyze the OpenMRS REST API, identify potential security risks, perform security testing, and evaluate compliance with relevant legislation such as the General Data Protection Regulation (GDPR).

---

## Project Objectives

The main objectives of this audit are to:

- Analyze the architecture of the application
- Identify potential security vulnerabilities
- Perform security testing on the application
- Evaluate legal and compliance requirements
- Assess privacy and data protection measures
- Review the software development and deployment process
- Propose security and compliance improvements

---

## About OpenMRS

OpenMRS is an open-source Electronic Medical Record (EMR) platform used worldwide to support healthcare delivery, particularly in resource-constrained environments.

- Official Website: https://openmrs.org
- Original Repository: https://github.com/openmrs/openmrs-module-webservices.rest

The REST Web Services Module exposes OpenMRS functionality through RESTful APIs, allowing external applications to interact with OpenMRS resources.

---

## Audit Scope

The security assessment focuses on the following areas:

### Authentication & Authorization

- User authentication mechanisms
- Access control implementation
- Privilege management

### API Security

- REST endpoint security
- Input validation
- Output encoding
- Error handling

### Dependency Security

- Vulnerable third-party libraries
- Dependency management practices
- Supply chain risks

### Application Security

- OWASP Top 10 vulnerabilities
- Secure coding practices
- Security misconfigurations

### Privacy & Compliance

- GDPR considerations
- Personal data handling
- Data minimization principles
- Audit logging requirements

### DevSecOps & CI/CD

- Build pipeline security
- Dependency scanning
- Static Application Security Testing (SAST)
- Security automation opportunities

---

## Audit Methodology

The audit consists of multiple phases:

1. Repository and architecture review
2. Threat modeling
3. Static code analysis
4. Dependency vulnerability assessment
5. Security testing
6. Compliance assessment
7. Reporting and recommendations

---

## Technologies

The project is primarily built using:

- Java
- Maven
- REST APIs
- JUnit
- OpenMRS Framework

---

## Deliverables

The final audit includes:

- Security findings
- Risk assessment
- Compliance analysis
- Vulnerability overview
- Recommendations for remediation
- Final audit report

---

## Environments (OTAP) & CI/CD Gates

This repository uses [GitHub Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment) to separate configuration and to gate any deployment-style automation behind approvals, in line with NEN-7510 requirements on segregating development, test and production.

| Environment | Purpose | Protection rules |
|---|---|---|
| `dev` | Ontwikkeling — day-to-day integration | none (open) |
| `test` | Testing — pre-acceptance verification | Required reviewers (`DanielvG-IT`, `BaasW`) + deployments only allowed from protected branches |
| `prod` | Productie — live module | Required reviewers (`DanielvG-IT`, `BaasW`) + deployments only allowed from protected branches |
| `copilot` | Sandbox for the GitHub Copilot coding agent | none |

`test` and `prod` both require sign-off from at least one of two designated reviewers before a deployment to that environment can proceed, and both restrict deployments to protected branches — this is the approval-gate mechanism requested by the assignment. Secrets are scoped per environment (rather than shared at repository level) so that, e.g., a production database credential is never available to a job running against `dev` or `test`.

### Preventing test data from reaching production

OpenMRS ships with a [standard demo dataset](https://wiki.openmrs.org/display/docs/Demo+Data) consisting entirely of synthetic patients and encounters. Development and test environments are seeded exclusively from this synthetic data — no real patient records are ever exported, copied, or seeded from `prod` into `dev`/`test`. Because the data flow is one-directional (synthetic → dev/test, never prod → dev/test) and `prod` is the only environment containing potentially sensitive data, there is no pipeline step that could leak real records downward.

### Working with the environments as a new developer

No access to the `dev`/`test`/`prod` GitHub Environments (or their secrets) is needed to work on this module day to day — they exist purely to gate CI/CD automation, not local development. To get started:

1. Clone the repository and build it locally with Maven (see [Running the Application](#running-the-application) below) — this uses only the synthetic OpenMRS demo data and requires no environment secrets.
2. Open a pull request against `main` as usual; the CodeQL, SBOM/SCA and SonarCloud workflows run automatically and report results without needing environment access.
3. Only repository maintainers can grant access to the protected environments (and their secrets), and only when a workflow actually needs to deploy to or act on `test`/`prod` — request this from a maintainer if your change requires it.

---

## Running the Application

Clone the repository:

```bash
git clone https://github.com/GrannyGuard/webservices-rest-audit.git
```

Build the project using Maven:

```bash
mvn clean install
```

Requirements:

- Java 8+
- Maven

---

## Disclaimer

This repository is a fork of the original OpenMRS REST Web Services Module and is used solely for educational purposes within the Software Security & Compliance course.

The original software, trademarks, and intellectual property belong to the OpenMRS community and contributors.

---

## Team

Security Audit Team

- Wouter Baas
- Daniël van Ginneken
- Wassim Balouda