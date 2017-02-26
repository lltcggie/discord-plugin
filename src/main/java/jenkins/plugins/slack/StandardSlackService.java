package jenkins.plugins.slack;

import hudson.security.ACL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

import org.apache.commons.lang.StringUtils;

public class StandardSlackService implements SlackService {

    private static final Logger logger = Logger.getLogger(StandardSlackService.class.getName());

    private String host = "discordapp.com";
    private String webhookId;
    private String token;
    private String authTokenCredentialId;
    private boolean botUser;
    private String[] roomIds;

    public StandardSlackService(String webhookId, String token, String authTokenCredentialId, boolean botUser, String roomId) {
        super();
        this.webhookId = webhookId;
        this.token = token;
        this.authTokenCredentialId = StringUtils.trim(authTokenCredentialId);
        this.botUser = botUser;
        this.roomIds = roomId.split("[,; ]+");
    }

    public boolean publish(String message) {
        return publish(message, "warning");
    }

    public boolean publish(String message, String color) {
        boolean result = true;
        for (String roomId : roomIds) {
            //prepare attachments first
            //JSONObject field = new JSONObject();
            //field.put("short", false);
            //field.put("value", message);

            //JSONArray fields = new JSONArray();
            //fields.put(field);

            if (color == "good") {
                color = "#36a64f";
            } else if (color == "warning") {
                color = "#daa038";
            } else if (color == "danger") {
                color = "#d00000";
            } else {
                color = "#e8e8e8";
            }

            JSONObject attachment = new JSONObject();
            attachment.put("fallback", message);
            attachment.put("color", color);
            //attachment.put("fields", fields);
            //JSONArray mrkdwn = new JSONArray();
            //mrkdwn.put("pretext");
            //mrkdwn.put("text");
            //mrkdwn.put("fields");
            //attachment.put("mrkdwn_in", mrkdwn);
            attachment.put("text", message);
            JSONArray attachments = new JSONArray();
            attachments.put(attachment);

            PostMethod post;
            String url;
            //prepare post methods for both requests types
            if (!botUser) {
                // url = "https://" + webhookId + "." + host + "/services/hooks/jenkins-ci?token=" + getTokenToUse();
                url = "https://discordapp.com/api/webhooks/" + webhookId + "/" + getTokenToUse() + "/slack?wait=true";
                post = new PostMethod(url);
                JSONObject json = new JSONObject();

                //json.put("channel", roomId);
                json.put("attachments", attachments);
                //json.put("link_names", "1");
                
                //json.put("text", message);

                post.addParameter("payload", json.toString());

            } else {
                logger.info("mitaiou");
                /*
                url = "https://slack.com/api/chat.postMessage?token=" + getTokenToUse() +
                        "&channel=" + roomId +
                        "&link_names=1" +
                        "&as_user=true";
                try {
                    url += "&attachments=" + URLEncoder.encode(attachments.toString(), "utf-8");
                } catch (UnsupportedEncodingException e) {
                    logger.log(Level.ALL, "Error while encoding attachments: " + e.getMessage());
                }
                post = new PostMethod(url);
                */
                url = "https://discordapp.com/api/webhooks/" + webhookId + "/" + getTokenToUse() + "/slack";
                post = new PostMethod(url);
                JSONObject json = new JSONObject();

                json.put("attachments", attachments);

                post.addParameter("payload", json.toString());

            }
            logger.fine("Posting: to " + roomId + " on " + webhookId + " using " + url + ": " + message + " " + color);
            HttpClient client = getHttpClient();
            post.getParams().setContentCharset("UTF-8");
            post.getParams().setParameter("http.useragent", "discord webhook");

            try {
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if (responseCode != HttpStatus.SC_OK) {
                    logger.log(Level.WARNING, "Slack post may have failed. Response: " + response);
                    result = false;
                } else {
                    logger.info("Posting succeeded");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to Slack", e);
                result = false;
            } finally {
                post.releaseConnection();
            }
        }
        return result;
    }

    private String getTokenToUse() {
        if (authTokenCredentialId != null && !authTokenCredentialId.isEmpty()) {
            StringCredentials credentials = lookupCredentials(authTokenCredentialId);
            if (credentials != null) {
                logger.fine("Using Integration Token Credential ID.");
                return credentials.getSecret().getPlainText();
            }
        }

        logger.fine("Using Integration Token.");

        return token;
    }

    private StringCredentials lookupCredentials(String credentialId) {
        List<StringCredentials> credentials = CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId(credentialId);
        return CredentialsMatchers.firstOrNull(credentials, matcher);
    }

    protected HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    // http://hc.apache.org/httpclient-3.x/authentication.html#Proxy_Authentication
                    // and
                    // http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/examples/BasicAuthenticationExample.java?view=markup
                    client.getState().setProxyCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }

    void setHost(String host) {
        this.host = host;
    }
}
