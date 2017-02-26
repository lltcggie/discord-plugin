package jenkins.plugins.slack;

public class StandardSlackServiceStub extends StandardSlackService {

    private HttpClientStub httpClientStub;

    public StandardSlackServiceStub(String webhookId, String token, String tokenCredentialId, boolean botUser) {
        super(webhookId, token, tokenCredentialId, botUser);
    }

    @Override
    public HttpClientStub getHttpClient() {
        return httpClientStub;
    }

    public void setHttpClient(HttpClientStub httpClientStub) {
        this.httpClientStub = httpClientStub;
    }
}
