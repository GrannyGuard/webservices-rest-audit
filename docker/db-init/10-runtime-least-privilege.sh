#!/bin/bash
# NEN-7510 A.8.2 (privileged access rights) / A.8.3 (least privilege).
# Hardening checklist §2 — "Least-privilege databasegebruiker (kan nooit DROP)".
#
# The openmrs/openmrs-core image connects with ONE account that both runs Liquibase
# (DDL) at startup AND serves runtime traffic. Leaving that account on the MariaDB
# image default (ALL PRIVILEGES on the database, incl. DROP/ALTER/GRANT) means an
# application-layer SQL injection or a compromised OpenMRS module could drop or alter
# the schema (see threat-model TM-E2 / audit finding SQ7). We therefore split the roles:
#
#   * ${MYSQL_USER}   — MIGRATION/ADMIN account (full privileges on the DB). Used only
#                       for schema creation and Liquibase upgrades at deploy time.
#   * ${DB_APP_USER}  — RUNTIME account, DML only (SELECT/INSERT/UPDATE/DELETE).
#                       It can NEVER run DDL: no DROP, ALTER, CREATE, GRANT, FILE.
#
# Steady-state production points OpenMRS at ${DB_APP_USER} (see docker/README.md).
# This script runs once, on first DB initialization (empty data volume).
set -euo pipefail

APP_USER="${DB_APP_USER:-openmrs_app}"
APP_PASSWORD="${DB_APP_PASSWORD:-change_me_app}"
DB="${MARIADB_DATABASE:-${MYSQL_DATABASE:-openmrs}}"
ROOT_PASSWORD="${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}"

mariadb --protocol=socket -uroot -p"${ROOT_PASSWORD}" <<-SQL
	CREATE USER IF NOT EXISTS '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASSWORD}';
	-- Explicitly DML-only. Granting individual statements (not ALL) guarantees the
	-- runtime account cannot DROP/ALTER/CREATE/GRANT even if the connection is abused.
	GRANT SELECT, INSERT, UPDATE, DELETE ON \`${DB}\`.* TO '${APP_USER}'@'%';
	FLUSH PRIVILEGES;
SQL

echo "[least-privilege] runtime DB user '${APP_USER}' created with DML-only grants on '${DB}'."
echo "[least-privilege] verify with:  SHOW GRANTS FOR '${APP_USER}'@'%';"
