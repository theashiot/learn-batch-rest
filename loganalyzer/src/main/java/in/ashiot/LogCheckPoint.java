package in.ashiot;

import java.io.Serializable;

public class LogCheckPoint implements Serializable {
    Long noOfLines = (long) 0;

    public void increase() {
        noOfLines++;
    }

    public long getLineNumber() {
        return noOfLines;
    }
}
