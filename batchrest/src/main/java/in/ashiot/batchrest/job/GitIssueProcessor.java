package in.ashiot.batchrest.job;

import in.ashiot.batchrest.GitIssue;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.inject.Named;

@Named("issueProcessor")
public class GitIssueProcessor implements ItemProcessor{
        
    @Override
    public Object processItem(Object item) throws Exception {
//        System.out.println("Inside processor");
        GitIssue gitIssue = (GitIssue) item;
        String assignee;
        gitIssue.setAssignee((assignee = gitIssue.getAssignee()) == null ? "No assignee" :  assignee);
        return gitIssue;
    }  
}
