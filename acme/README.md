# SICA Desktop Core

The Java 17 core uses JDBC through HikariCP and connects to the existing MySQL
database `sica_db` on `localhost:3307`. It does not initialize, migrate, or
recreate the schema.

Configure the connection password outside the repository:

```bash
export SICA_DB_PASSWORD='your-database-password'
mvn -f acme/pom.xml clean test
```

The URL and username default to the SICA Docker values. They can be overridden
with `SICA_DB_URL` and `SICA_DB_USERNAME`, or with the corresponding
`sica.db.url` and `sica.db.username` Java system properties. The password also
supports `-Dsica.db.password=...`.

Do not run `database/schema.sql` against an existing installation: it starts
with `DROP DATABASE IF EXISTS sica_db`. The Swing interface and removal of the
legacy static web assets are part of Phase 3.
