import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;

public class RsyncReceiver {
    
    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error setting UTF-8 encoding: " + e.getMessage());
            return;
        }
        
        if (args.length != 3) {
            System.out.println("Usage: java RsyncReceiver [port] [output_file] [shared_key]");
            System.out.println("Example: java RsyncReceiver 8081 a.out passphase");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String outputFile = args[1];
        String sharedKey = args[2];

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Listening on port " + port + "...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected from: " + clientSocket.getInetAddress());

                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), "UTF-8")
                    );

                    try (BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(
                                new FileOutputStream(outputFile, true), "UTF-8"
                            ))) {
                        
                        String encryptedLine;
                        while ((encryptedLine = in.readLine()) != null) {
                            try {
                                // 수신한 데이터 복호화
                                String decryptedLine = CryptoUtil.decrypt(encryptedLine, sharedKey);
                                
                                // 콘솔에 출력
                                System.out.println(decryptedLine);
                                
                                // 파일에 쓰기
                                writer.write(decryptedLine);
                                writer.newLine();
                                writer.flush();
                            } catch (Exception e) {
                                System.err.println("Error processing line: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }

                    System.out.println("Connection closed. Data saved to " + outputFile);
                } catch (Exception e) {
                    System.err.println("Error handling client: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 