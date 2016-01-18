import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.jms.JMSContext;

import com.tibco.tibjms.TibjmsQueueConnectionFactory;

public class TestSendTibco {

	public static final String user = "tibbw";  
    public static final String password = "tibbw";  
    public static final String url = "tcp://gbnvraapp01.cpwplc.net:9330";  
    public static final String queueInName = "CPW.GBR.QA.Public.Request.Common.Payment.ProcessCPPayment.1";  
    public static final String queueOutName = "CPW.GBR.QA.Public.Request.Common.Payment.ProcessCPPayment.1";
    public static final String messageBody = "Hello you Tibco!";  
    public static final boolean transacted = false;  
    public static final boolean persistent = true;  
      
    public static void main(String[] args){  
    	QueueConnection connection = null;  
    	QueueSession session = null;  
          
        try{  
           
            QueueConnectionFactory connectionFactory = new TibjmsQueueConnectionFactory(url);
          
			connection = connectionFactory.createQueueConnection(user, password);
			session = connection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
 
			Queue  senderQueue = session.createQueue(queueOutName); 
                   
            // create the producer  
			QueueSender sender = session.createSender(senderQueue); 

			TextMessage jmsMessage = session.createTextMessage();
			jmsMessage.setJMSCorrelationID("123456");
			jmsMessage.setStringProperty("SourceId", "HoneyBee");
			jmsMessage.setText(messageBody);
			sender.send(jmsMessage);
			System.out.println("Sent message: " + messageBody);
			

              
 
              
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
