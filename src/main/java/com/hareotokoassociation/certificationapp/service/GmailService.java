package com.hareotokoassociation.certificationapp.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

@Service
public class GmailService {
    private static final Logger LOGGER = Logger.getLogger(GmailService.class.getName());

    private static final String APPLICATION_NAME = "晴れ男・晴れ女認定システム";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private Gmail service;
    private boolean isAuthorized = false;

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void authorize() throws IOException {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(
                    Optional.ofNullable(GmailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH))
                        .orElseThrow(() -> new IllegalStateException("Credentials file not found: " + CREDENTIALS_FILE_PATH))
                ));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            isAuthorized = true;

            LOGGER.info("Gmail API authorization successful");
        } catch (GeneralSecurityException e) {
            LOGGER.log(Level.SEVERE, "Authorization failed", e);
            throw new IOException("Authorization failed", e);
        }
    }

    public void sendHtmlEmail(String to, String name, String subject, String htmlBody)
            throws MessagingException, IOException {
        validateEmailParameters(to, subject, htmlBody);

        if (!isAuthorized) {
            try {
                authorize();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to authorize before sending email", e);
                throw new MessagingException("Failed to authorize", e);
            }
        }

        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);

            email.setFrom(new InternetAddress("me"));
            email.addRecipient(jakarta.mail.Message.RecipientType.TO,
                    new InternetAddress(to, name, "UTF-8"));
            email.setSubject(subject, "UTF-8");
            email.setContent(htmlBody, "text/html; charset=utf-8");

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] bytes = buffer.toByteArray();
            String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);

            Message message = new Message();
            message.setRaw(encodedEmail);

            service.users().messages().send("me", message).execute();

            LOGGER.info("Email sent successfully to: " + to);
        } catch (GoogleJsonResponseException e) {
            LOGGER.log(Level.SEVERE, "Gmail API error: " + e.getDetails(), e);
            throw new MessagingException("Failed to send email via Gmail API", e);
        }
    }

    public void sendCertificationEmail(String to, String name, String certificationNumber, boolean isResend) throws MessagingException, IOException {
        String subject = "【晴れ男・晴れ女認定】合格通知";
        String htmlBody;

        if (isResend) {
            htmlBody = String.format("""
                <p>%s 殿</p>
                <p>以前に合格をされていましたので、再送しました。</p>
                <p><strong>認定番号は <span style='font-size: 24px;'>%s</span> です。</strong></p>
                <p>今後ともどうぞよろしくお願いします。</p>
                <p>事務局</p>
                """, name, certificationNumber);
        } else {
            htmlBody = String.format("""
                <p>%s 殿</p>
                <p>このたびは一般社団法人全日本晴れ男・晴れ女協会の<br>
                「晴れ男・晴れ女検定」に受験くださり誠にありがとうございます。</p>

                <p>採点の結果、貴殿は本協会の基準に達しましたので<br>
                本協会公認の「晴れ男・晴れ女」として認めます。</p>

                <p>今後、晴れを呼ぶ「人」として<br>
                世の中を明るく照らす太陽になってください。</p>

                <p><strong>認定番号は <span style='font-size: 24px;'>%s</span> になります。</strong></p>

                <p>認定証の発行が希望の方は<br>
                下記から認定証の発行を行ってください。</p>

                <p>※認定証を発行していただくとデータベースに登録を行いますので、<br>
                今後、履歴書にご記入できるようになります。</p>

                <p><a href='https://hareotoko725.official.ec/items/60994324'>認定証はこちら</a></p>
                <p><img src='https://i.imgur.com/vCyWGxo.jpeg' alt='認定証サンプル' width='300' height='300'></p>

                <p>またゴールド認定カードが　500円（税別）で発行できますので、ご希望であれば下記からお申し込みください。</p>
                <p><a href='https://hareotoko725.official.ec/items/14579123'>https://hareotoko725.official.ec/items/14579123</a></p>

                <p>認定証代わりとなるステッカーもございますので、ご希望であれば下記からお申し込みください。</p>
                <p><a href='https://hareotoko725.official.ec/items/24147889'>https://hareotoko725.official.ec/items/24147889</a></p>

                <p>改めまして合格おめでとうございます！</p>
                <p>事務局</p>
                """, name, certificationNumber);
        }

        sendHtmlEmail(to, name, subject, htmlBody);
    }

    public void validateEmailParameters(String to, String subject, String bodyText) {
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email cannot be empty");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject cannot be empty");
        }
        if (bodyText == null || bodyText.trim().isEmpty()) {
            throw new IllegalArgumentException("Email body cannot be empty");
        }
        if (!isValidEmailAddress(to)) {
            throw new IllegalArgumentException("Invalid email address: " + to);
        }
    }

    private boolean isValidEmailAddress(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static class EmailResult {
        private final String recipient;
        private final boolean success;
        private final String errorMessage;

        public EmailResult(String recipient, boolean success) {
            this(recipient, success, null);
        }

        public EmailResult(String recipient, boolean success, String errorMessage) {
            this.recipient = recipient;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getRecipient() {
            return recipient;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
