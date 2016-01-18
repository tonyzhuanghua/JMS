import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import com.tibco.tibjms.TibjmsQueueConnectionFactory;

public class TestTibcoJms {

//	public static final String user = "tibbw";  
//    public static final String password = "tibbw";  
//	public static final String url = "tcp://gbnvraapp01.cpwplc.net:9330"; 
    public static final String user = "hua.zhuang";  
    public static final String password = "hua.zhuang"; 
    public static final String url = "tcp://localhost:8222";  
    public static final String queueInName = "payment_in";  
    public static final String queueOutName = "payment_out";
   // public static final String queueOutName = "CPW.GBR.QA.Public.Request.Common.Payment.ProcessCPPayment.1";
    public static final String messageBody = "Hello Tibco!";  
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
    
	
	 public static String getKey(){		 
	     int rInt = new Random().nextInt(1000);
	     long nano = Math.abs((System.nanoTime() % 1000));
	     Long longId = new Long(System.currentTimeMillis() + String.format("%0" + 3 + "d", nano) + String.format("%0" + 3 + "d", rInt));
	     return longId.toString().substring(10);
	 }
	 
	 
    public static void main(String[] args) throws JMSException, IOException{  
    	
    	QueueConnection connection = null;  
    	QueueSession session = null;  
    	//System.out.println("UUID: " + getKey());
          
     
           
            QueueConnectionFactory connectionFactory = new TibjmsQueueConnectionFactory(url);
            
			connection = connectionFactory.createQueueConnection(user, password);
			session = connection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    		connection.start();
    		
    		
    		
 /////////////////////////   		
    		
     		Queue receiverQueue = session.createQueue(queueInName);
    		QueueReceiver receiver = session.createReceiver(receiverQueue);
			TextMessage message = (TextMessage) receiver.receive();
			System.out.println("Received message from request: " + message.getText());
			
			
			
			System.out.println("SourceId: " + message.getStringProperty("SourceId"));
			System.out.println("BusinessEvent: " + message.getStringProperty("BusinessEvent"));
			System.out.println("SchemaVersion: " + message.getStringProperty("SchemaVersion"));
			System.out.println("UserId: " + message.getStringProperty("UserId"));
			System.out.println("SLATimeStamp: " + message.getStringProperty("SLATimeStamp"));
			System.out.println("RouteInfo: " + message.getStringProperty("RouteInfo"));
			System.out.println("MessageType: " + message.getStringProperty("MessageType"));
			System.out.println("CompanyId: " + message.getStringProperty("CompanyId"));
			
			
			
			String file = "C:\\git\\hb-dcpwpospoc-mule\\dcpw-api\\src\\main\\api\\payment\\Examples\\ProcessCPPaymentResp1.xml";//ProcessCPPaymentResponse.xml
			
			Queue  senderQueue = session.createQueue(queueOutName); 
/////////////////			
 
			
//			TextMessage message = session.createTextMessage();
//            String file = "C:\\git\\hb-dcpwpospoc-mule\\dcpw-api\\src\\main\\api\\payment\\Examples\\ProcessCPPaymentReq1.xml";
			// Queue  senderQueue = session.createQueue(queueInName); 
            
            
            String outMsgBody = readFile(file);
            message.clearBody();
            message.setText(outMsgBody);

			
			QueueSender sender = session.createSender(senderQueue);
			sender.send(message);
			System.out.println("Sent message to response: " + outMsgBody);
			System.out.println("CorrelationId is:" + message.getJMSCorrelationID());
			
			connection.close();
        	
        	
              
    }
}
