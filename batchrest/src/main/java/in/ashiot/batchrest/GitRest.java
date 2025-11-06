package in.ashiot.batchrest;


import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class GitRest {

    private static final String GITHUB_API_BASE = 
        "https://api.github.com/repos/wildfly/wildfly-core";
    private static final String GITHUB_API_VERSION_HEADER = "2022-11-28";

    private static final Client CLIENT = ClientBuilder.newClient();
    
    public GitRest() {

    }

    public String getIssues() {

        WebTarget webTarget = CLIENT.target(GITHUB_API_BASE);
        WebTarget issuesTarget = webTarget.path("issues").queryParam("state", "open");

        String issuesString;
        try (Response response = issuesTarget.request(MediaType.APPLICATION_JSON)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION_HEADER)
                .get()) {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        StringBuilder sb = new StringBuilder("--- GitHub API Response ---\n");
                        sb.append(response.readEntity(String.class));
                        issuesString = sb.toString();
                    }
                    else {
                        issuesString = response.readEntity(String.class);
                    }
                } finally {
                    CLIENT.close();
                }

        return issuesString;
    }
}
