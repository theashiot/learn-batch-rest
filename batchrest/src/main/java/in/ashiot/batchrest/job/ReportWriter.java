package in.ashiot.batchrest.job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.List;
import in.ashiot.batchrest.GitIssue;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.inject.Named;

@Named("reportWriter")
public class ReportWriter extends AbstractItemWriter {
    private File tempFile = new File(System.getProperty("jboss.server.temp.dir"), "report.html");
    private BufferedWriter bos;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        //System.out.println("File created at "+tempFile.getAbsolutePath());
        try {
            bos = new BufferedWriter(new FileWriter(tempFile, false));
            bos.write ("<html><body>" +
                        "<table border= \"1\" style=\"width: 100%;\"> "+
                               "<tr>" +
                               "<th>Issue ID</th>" +
                               "<th>Title</th>" +
                               "<th>Creator</th>" +
                               "<th>Assignee</th>" +
                               "<th>Last Update</th>" +
                               "</tr>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeItems(List<Object> items) throws Exception {
//        System.out.println("Called witeItems");
        for (int i = 0; i < items.size(); i++) {
            GitIssue gitIssue = (GitIssue) items.get(i);
            String entry = "<tr>" +
                           "<td>"+gitIssue.getIssueID()+"</td>\n" +
                           "<td>"+gitIssue.getTitle()+"</td>\n" +
                           "<td>"+gitIssue.getCreator()+"</td>\n" +
                           "<td>"+gitIssue.getAssignee()+"</td>\n" +
                           "<td>"+gitIssue.getLastUpdate()+"</td>\n" +
                           "<tr>\n"; 
            bos.append(entry);     
 //           System.out.println ("ID: "+gitIssue.getIssueID()+" Creator: "+gitIssue.getCreator()+" Assignee: "+gitIssue.getAssignee());

        }        
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return new GitIssueCheckPoint();
    }

    @Override
    public void close() throws Exception {
        bos.append("</table></body></html>");
        bos.close();
    }
    
}
