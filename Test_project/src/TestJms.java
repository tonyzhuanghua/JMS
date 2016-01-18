import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jms.Connection;  
import javax.jms.ConnectionFactory;  
import javax.jms.DeliveryMode;  
import javax.jms.Destination;  
import javax.jms.Message;  
import javax.jms.MessageConsumer;  
import javax.jms.MessageProducer;  
import javax.jms.Session;  
import javax.jms.TextMessage;  

import org.apache.activemq.ActiveMQConnectionFactory; 

public class TestJms {

	public static final String user = "system";  
    public static final String password = "manager";  
    public static final String url = "tcp://localhost:61616";  
    public static final String queueInName = "payment_in";  
    public static final String queueOutName = "payment_out";
    public static final String messageBody = "Hello JMS!";  
    public static final boolean transacted = false;  
    public static final boolean persistent = true;  
      
    
	public static String readFile(String filePath) throws IOException {

		InputStreamReader inputReader = null;
		BufferedReader bufferReader = null;
		InputStream inputStream = new FileInputStream(filePath);
		inputReader = new InputStreamReader(inputStream);
		bufferReader = new BufferedReader(inputReader);
		String line = null;
		StringBuffer strBuffer = new StringBuffer();
		while ((line = bufferReader.readLine()) != null) {
			strBuffer.append(line);
		}
		bufferReader.close();
		return strBuffer.toString();

	}
    
    public static void main(String[] args){  
        Connection connection = null;  
        Session session = null;  
          
        try{  
            // create the connection  
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);  
            connection = connectionFactory.createConnection();  
            connection.start();  
              
            // create the session  
            session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);  
            Destination destination_in = session.createQueue(queueInName); 
            
            Destination destination_out = session.createQueue(queueOutName);  
            
            
            
            
            MessageConsumer consumer = session.createConsumer(destination_in);  
            Message recvMessage = consumer.receive();  
            System.out.println("Receive message: " + ((TextMessage)recvMessage).getText()); 
            
            
            
              
            // create the producer  
            MessageProducer producer = session.createProducer(destination_out);  
            if (persistent){  
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);  
            }else{  
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);  
            }  
 
            
            String file = "C:\\git\\hb-dcpwpospoc-mule\\dcpw-api\\src\\main\\api\\payment\\Examples\\ProcessCPPaymentResponse.xml";
            String outMsgBody = readFile(file);
            recvMessage.clearBody();
            ((TextMessage)recvMessage).setText(outMsgBody);
            Message outMessage = recvMessage; //session.createTextMessage(outMsgBody);  
            //outMessage.setStringProperty("Resource", "HoneyBee");
            //Message outMessage = recvMessage;
            producer.send(outMessage);  
             
            System.out.println("Send message:::: " + ((TextMessage)outMessage).getText());  
            System.out.println("JMSCorrelationID:::: " + outMessage.getJMSCorrelationID()); 
 
              
        }catch (Exception e){  
            e.printStackTrace();  
        }finally{  
            try{  
                // close session and connection  
                if (session != null){  
                    session.close();  
                }  
                if (connection != null){  
                    connection.close();  
                }  
            }catch (Exception e){  
                e.printStackTrace();  
            }  
        }  
    }  
}
