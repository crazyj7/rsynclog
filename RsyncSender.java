import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class RsyncSender {
    private static final int STATUS_CHECK_INTERVAL = 60000; // 상태 체크용 주기 1분
    private static final int CONNECT_TIMEOUT = 3000;     // 접속 timeout: 3초

    // 수신자 상태를 관리하는 ConcurrentMap
    private static final ConcurrentMap<String, Boolean> destinationStatus = new ConcurrentHashMap<>();
    
    // 수신자 상태 체크 쓰레드
    static class StatusChecker extends Thread {
        private final String[] destinations;
        
        public StatusChecker(String[] destinations) {
            this.destinations = destinations;
            for (String dest : destinations) {
                destinationStatus.put(dest, true); // 초기 상태는 활성화로 설정
            }
        }
        
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                for (String destination : destinations) {
                    String[] hostPort = destination.split(":");
                    String host = hostPort[0];
                    int port = Integer.parseInt(hostPort[1]);
                    
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
                        destinationStatus.put(destination, true);
                        System.out.println("Status check: " + destination + " is UP");
                    } catch (Exception e) {
                        destinationStatus.put(destination, false);
                        System.out.println("Status check: " + destination + " is DOWN - " + e.getMessage());
                    }
                }
                
                try {
                    Thread.sleep(STATUS_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error setting UTF-8 encoding: " + e.getMessage());
            return;
        }
        
        if (args.length != 3) {
            System.out.println("Usage: java RsyncSender [file_path] [destination1,destination2,...] [shared_key]");
            System.out.println("Example: java RsyncSender input.txt 192.168.0.2:8081,192.168.0.3:8082 passphase");
            return;
        }

        String filePath = args[0];
        String[] destinations = args[1].split(",");
        String sharedKey = args[2];
        
        // 상태 체크 쓰레드 시작
        StatusChecker statusChecker = new StatusChecker(destinations);
        statusChecker.setDaemon(true);
        statusChecker.start();
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("Created new file: " + filePath);
            }

            long lastPosition = file.length();
            System.out.println("Monitoring file from position: " + lastPosition);

            while (true) {
                try {
                    long fileLength = file.length();

                    if (fileLength > lastPosition) {
                        List<String> newLines = new ArrayList<>();
                        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                            raf.seek(lastPosition);
                            String line;
                            while ((line = raf.readLine()) != null) {
                                line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                                newLines.add(line);
                            }
                            lastPosition = raf.getFilePointer();
                        }

                        for (String destination : destinations) {
                            // 수신자가 활성화 상태인 경우에만 전송
                            if (destinationStatus.getOrDefault(destination, false)) {
                                String[] hostPort = destination.split(":");
                                String host = hostPort[0];
                                int port = Integer.parseInt(hostPort[1]);
                                
                                try (Socket socket = new Socket()) {
                                    // 데이터 전송 시 짧은 timeout 사용
                                    socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
                                    
                                    try (BufferedWriter out = new BufferedWriter(
                                            new OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                                        )) {
                                        
                                        for (String line : newLines) {
                                            System.out.println("Sending to " + destination + " (length: " + line.length() + " bytes)");
                                            System.out.println("Original data: " + line);
                                            
                                            String encryptedLine = CryptoUtil.encrypt(line, sharedKey);
                                            out.write(encryptedLine);
                                            out.newLine();
                                            out.flush();
                                        }
                                        
                                        System.out.println("Data encrypted and transmitted successfully to " + destination);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error sending to " + destination + ": " + e.getMessage());
                                    destinationStatus.put(destination, false); // 전송 실패 시 상태 업데이트
                                }
                            } else {
                                System.out.println("Skipping " + destination + " - destination is DOWN");
                            }
                        }
                    }

                    Thread.sleep(1000);

                } catch (Exception e) {
                    System.out.println("Error during transmission: " + e.getMessage());
                    e.printStackTrace();
                    Thread.sleep(5000);
                }
            }

        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            statusChecker.interrupt(); // 프로그램 종료 시 상태 체크 쓰레드 중지
        }
    }
} 