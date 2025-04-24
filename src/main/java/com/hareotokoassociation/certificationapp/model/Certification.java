package com.hareotokoassociation.certificationapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "certifications")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @Column(nullable = false, unique = true)
    private String certificationNumber;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // デフォルトコンストラクタ
    public Certification() {}

    // コンストラクタ（テストや生成に便利なように追加）
    public Certification(Applicant applicant, String certificationNumber, LocalDate issueDate, LocalDateTime createdAt) {
        this.applicant = applicant;
        this.certificationNumber = certificationNumber;
        this.issueDate = issueDate;
        this.createdAt = createdAt;
    }

    // ゲッター、セッター
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Applicant getApplicant() {
        return applicant;
    }

    public void setApplicant(Applicant applicant) {
        this.applicant = applicant;
    }

    public String getCertificationNumber() {
        return certificationNumber;
    }

    public void setCertificationNumber(String certificationNumber) {
        this.certificationNumber = certificationNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Certification{" +
                "id=" + id +
                ", applicant=" + (applicant != null ? applicant.getEmail() : "null") +
                ", certificationNumber='" + certificationNumber + '\'' +
                ", issueDate=" + issueDate +
                ", createdAt=" + createdAt +
                '}';
    }
}
