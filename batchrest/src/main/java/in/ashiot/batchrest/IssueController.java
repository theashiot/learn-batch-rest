package in.ashiot.batchrest;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class IssueController {
    
    private List <GitIssue> ghIssuesList;
    private static int issueCount = 0;

    public IssueController() {
        ghIssuesList = new ArrayList <GitIssue>();
    }

    public void inferIssuesFromGHResponse(String ghResponse) {
        try (JsonReader reader = Json.createReader(new StringReader(ghResponse))) {
            JsonArray jsonArray = reader.readArray();
            int index = 0;
            GitIssue gitIssue = null;
            for (JsonValue value : jsonArray) {
                if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                    gitIssue = new GitIssue();
                    JsonObject jsonObject = value.asJsonObject();
                    printKeysAndValues(jsonObject, "",gitIssue);
                    ghIssuesList.add(gitIssue);
                }
                index++;
            }
            issueCount = index;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursively navigates a JsonObject and prints all keys and their values.
     *
     * @param obj      The JsonObject to traverse.
     * @param prefix   A prefix to show the nested path (e.g., "user.login")
     * @param gitIssue The gitIssue object to populate
     */
    public static void printKeysAndValues(JsonObject obj, String prefix, GitIssue gitIssue) {
        
        for (var entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonValue value = entry.getValue();
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            switch (value.getValueType()) {
                case OBJECT:
                    //System.out.println("Key: " + fullKey + " (Nested Object)");
                    printKeysAndValues(value.asJsonObject(), fullKey, gitIssue);
                    break;
                
                case ARRAY:
                    //System.out.println("Key: " + fullKey + ", Value: " + value);
                    break;

                default:                    
                    if (fullKey.equals("id")) {
                        gitIssue.setIssueID(value.toString());
                    }
                    if (fullKey.equals("user.login")) {
                        gitIssue.setCreator(value.toString());
                    }
                    if (fullKey.equals("assignee.login")) {
                    }
                    if (fullKey.equals("updated_at")) {
                        gitIssue.setLastUpdate(value.toString());
                    }
                    if (fullKey.equals("title")) {
                        gitIssue.setTitle(value.toString());
                    }
                    break;
            }
        }
    }

    public int getIssueCount() {
        return issueCount;
    }

    public List<GitIssue> getGitIssueList () {
        return this.ghIssuesList;
    }
}
