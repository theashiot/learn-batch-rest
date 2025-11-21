package in.ashiot.batchrest.job;

import java.io.Serializable;

public class GitIssueCheckPoint implements Serializable {
    private int noOfIssues = 0;
    public void increment() {
        noOfIssues++;
    }
    public int getNoOfIssues() {
        return noOfIssues;
    }
}
