package in.ashiot.batchrest;

public class GitIssue {

    private String assignee;
    private String creator;
    private String issueID;
    private String lastUpdate;
    private String title;

    public String getAssignee() {
        return this.assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCreator() {
        return this.creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getIssueID() {
        return issueID;
    }

   public void setIssueID(String issueID) {
        this.issueID = issueID;
   }

    public String getLastUpdate() {
        return lastUpdate;
    }

   public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
   }
    public String getTitle() {
        return title;
    }

   public void setTitle(String title) {
        this.title = title;
   }
}
