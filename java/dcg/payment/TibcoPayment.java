package dcg.payment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jms.DeliveryMode;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import com.tibco.tibjms.TibjmsQueueConnectionFactory;

public class TibcoPayment implements Callable {

	private String user;
	private String password;
	private String url;
	private String sendQueueName;
	private String receiveQueueName;
	private String selectorKey;
	private String selectorVal;
	private String businessEvent;
	private String schemaVersion;
	private String userId;
	private String routeInfo;
	private String messageType;
	private String companyId;
	private String xmlSource;
	private String receiveSelector;
	private String javaReceiver;
	
	
	private String headerId;
	private String timingName;
	private String trackingService;
	private String trackingEngine;
	private String trackingStatus;
	private String trackingSequenceNo;
	private String loginUserId;
	private String loginPin;
	private String operationMode;
	private String terminalIp;
	private String transactionType;
	private String node;
	private String mailbox;
	private String pspAccountId;
	private String countryCode;
	private String serviceProvider;
	private String paymentCardToken;
	private String offlineAuthCode;
	private String isFirstCardPaymentAttempt;
	
	private static final XMLOutputter OUTPUT = new XMLOutputter(
			Format.getPrettyFormat());

	protected static final Namespace ns0 = Namespace.getNamespace("http://xmlns.cpw.co.uk/CPW/Common/Payment/ProcessCPPayment");
	protected static final Namespace ns1 = Namespace.getNamespace("http://xmlns.cpw.co.uk/CPWIntegrationsTibco/Common/CommonHeader");
	protected static final Namespace ns2 = Namespace.getNamespace("http://xmlns.cpw.co.uk/CPW/CDM/CardPaymentDetails");
	protected static final Namespace ns3 = Namespace.getNamespace("http://xmlns.cpw.co.uk/CPW/CDM/CPCreditCardPaymentDetails");
	protected static final Namespace ns4 = Namespace.getNamespace("http://xmlns.cpw.co.uk/CPW/CDM/TerminalIntegrationCommand");
	
	protected static final String xmlFromMule = "mule";

	public static String readFile(String filePath) throws IOException {

		InputStreamReader inputReader = null;
		BufferedReader bufferReader = null;
		//InputStream inputStream = new FileInputStream(filePath);
		InputStream inputStream = TibcoPayment.class.getResourceAsStream(filePath);
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

	@Override
	public synchronized Object onCall(MuleEventContext eventContext)
			throws Exception {

		QueueConnection connection = null;
		QueueSession session = null;

		String xml = null;
		String rtnPayload = null;

		try {
			if (this.xmlSource.equals(xmlFromMule)) {
				xml = eventContext.getMessage().getPayload().toString();
			} else {
				// switch to file input
				String payload = readFile("ProcessCPPaymentReq1.xml");
				xml = composeXML(payload, eventContext);
			}
			String correlationId = eventContext.getMessage().getProperty(
					"MULE_CORRELATION_ID", PropertyScope.OUTBOUND);
			String timeStampTxt = eventContext.getMessage().getProperty(
					"SLATimeStamp", PropertyScope.OUTBOUND);

			// create the connection
			QueueConnectionFactory connectionFactory = new TibjmsQueueConnectionFactory(url);
			connection = connectionFactory.createQueueConnection(this.user,	this.password);
			session = connection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

			Queue senderQueue = session.createQueue(sendQueueName);
			QueueSender sender = session.createSender(senderQueue);
			sender.setDeliveryMode(DeliveryMode.PERSISTENT);

			TextMessage jmsMessage = session.createTextMessage();

			jmsMessage.setStringProperty("SourceId", selectorVal);
			jmsMessage.setStringProperty("BusinessEvent", businessEvent);
			jmsMessage.setStringProperty("SchemaVersion", schemaVersion);
			jmsMessage.setStringProperty("UserId", userId);
			jmsMessage.setStringProperty("SLATimeStamp", timeStampTxt);
			jmsMessage.setStringProperty("RouteInfo", routeInfo);
			jmsMessage.setStringProperty("MessageType", messageType);
			jmsMessage.setStringProperty("CompanyId", companyId);

			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setText(xml);
			sender.send(jmsMessage);
			System.out.println("Sent message in Java:\n\n\n " + xml);
			System.out.println("Sent JMSCorrelationID: " + correlationId);
			System.out.println("Java Receive: " + javaReceiver + "\n\n\n");
			
			rtnPayload = xml;
			
			if(this.javaReceiver.equals("yes")){
				connection.start();
				Queue receiverQueue = session.createQueue(receiveQueueName);
				QueueReceiver receiver = null;
				if(receiveSelector.equals("yes")){
					 receiver = session.createReceiver(receiverQueue,"JMSCorrelationID = '" + correlationId + "'");
				}else{
					 receiver = session.createReceiver(receiverQueue);
				}
				TextMessage message = (TextMessage) receiver.receive();
				rtnPayload = message.getText();
				System.out.println("Received message in Java: \n\n\n " + rtnPayload);
				System.out.println("Received JMSCorrelationID: " + message.getJMSCorrelationID());
				connection.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				// close session and connection
				if (session != null) {
					session.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return rtnPayload;
	}

	private String composeXML(String payload, MuleEventContext eventContext)
			throws Exception {

		String requestId = eventContext.getMessage().getProperty("requestId",PropertyScope.INVOCATION);
		Double amountPay = Double.parseDouble((String) eventContext.getMessage()
				.getProperty("amountPay", PropertyScope.INVOCATION));

		String timeStampTxt = eventContext.getMessage().getProperty("SLATimeStamp", PropertyScope.OUTBOUND);

		ByteArrayInputStream msgByte = new ByteArrayInputStream(payload.getBytes("UTF-8"));
		SAXBuilder builder = new SAXBuilder(false);
		Document document = builder.build(msgByte);

		Element rootElement = document.getRootElement();
		
		Element paymentTransactionIdEle = rootElement.getChild("paymentTransactionId", ns2);
		if (paymentTransactionIdEle != null) {
			paymentTransactionIdEle.setText(requestId);
		}
		
		Element paymentTransactionTypeEle = rootElement.getChild("paymentTransactionType",ns2);
		if (paymentTransactionTypeEle != null) {
			paymentTransactionTypeEle.setText(transactionType);
		}
		
		Element creditCardPaymentAmountEle = rootElement.getChild("creditCardPaymentAmount",ns2);
		if (creditCardPaymentAmountEle != null) {
			creditCardPaymentAmountEle.setText((int) (amountPay * 100) + "");
		}
		
		Element pspAccountIdEle = rootElement.getChild("pspAccountId",ns2);
		if (pspAccountIdEle != null) {
			pspAccountIdEle.setText(pspAccountId);
		}	
		
		Element countryCodeEle = rootElement.getChild("countryCode",ns2);
		if (countryCodeEle != null) {
			countryCodeEle.setText(countryCode);
		}			

		Element serviceProviderEle = rootElement.getChild("serviceProvider",ns2);
		if (serviceProviderEle != null) {
			serviceProviderEle.setText(serviceProvider);
		}	
		
		
		
		Element terminalIpEle = rootElement.getChild("terminalIPAddress",ns3);
		if (terminalIpEle != null) {
			terminalIpEle.setText(terminalIp);
		}
		
		Element operationModeEle = rootElement.getChild("operationMode",ns3);
		if (operationModeEle != null) {
			operationModeEle.setText(operationMode);
		}
		
		Element mailboxEle = rootElement.getChild("mailbox",ns3);
		if (mailboxEle != null) {
			mailboxEle.setText(mailbox);
		}
		
		Element nodeEle = rootElement.getChild("node",ns3);
		if (nodeEle != null) {
			nodeEle.setText(node);
		}		

	
		Element offlineAuthCodeEle = rootElement.getChild("offlineAuthCode",ns2);
		if (offlineAuthCodeEle != null) {
			offlineAuthCodeEle.setText(this.offlineAuthCode);
		}		
		
		
		
		Element userIdEle = rootElement.getChild("LoginDetails", ns0).getChild("userId", ns4);
		
		if (userIdEle != null) {
			userIdEle.setText(this.loginUserId);
		}			
		
		Element loginPinEle = rootElement.getChild("LoginDetails", ns0).getChild("personalIdentificationNumber", ns4);
		
		if (loginPinEle != null) {
			loginPinEle.setText(this.loginPin);
		}		
		
		Element commonHeader = rootElement.getChild("CommonHeader", ns0).getChild("commonHeader", ns1);
		
		Element headerId = commonHeader.getChild("id", ns1);
		if(headerId != null){
			headerId.setText(this.headerId);
		}
		
		Element headerToId = commonHeader.getChild("to", ns1);
		if(headerToId != null){
			headerToId.setAttribute("destination", this.sendQueueName, ns1);			
		}
		
		
		Element trackingItem = commonHeader.getChild("tracking", ns1).getChild("item", ns1);

		if (trackingItem != null) {
			trackingItem.setAttribute("service", this.trackingService, ns1);
			trackingItem.setAttribute("engine", this.trackingEngine, ns1);
			trackingItem.setAttribute("status", this.trackingStatus, ns1);
			trackingItem.setAttribute("sequenceNo", this.trackingSequenceNo, ns1);
			trackingItem.setAttribute("timestamp", timeStampTxt, ns1);
		}

		Element timing = commonHeader.getChild("timing", ns1).getChild("timing", ns1);

		if (timing != null) {
			timing.setAttribute("name", this.timingName, ns1);
			timing.setAttribute("timestamp", timeStampTxt, ns1);
		}

		return OUTPUT.outputString(document);

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

	public void setSelectorKey(String selectorKey) {
		this.selectorKey = selectorKey;
	}

	public void setSelectorVal(String selectorVal) {
		this.selectorVal = selectorVal;
	}

	public void setSendQueueName(String sendQueueName) {
		this.sendQueueName = sendQueueName;
	}

	public void setBusinessEvent(String businessEvent) {
		this.businessEvent = businessEvent;
	}

	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setRouteInfo(String routeInfo) {
		this.routeInfo = routeInfo;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	public void setReceiveQueueName(String receiveQueueName) {
		this.receiveQueueName = receiveQueueName;
	}

	public void setXmlSource(String xmlSource) {
		this.xmlSource = xmlSource;
	}

	public void setReceiveSelector(String receiveSelector) {
		this.receiveSelector = receiveSelector;
	}

	public void setJavaReceiver(String javaReceiver) {
		this.javaReceiver = javaReceiver;
	}

	public void setHeaderId(String headerId) {
		this.headerId = headerId;
	}

	public void setTimingName(String timingName) {
		this.timingName = timingName;
	}

	public void setTrackingService(String trackingService) {
		this.trackingService = trackingService;
	}

	public void setTrackingEngine(String trackingEngine) {
		this.trackingEngine = trackingEngine;
	}

	public void setTrackingStatus(String trackingStatus) {
		this.trackingStatus = trackingStatus;
	}

	public void setTrackingSequenceNo(String trackingSequenceNo) {
		this.trackingSequenceNo = trackingSequenceNo;
	}

	public void setLoginUserId(String loginUserId) {
		this.loginUserId = loginUserId;
	}

	public void setLoginPin(String loginPin) {
		this.loginPin = loginPin;
	}

	public void setOperationMode(String operationMode) {
		this.operationMode = operationMode;
	}

	public void setTerminalIp(String terminalIp) {
		this.terminalIp = terminalIp;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public void setMailbox(String mailbox) {
		this.mailbox = mailbox;
	}

	public void setPspAccountId(String pspAccountId) {
		this.pspAccountId = pspAccountId;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public void setServiceProvider(String serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

	public void setPaymentCardToken(String paymentCardToken) {
		this.paymentCardToken = paymentCardToken;
	}

	public void setOfflineAuthCode(String offlineAuthCode) {
		this.offlineAuthCode = offlineAuthCode;
	}

	public void setIsFirstCardPaymentAttempt(String isFirstCardPaymentAttempt) {
		this.isFirstCardPaymentAttempt = isFirstCardPaymentAttempt;
	}

	
}
