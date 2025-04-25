# ☀️ 晴れ男・晴れ女 認定アプリ - Certification App

このアプリは、一般社団法人全日本晴れ男・晴れ女協会の「晴れ男・晴れ女検定」の結果をもとに、合格者に対して認定番号の発行やGmailAPIを用いた認定メール送信を行うJavaデスクトップアプリです。

---

## ✨ Features / 機能

- ローカルCSVファイルから新規認定対象者を取り込み
- 同一メールの重複認定を回避
- メール内容は定型HTML + 認定番号を含む
- Gmail APIを通じて認定者へ自動送信
- 認定結果をCSVとしてエクスポート (エンコード: UTF-8 with BOM)
- GUI起動 (強制ウィンドウ機能)

---

## 🚀 Quick Start

### 💻 Prerequisites
- Java 21  
- MySQL 8.x（8.0.36で動作確認済み）  
- Maven 3.x（3.9.5で動作確認済み）
- Gmail API クライアントデータ (credentials.json)

### 📆 DB Setup (MySQL)　テスト版
```sql
CREATE DATABASE cert_app_test DEFAULT CHARACTER SET utf8mb4;
CREATE USER 'test_user'@'localhost' IDENTIFIED BY 'secret';
GRANT ALL PRIVILEGES ON cert_app_test.* TO 'test_user'@'localhost';
```

### 🚒 Build
```bash
mvn clean package -DskipTests
```

### 💡 Run
```bash
java -Djava.awt.headless=false -jar target/certification-app-0.0.1-SNAPSHOT.jar
```

---

## ⚙️ Configuration

`application.properties`:
```properties
spring.application.name=certification-app
spring.datasource.url=jdbc:mysql://localhost:3306/cert_app_test
spring.datasource.username=test_user
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
app.mode=normal
```

**Note:** GitHubで公開する際はこのファイルを.gitignoreで除外することを推奨

---

## 🎨 UI
- `JFileChooser`でCSVアップロード
- `JOptionPane`でエラー表示/確認ダイアログ

---

## 🌐 Gmail API
- Gmail API OAuth2.0
- HTMLメール送信
- さまざまなドメインに対したBase64/UTF-8エンコード完備

---
