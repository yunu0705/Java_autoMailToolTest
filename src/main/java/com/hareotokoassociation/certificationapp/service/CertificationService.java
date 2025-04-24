package com.hareotokoassociation.certificationapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hareotokoassociation.certificationapp.model.Applicant;
import com.hareotokoassociation.certificationapp.model.Certification;
import com.hareotokoassociation.certificationapp.repository.ApplicantRepository;
import com.hareotokoassociation.certificationapp.repository.CertificationRepository;

@Service
public class CertificationService {

    private final ApplicantRepository applicantRepository;
    private final CertificationRepository certificationRepository;

    @Autowired
    public CertificationService(ApplicantRepository applicantRepository,
                                 CertificationRepository certificationRepository) {
        this.applicantRepository = applicantRepository;
        this.certificationRepository = certificationRepository;
    }

    // 申請者の認定処理を行う（新規か再送かを判断）
    @Transactional
    public CertificationResult processApplicant(Applicant applicant) {
        Optional<Certification> existingCert = certificationRepository.findByApplicantEmail(applicant.getEmail());

        if (existingCert.isPresent()) {
            Certification cert = existingCert.get();
            // 認定番号がnullなら再発行
            if (cert.getCertificationNumber() == null || cert.getCertificationNumber().isEmpty()) {
                cert.setCertificationNumber(generateCertificationNumber());
                certificationRepository.save(cert);
            }
            return new CertificationResult(cert, true); // 再送扱い
        } else {
            applicant.setCreatedAt(LocalDateTime.now());
            Applicant savedApplicant = applicantRepository.save(applicant);
            Certification cert = createNewCertification(savedApplicant);
            return new CertificationResult(cert, false); // 新規認定
        }
    }

    // 認定番号を生成
    public String generateCertificationNumber() {
        String maxNumber = certificationRepository.findMaxCertificationNumber();
        int startNumber = 10774;

        if (maxNumber == null || maxNumber.isEmpty()) {
            return String.valueOf(startNumber);
        }

        try {
            int nextNumber = Math.max(Integer.parseInt(maxNumber) + 1, startNumber);
            return String.valueOf(nextNumber);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("認定番号の形式が不正です: " + maxNumber);
        }
    }

    // 新しい認定レコードを作成
    private Certification createNewCertification(Applicant applicant) {
        Certification certification = new Certification();
        certification.setApplicant(applicant);
        certification.setCertificationNumber(generateCertificationNumber());
        certification.setIssueDate(LocalDate.now());
        certification.setCreatedAt(LocalDateTime.now());
        return certificationRepository.save(certification);
    }

    // 認定済みデータのインポート処理
    @Transactional
    public void importExistingCertifications(List<Applicant> applicants, List<String> certificationNumbers) {
        if (applicants.size() != certificationNumbers.size()) {
            throw new IllegalArgumentException("申請者リストと認定番号リストの長さが一致しません");
        }

        for (int i = 0; i < applicants.size(); i++) {
            Applicant applicant = applicants.get(i);
            String certNumber = certificationNumbers.get(i);

            // すでに同じメールがDBに存在する場合はスキップ
            if (applicantRepository.existsByEmail(applicant.getEmail())) {
                continue;
            }

            applicant.setCreatedAt(LocalDateTime.now());
            Applicant savedApplicant = applicantRepository.save(applicant);

            Certification certification = new Certification();
            certification.setApplicant(savedApplicant);
            certification.setCertificationNumber(certNumber);
            certification.setIssueDate(LocalDate.now());
            certification.setCreatedAt(LocalDateTime.now());

            certificationRepository.save(certification);
        }
    }

    // 認定済みリストを取得（CSV出力などに使用）
    public List<Certification> getAllCertificationsForVoting() {
        return certificationRepository.findAllByOrderByCertificationNumberAsc();
    }

    // 認定情報を手動保存（認定番号付与後の保存用）
    @Transactional
    public void saveCertification(Certification certification) {
        certificationRepository.save(certification);
    }

    // 認定結果を格納する内部クラス
    public static class CertificationResult {
        private final Certification certification;
        private final boolean isResend;

        public CertificationResult(Certification certification, boolean isResend) {
            this.certification = certification;
            this.isResend = isResend;
        }

        public Certification getCertification() {
            return certification;
        }

        public boolean isResend() {
            return isResend;
        }
    }
}
