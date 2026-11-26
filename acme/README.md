# SICA Desktop

Aplicación de escritorio Java 17 construida con Swing y FlatLaf. Se conecta
mediante JDBC/HikariCP a la base MySQL existente `sica_db` en
`localhost:3307`; no crea ni modifica el esquema.

Configure la contraseña fuera del repositorio y ejecute la aplicación desde
la raíz del workspace:

```bash
export SICA_DB_PASSWORD='your-database-password'
mvn -f acme/pom.xml -Dsica.db.password="$SICA_DB_PASSWORD" exec:java
```

La URL y el usuario predeterminados son los de SICA Docker. Se pueden cambiar
con `SICA_DB_URL` y `SICA_DB_USERNAME`, o con las propiedades
`sica.db.url` y `sica.db.username`. Para verificar compilación y pruebas:

```bash
mvn -f acme/pom.xml clean test
```

No ejecute `database/schema.sql` sobre una instalación existente: comienza
con `DROP DATABASE IF EXISTS sica_db`.
