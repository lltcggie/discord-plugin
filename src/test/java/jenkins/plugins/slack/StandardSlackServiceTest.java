package jenkins.plugins.slack;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StandardSlackServiceTest {
    /**
     * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
     */
    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardSlackService service = new StandardSlackService("foo", "token", null, false);
        service.setHost("hostvaluethatwillcausepublishtofail");
        service.publish("message");
    }

    /**
     * Use a valid host, but an invalid team domain
     */
    @Test
    public void invalidWebhookIdShouldFail() {
        StandardSlackService service = new StandardSlackService("my", "token", null, false);
        service.publish("message");
    }

    /**
     * Use a valid team domain, but a bad token
     */
    @Test
    public void invalidTokenShouldFail() {
        StandardSlackService service = new StandardSlackService("tinyspeck", "token", null, false);
        service.publish("message");
    }
}
