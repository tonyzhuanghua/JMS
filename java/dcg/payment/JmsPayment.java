package dcg.payment;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

public class JmsPayment implements Callable{

	private String user;  
	private String password;  
	private String url;
	private String receiveQueueName;
	private String selectorKey;
	private String selectorVal;

	
	@Override
	public synchronized Object onCall(MuleEventContext eventContext) throws Exception {
 
	    Connection connection = null;  
        Session session = null;  
        
    	
		String correlationId= null;  
          
        try{  
        	correlationId = eventContext.getMessage().getProperty("MULE_CORRELATION_ID", PropertyScope.OUTBOUND);
        	
    		System.out.println("JMSCorrelationID: " + correlationId);
            // create the connection  
    		
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(this.user, this.password, this.url);  
            connection = connectionFactory.createConnection();  
            connection.start();  
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);  
            Destination destination_out = session.createQueue(this.receiveQueueName); 
            
            MessageConsumer consumer = null;

            if(correlationId!=null){
             	consumer = session.createConsumer(destination_out, "JMSCorrelationID = '" + correlationId + "'");
             }else{
            
	            if(this.selectorKey !=null && this.selectorVal!=null){
	            	 consumer = session.createConsumer(destination_out, this.selectorKey+"='"+this.selectorVal+"'");  

	            }else{
	            	 consumer = session.createConsumer(destination_out);
	            }
             }
            
            Message recvMessage = consumer.receive();  
    		
            return ((TextMessage)recvMessage).getText();
            
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
		
		
		
		return null;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setReceiveQueueName(String receiveQueueName) {
		this.receiveQueueName = receiveQueueName;
	}
	public void setSelectorKey(String selectorKey) {
		this.selectorKey = selectorKey;
	}
	public void setSelectorVal(String selectorVal) {
		this.selectorVal = selectorVal;
	}

	
	

}
