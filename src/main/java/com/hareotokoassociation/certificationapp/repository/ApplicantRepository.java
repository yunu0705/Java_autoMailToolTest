package com.hareotokoassociation.certificationapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hareotokoassociation.certificationapp.model.Applicant;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
	
	// メールアドレスで申請者を検索
	Optional<Applicant> findByEmail(String email);
	
	// メールアドレスで申請者の存在チェック
	boolean existsByEmail(String email);
}
