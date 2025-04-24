package com.hareotokoassociation.certificationapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hareotokoassociation.certificationapp.model.Certification;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

	// 申請者のメールアドレスで認定情報を検索
	Optional<Certification> findByApplicantEmail(String email);
	
	// 認定番号順に全ての認定情報を取得
	List<Certification> findAllByOrderByCertificationNumberAsc();
	
	// 最大認定番号を取得するメソッド
	@Query("SELECT MAX(c.certificationNumber) FROM Certification c")
	String findMaxCertificationNumber();
	}
