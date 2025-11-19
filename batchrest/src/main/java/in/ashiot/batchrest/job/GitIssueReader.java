package in.ashiot.batchrest.job;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import in.ashiot.batchrest.GitIssue;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Named("issueReader")
public class GitIssueReader extends AbstractItemReader {

    @Inject
    JobContext jobContext;
    
    private int countOfIssues = 0;
    private static final String GITHUB_API_BASE = "https://api.github.com/repos/wildfly/wildfly-core";
        //"https://api.github.com/repos/infinispan/infinispan";    
    private static final String GITHUB_API_VERSION_HEADER = "2022-11-28";
    private static final Client CLIENT = ClientBuilder.newClient();
    private StringBuilder responseSB = new StringBuilder();
    private String responseString;
    private List <GitIssue> listOfGitIssues = new ArrayList<GitIssue>();
    private int count = 0;

    /*
     * Initialize a Rest client and get the list of GH Issues
     */

    @Override
    public void open(Serializable previousCheckpoint) throws Exception {        

        WebTarget webTarget = CLIENT.target(GITHUB_API_BASE);
        WebTarget issuesTarget = webTarget.path("issues").queryParam("state", "open");
        Response response = issuesTarget.request(MediaType.APPLICATION_JSON)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION_HEADER)
                .get();
        
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            responseSB.append(response.readEntity(String.class));            
            responseString = responseSB.toString();
        }
        else {
            responseString = response.readEntity(String.class);
        }
        
        GitIssue gitIssue; 
        try (JsonReader reader = Json.createReader(new StringReader(this.responseString))) {
            JsonArray jsonArray = reader.readArray();
            for (JsonValue value : jsonArray) {
                if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                    gitIssue = new GitIssue();
                    JsonObject jsonObject = value.asJsonObject();
                    printKeysAndValues(jsonObject, "",gitIssue);
//                    System.out.println("Creator is "+gitIssue.getCreator());
                    listOfGitIssues.add(gitIssue);
                }
//                System.out.println("Creator at "+this.countOfIssues+" is "+listOfGitIssues.get(this.countOfIssues).getCreator());
                this.countOfIssues++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }    
    
    }


    @Override
    public GitIssue readItem() throws Exception {
        if (count < countOfIssues) {
            GitIssue issue = listOfGitIssues.get(count);
            count++;
            return issue;
        }

        else {
            return null;
        }

    }

    @Override
    public void close() {
        //CLIENT.close();
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
                    printKeysAndValues(value.asJsonObject(), fullKey, gitIssue);
                    break;
                
                case ARRAY:
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
}
