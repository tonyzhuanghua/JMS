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

public class TestSendJms {

	public static final String user = "system";  
    public static final String password = "manager";  
    public static final String url = "tcp://localhost:61616";  
    public static final String queueInName = "payment_in";  
    public static final String queueOutName = "payment_out";
    public static final String messageBody = "Hello JMS!";  
    public static final boolean transacted = false;  
    public static final boolean persistent = true;  
      
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
            
            
         
            
              
            // create the producer  
            MessageProducer producer = session.createProducer(destination_in);  
            if (persistent){  
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);  
            }else{  
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);  
            }  
 
            Message outMessage = session.createTextMessage(messageBody);  
            outMessage.setStringProperty("IDE", "MULE");
           
            producer.send(outMessage);  
            System.out.println("Send message: " + ((TextMessage)outMessage).getText());  
              
 
              
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
