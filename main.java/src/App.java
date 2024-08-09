import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Ref :
 *      RCF - https://datatracker.ietf.org/doc/html/rfc6455#section-5.2
 */

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");

        ServerSocket server = new ServerSocket(80);
        try {
            System.out.println("Server has started on 127.0.0.1:80.\r\nWaiting for a connection…");
            Socket client = server.accept();
            System.out.println("A client connected.");


            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            Scanner s = new Scanner(in, "UTF-8");

            String data = s.useDelimiter("\\r\\n\\r\\n").next();
            Matcher get = Pattern.compile("^GET").matcher(data);
          
            if (get.find()) {
                Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                match.find();

                byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                  + "Connection: Upgrade\r\n"
                  + "Upgrade: websocket\r\n"
                  + "Sec-WebSocket-Accept: "
                  + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                  + "\r\n\r\n").getBytes("UTF-8");

                out.write(response, 0, response.length);


                // Now handle WebSocket frames
                while (true) {
                    // Read the frame header. First 16-bit.
                    int firstByte = in.read();      // i.e.  129 = 1000 0001
                    int secondByte = in.read(); 


                    System.out.println("first byte = "  + firstByte);

                    // Decode the FIN, Opcode, and masking information
                    // boolean fin = (firstByte & 0x80) != 0;
                    int opcode = firstByte & 0x0F;   // ox0f = 1000 0001 [AND] 0000 1111 = 1 decimal.
                    int payloadLength = secondByte & 0x7F;
                    boolean masked = (secondByte & 0x80) != 0; // This condition checks if the frame is masked. The masked variable is a boolean that indicates whether the WebSocket frame's payload data is masked


                    if (payloadLength == 126) {
                        // WebSocket frames can specify payload lengths in different ways. If payloadLength is 126, it indicates that the length of the payload is specified in an additional 2 bytes
                        payloadLength = (in.read() << 8) | in.read(); // read next two byte. XOR into 16-bit number
                    } else if (payloadLength == 127) {
                        // If payloadLength is 127, it indicates that the payload length is specified using 8 bytes, which allows for extremely large payloads.
                        throw new UnsupportedOperationException("Payload length of 127 not supported");
                    }

                    // Read masking key if frame is masked
                    byte[] maskingKey = new byte[4];
                    if (masked) {
                        // If the frame is masked, this line reads 4 bytes from the input stream (in) and stores them in the maskingKey array. These 4 bytes are the masking key used to mask the payload data.
                        in.read(maskingKey);
                    }

                    // Read payload data
                    byte[] payloadData = new byte[payloadLength];
                    in.read(payloadData);  

                    // Unmasking code
                    byte[] decoded = new byte[payloadLength];                   // Decoded data
                    byte[] encoded = payloadData;                               // Use the actual payload data
                    byte[] key = maskingKey;                                    // Use the actual masking key.
                    for (int i = 0; i < encoded.length; i++) {
                        decoded[i] = (byte) (encoded[i] ^ key[i & 0x3]);        // XOR (exclusive OR) operation to reverse the masking applied to the payload data. i & 0x3 = 11 (binary), ensures range is 0, ..., 3.
                    }

                    // Convert to string and print the message
                    String message = new String(decoded, "UTF-8");
                    System.out.println("Received message: " + message);

                    // Break the loop for testing; remove or modify as needed
                    // if (fin) break;
                }


                
            }







        }catch(Exception e){
            System.out.println(e);
        }
    }
}
