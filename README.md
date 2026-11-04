# SubastIX

Aplicación web JSP para Tomcat 11, Java 17+ y MySQL 8. El backend usa Jakarta Servlet, JDBC/HikariCP y el modelo relacional `subastix_db`.

## Preparar MySQL

Ejecuta `src/main/resources/db/schema.sql` una vez. El script crea la base y tablas faltantes sin borrar ni recrear tablas existentes, y registra las categorías de ejemplo.

La aplicación espera que exista un usuario MySQL con permisos sobre `subastix_db`. Configura estas variables en el entorno de Tomcat:

- `DB_URL` (opcional; predeterminado: `jdbc:mysql://localhost:3306/subastix_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC`)
- `DB_USER` (opcional; predeterminado: `subastix`)
- `DB_PASSWORD` (configura la contraseña fuera del código)
- `UPLOAD_DIR` (opcional; predeterminado: `${catalina.base}/subastix-uploads`)

El registro crea hashes PBKDF2. El inicio de sesión también acepta hashes BCrypt existentes para facilitar la integración con usuarios de tu esquema. Los hashes abreviados de ejemplo (`...hash`) no son válidos; registra una cuenta nueva si esos marcadores están en tu base.

## Compilar y desplegar

Con Maven 3.9+, ejecuta `mvn clean package`. Copia `target/SubastIX.war` a `CATALINA_BASE/webapps` y arranca Tomcat. El driver MySQL, HikariCP y Jackson se empaquetan en el WAR; la API Servlet la proporciona Tomcat.

Registra una cuenta de vendedor para publicar subastas. Compradores y vendedores registrados pueden participar en pujas. Las pujas se guardan en una transacción que bloquea la subasta, vuelve a comprobar el mínimo y aplica la extensión de tiempo en los últimos 60 segundos.

## Alcance actual

El saldo del esquema se conserva, pero esta versión no procesa pagos, no reserva fondos ni recarga billeteras. Las notificaciones y el estado de pago tampoco están conectados a servicios externos. La imagen de cada producto se guarda fuera del directorio público y se sirve mediante una ruta con nombre generado por el servidor.
