package doctor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author matheus
 */
public class Client {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    
    /**
     * 
     * @param message to be sent to the server
     * @return the response that came from the server
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public static HashMap<String, Object> send(HashMap<String, Object> message) throws IOException, ClassNotFoundException {
        //abre um novo socket local apontando para a porta 8080
        socket = new Socket("127.0.0.1", 8080);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        
        //envia a mensagem ao server
        out.writeObject(message);
        return (HashMap<String, Object>) in.readObject();
    }
    
    
}
