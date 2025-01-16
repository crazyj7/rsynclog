# Encrypted File Sync Tool

이 프로그램은 파일의 변경사항을 실시간으로 모니터링하고, 변경된 내용을 암호화하여 네트워크를 통해 다른 시스템으로 전송하는 도구입니다.

예를 들어 tomcat log를 원격의 다른 서버에서 볼 수 있도록 하고 싶을 때 사용 가능.


## 주요 기능

- 실시간 파일 모니터링
- AES 암호화를 통한 보안 전송
- SHA-256 기반의 유연한 키 관리
- 라인 단위 실시간 동기화
- UTF-8 인코딩 지원
- 목적지 서버 상태 확인
- 다수의 목적지 서버 지원

## 시스템 요구사항

- Java 1.8 이상
- 네트워크 연결
- 읽기/쓰기 권한이 있는 파일시스템

## 컴파일 방법

UTF-8 인코딩을 사용하여 컴파일합니다:

```bash
# 기존 클래스 파일이 있다면 삭제
rm *.class

# Java 8 버전으로 컴파일
javac -source 1.8 -target 1.8 -encoding UTF-8 CryptoUtil.java RsyncSender.java RsyncReceiver.java
```


## 사용 방법

### 1. 수신자 실행 (목적지 시스템)

```
bash
java -Dfile.encoding=UTF-8 RsyncReceiver [포트번호] [출력파일명] [공유키]

java -Dfile.encoding=UTF-8 RsyncReceiver 8081 output.txt helloworld
```

### 2. 송신자 실행 (소스 시스템)
```
bash
java -Dfile.encoding=UTF-8 RsyncSender [모니터링할파일] [목적지IP:포트,목적지IP:포트,...] [공유키]

java -Dfile.encoding=UTF-8 RsyncSender input.txt 192.168.0.2:8081,192.168.0.3:8082 helloworld

데이터 전송 예)
dir >> input.txt
이렇게 송신자에서 대상 파일에 데이터가 추가되면, 수신자쪽에 자동으로 데이터가 추가됩니다.

백그라운드로 실행
cat > logsender.sh <<EOF
java RsyncSender ./home/tomcat9/logs/catalina.out 192.168.100.1:5230,192.168.100.2:5231 helloworld
EOF

nohup ./logsender.sh &

```



## 주요 기능

- 송신자는 지정된 파일을 모니터링하며, 새로운 내용이 추가될 때마다 자동으로 전송합니다.
- 모니터링할 파일이 없는 경우 자동으로 생성됩니다.
- 수신자는 받은 내용을 지정된 파일에 추가하고 콘솔에도 출력합니다.
- 모든 텍스트는 UTF-8 인코딩을 사용하여 한글 등이 깨지지 않습니다.

## 주의사항

- Windows에서 실행 시 콘솔 인코딩을 UTF-8로 설정하는 것이 좋습니다:
  ```bash
  chcp 65001
  ```
- 방화벽 설정에서 사용할 포트가 열려있어야 합니다.
- 수신자가 먼저 실행된 상태여야 합니다.
- 수신자가 늦게 떠도 상태확인 주기 이후부터 데이터가 전송됩니다.
- 수신자 상태 확인 주기는 1분 입니다. (STATUS_CHECK_TIMEOUT)
- 데이터 전송시 수신자 connect timeout 시간은 3초 입니다. (STATUS_CHECK_TIMEOUT)

