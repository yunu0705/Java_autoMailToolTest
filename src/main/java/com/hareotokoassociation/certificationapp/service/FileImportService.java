package com.hareotokoassociation.certificationapp.service;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.hareotokoassociation.certificationapp.model.Applicant;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

@Service
public class FileImportService {

    // CSVファイルから "新規認定対象者のみ" を抽出
    public List<Applicant> readApplicantsFromFile(String filePath) throws IOException {
        List<Applicant> applicants = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            boolean isFirstLine = true;

            while ((line = reader.readNext()) != null) {
                System.out.println("読み取り中: " + Arrays.toString(line));
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 必須列数チェック（認定番号 + スコア含めて最低7列）
                if (line.length < 5) continue;

                String certNumber = line[0].trim(); // 認定番号
                String name = line[1].trim();
                String email = line[2].trim();
                String prefecture = line[3].trim();
                String city = line[4].trim();

                // 認定番号がある → 既存認定者 → スキップ
                if (!certNumber.isEmpty()) {
                    continue;
                }

                // メールが空 or スコアが無効 or 重複 → スキップ
                if (email.isEmpty() || seenEmails.contains(email)) continue;

                int score = 90;

                Applicant applicant = new Applicant();
                applicant.setName(name);
                applicant.setEmail(email);
                applicant.setPrefecture(prefecture);
                applicant.setCity(city);
                applicant.setScore(score);
                applicant.setCreatedAt(LocalDateTime.now());

                applicants.add(applicant);
                seenEmails.add(email);
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSVファイルの読み込みに失敗しました", e);
        }

        return applicants;
    }

    // 認定済みデータ（認定番号つき）を読み込む
    public List<Object[]> readExsistingDataFromFile(String filePath) throws IOException {
        List<Object[]> result = new ArrayList<>();
        List<Applicant> applicants = new ArrayList<>();
        List<String> certNumbers = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            boolean isFirstLine = true;

            while ((line = reader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (line.length < 5) continue;

                String certNumber = line[0].trim();

                Applicant applicant = new Applicant();
                applicant.setName(line[1].trim());
                applicant.setEmail(line[2].trim());
                applicant.setPrefecture(line[3].trim());
                applicant.setCity(line[4].trim());
                applicant.setScore(60); // 認定済みはスコア不要のため仮で60を設定
                applicant.setCreatedAt(LocalDateTime.now());

                applicants.add(applicant);
                certNumbers.add(certNumber);
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSVファイルの読み込みに失敗しました", e);
        }

        result.add(new Object[]{applicants, certNumbers});
        return result;
    }
}
