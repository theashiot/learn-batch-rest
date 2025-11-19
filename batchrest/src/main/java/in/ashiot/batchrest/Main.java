package in.ashiot.batchrest;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.JobExecution;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;


/**
 *
 * <p>
 * The servlet is registered and mapped to /HelloServlet using the {@linkplain WebServlet
 */
@WebServlet("/report")
public class Main extends HttpServlet {

    private static final Client CLIENT = ClientBuilder.newClient();
    static String PAGE_HEADER = "<html><head><title>GitHub Report</title></head><body>";
    static String PAGE_FOOTER = "</body></html>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
             
        
        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.println(PAGE_HEADER);
        
        GitRest gitRest = new GitRest();

        try {
                String msg = gitRest.getIssues(CLIENT);
                IssueController isc = new IssueController();
                isc.inferIssuesFromGHResponse(msg);                
                writer.println("<h1>Report of the wildfly-core repository</h1>");
                int noOfIssues = isc.getIssueCount();
                writer.println("<p>Found "+noOfIssues+" issues.</p>");
                List <GitIssue> issues;
                
                issues = isc.getGitIssueList();

                writer.println("<table border= \"1\" style=\"width: 100%;\"> "+
                               "<tr>" +
                               "<th>Issue ID</th>" +
                               "<th>Title</th>" +
                               "<th>Creator</th>" +
                               "<th>Last Update</th>" +
                               "</tr>");
                for (GitIssue issue : issues) {
                    writer.println("<tr>");
                    writer.println("<td>"+ issue.getIssueID()+"</td>");
                    writer.println("<td>"+ issue.getTitle()+"</td>");
                    writer.println("<td>"+ issue.getCreator()+"</td>");
                    writer.println("<td>"+ issue.getLastUpdate()+"</td>");
                    writer.println("</tr>");
                }
                writer.println("</table>");
                
                writer.println("<p>Starting a job <br />");
                writer.println("Job status: "+ doJob() +"</p>");
                writer.println(PAGE_FOOTER);
                writer.close();
        } finally {
        }

    }

    public void destroy () {
        CLIENT.close();
    }

    private String doJob() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParameters = new Properties();
        Long executionId = jobOperator.start("hello-job", jobParameters);
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        return jobExecution.getBatchStatus().toString();
    }

}