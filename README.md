# learn-batch-rest

To launch the application, do this from the "batchrest" directory:

- Compile the application: `mvn package -Pprovisioned-server`
- Start the packaged server: `mvn wildfly:start`
- Access the application at http://localhost:8080/batchrest-1.0-SNAPSHOT/report

A Batch job also starts in the background that writes the report to an html file called report.html.
- Access the file at target/server/standalone/tmp/report.html.

- In the end, shutdown the server: `mvn wildfly:shutdown`

