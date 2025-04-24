package com.hareotokoassociation.certificationapp.service;

import org.springframework.stereotype.Service;

import com.hareotokoassociation.certificationapp.model.Applicant;
import com.hareotokoassociation.certificationapp.model.Certification;

@Service
public class EmailService {
	
	private static final String EMAIL_TEMPLATE = "この度は一般社団法人全日本晴れ男・晴れ女協会の「晴れ男・晴れ女検定」に受験くださり誠にありがとうございます。\n\n" +
            "採点の結果、貴殿は本協会の基準に達しましたので本協会公認の「晴れ男・晴れ女」として認めます。\n\n" +
            "今後、晴れを呼ぶ「人」として世の中を明るく照らす太陽になってください。\n\n" +
            "認定番号は「%s」になります。\n\n" +
            "認定証の発行が希望の方は下記から認定証の発行を行ってください。";
	
	// 認定メールの本文を生成
	public String generateEmailContent(Applicant applicatn, Certification certification) {
		return String.format(EMAIL_TEMPLATE, certification.getCertificationNumber());
	}
	

}
