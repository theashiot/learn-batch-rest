# learn-batch-rest

To launch the application, do this from the "batchrest" directory:

- Compile the application: `mvn package -Pprovisioned-server`
- Start the packaged server: `mvn wildfly:start`
- Access the application at http://localhost:8080/batchrest-1.0-SNAPSHOT/report
- In the end, shutdown the server: `mvn wildfly:shutdown`

