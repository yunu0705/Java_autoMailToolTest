package com.hareotokoassociation.certificationapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hareotokoassociation.certificationapp.model.Applicant;
import com.hareotokoassociation.certificationapp.model.Certification;
import com.hareotokoassociation.certificationapp.service.CertificationService;
import com.hareotokoassociation.certificationapp.service.CertificationService.CertificationResult;
import com.hareotokoassociation.certificationapp.service.EmailService;
import com.hareotokoassociation.certificationapp.service.FileImportService;
import com.hareotokoassociation.certificationapp.service.GmailService;
import com.opencsv.CSVWriter;

@SpringBootApplication
public class CertificationAppApplication implements CommandLineRunner {

    @Autowired
    private CertificationService certificationService;

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GmailService gmailService;

    @Value("${app.mode:normal}")
    private String appMode;

    public static void main(String[] args) {
        SpringApplication.run(CertificationAppApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if ("test".equalsIgnoreCase(appMode)) {
            return;
        }

        JOptionPane.showMessageDialog(null, "\uD83C\uDF1E 起動成功しました！アプリを開始します。");

        if (!gmailService.isAuthorized()) {
            JOptionPane.showMessageDialog(null, "Gmail認証が必要です。ブラウザでログインしてください。");
            gmailService.authorize();
            JOptionPane.showMessageDialog(null, "認証が完了しました！");
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("CSVファイルを選択してください（新規認定者）");
        int result = chooser.showOpenDialog(null);

        if (result != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(null, "⚠️ ファイルが選択されませんでした。アプリを終了します。");
            return;
        }

        String filePath = chooser.getSelectedFile().getAbsolutePath();
        File file = new File(filePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(null, "❌ ファイルが見つかりません: " + filePath);
            return;
        }

        try {
            List<Applicant> applicants = fileImportService.readApplicantsFromFile(filePath);
            JOptionPane.showMessageDialog(null, applicants.size() + "件の申請者データを読み込みました（60点以上のみ）");

            int processedCount = 0;
            List<CertificationResult> results = new ArrayList<>();

            for (Applicant applicant : applicants) {
                CertificationResult certResult = certificationService.processApplicant(applicant);
                results.add(certResult);
                processedCount++;
            }

            JOptionPane.showMessageDialog(null, "処理データ: " + processedCount + "件");

            // 全認定データをエクスポート（既存＋新規まとめて）
            List<Certification> allCertifications = certificationService.getAllCertificationsForVoting();
            String allPath = "certification_all_list.csv";
            exportCertificationsToCSV(allCertifications, allPath);
            JOptionPane.showMessageDialog(null, "全認定者リストをエクスポートしました: " + allPath);

            int confirm = JOptionPane.showConfirmDialog(null, "認定メールを送信しますか？", "メール送信", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(null, "認定メールを送信中...");
                long successCount = 0;

                for (CertificationResult mailResult : results) {
                    Certification cert = mailResult.getCertification();
                    String email = cert.getApplicant().getEmail();
                    String name = cert.getApplicant().getName();
                    boolean isResend = mailResult.isResend();
                    try {
                        gmailService.sendCertificationEmail(email, name, cert.getCertificationNumber(), isResend);
                        successCount++;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "❌ メール送信失敗: " + email + " → " + e.getMessage());
                    }
                }
                JOptionPane.showMessageDialog(null, "メール送信結果: " + successCount + "/" + results.size() + "件送信成功");
            } else {
                JOptionPane.showMessageDialog(null, "メール送信はキャンセルされました。アプリを終了します。");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "💥 エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }

        JOptionPane.showMessageDialog(null, "✅ 処理が完了しました。アプリを終了します。", "完了", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportCertificationsToCSV(List<Certification> certifications, String filePath) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8)) {

            // UTF-8 BOM を出力（Windows Excelでの文字化け防止）
            writer.write('\uFEFF');

            CSVWriter csvWriter = new CSVWriter(writer);
            String[] header = {"認定番号", "名前", "メールアドレス", "都道府県", "市区町村", "認定日"};
            csvWriter.writeNext(header);

            for (Certification cert : certifications) {
                String certNum = cert.getCertificationNumber();
                if (certNum == null || certNum.isBlank()) {
                    certNum = certificationService.generateCertificationNumber();
                    cert.setCertificationNumber(certNum);
                    certificationService.saveCertification(cert); // 保存して反映
                }
                String[] row = {
                    certNum,
                    cert.getApplicant().getName(),
                    cert.getApplicant().getEmail(),
                    cert.getApplicant().getPrefecture(),
                    cert.getApplicant().getCity(),
                    cert.getIssueDate().toString()
                };
                csvWriter.writeNext(row);
            }

            csvWriter.close();
        }
    }
}
