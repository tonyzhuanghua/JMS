import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.SimpleDateFormat;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class TestGway {
	private volatile String tenderId;
	private volatile String requestId;
	private volatile boolean printFlag = false;
	private volatile boolean completeFlag = false;
	private OutputStream out;
	private InputStream in;
	private Socket socket;
	private Thread incomingMessagePoller;
	private boolean connected;
	protected static final Namespace G8 = Namespace
			.getNamespace("http://www.stslimited.com/g8way/api");
	private static final XMLOutputter OUTPUT = new XMLOutputter(
			Format.getPrettyFormat());

	public void openConnection() throws IOException {

		closeConnection();
		try {
			//this.socket = new Socket("10.240.45.10", 5000);
		    this.socket = new Socket("localhost", 5001);
			this.out = this.socket.getOutputStream();
			this.in = this.socket.getInputStream();
			System.out.println("Connected");
			this.connected = true;
			this.incomingMessagePoller = new Thread() {
				public void run() {
					try {
						while (true) {
							int header = (in.read() << 24) + (in.read() << 16)
									+ (in.read() << 8) + (in.read() << 0);
							
							ByteArrayInputStream message = new ByteArrayInputStream(
									read(in, header));
							SAXBuilder builder = new SAXBuilder(false);
							org.jdom.Document document = builder.build(message);
							SimpleDateFormat time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String TimeString = time.format(new java.util.Date());
							System.out.println(TimeString);
							System.out.println("Receiving:<--<--<--<--<--");
							System.out.println(OUTPUT.outputString(document));
							
							Element rootElement = document.getRootElement();

							for (Object o : rootElement.getChildren()) {
								Element child = (Element) o;
							
								
								if (child.getName().equals("TenderComplete")) {
									completeFlag = true;
									requestId = rootElement.getChildText("RequestId", G8);
								}
								
								if (child.getName().equals("PrintReceipt")) {
									printFlag = true;	              
	                                tenderId = child.getChildText("TenderId", G8);	                             
									requestId = rootElement.getChildText("RequestId", G8);
								}
								
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			};
			this.incomingMessagePoller.start();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void closeConnection() {
		if (this.socket != null) {
			try {
				this.socket.close();
				this.socket = null;
			} catch (IOException e) {
				e.getMessage();
			}
		}
	}

	private boolean send(String data) throws UnsupportedEncodingException {
		
		SimpleDateFormat time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String TimeString = time.format(new java.util.Date());
		System.out.println(TimeString);
		ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes("UTF-8"));

		try {
			SAXBuilder builder = new SAXBuilder(false);
			org.jdom.Document document = builder.build(is);
			System.out.println("Sending:-->-->-->-->-->\n" + OUTPUT.outputString(document));
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] dataBytes = null;
		try {
			dataBytes = data.getBytes("UTF-8");
			this.out.write(dataBytes.length >> 24);
			this.out.write(dataBytes.length >> 16);
			this.out.write(dataBytes.length >> 8);
			this.out.write(dataBytes.length);
			this.out.write(dataBytes);
			this.out.flush();
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return true;

	}

	public static void readInto(InputStream in, byte[] data, int offset,
			int length) throws IOException, ArrayIndexOutOfBoundsException,
			IllegalArgumentException {
		if (offset + length > data.length) {
			throw new ArrayIndexOutOfBoundsException(offset + length);
		}
		if (offset < 0) {
			throw new ArrayIndexOutOfBoundsException(offset);
		}
		if (length < 0) {
			throw new IllegalArgumentException(
					"length cannot be less than zero");
		}
		int end = offset + length;
		int position = offset;
		while (position < end) {
			int readThisTime = in.read(data, position, end - position);
			if (readThisTime < 0) {
				throw new IOException(
						"Unexpected end of stream while waiting for "
								+ (end - position) + " of " + length + " bytes");
			}
			position += readThisTime;
		}
	}

	public static byte[] read(InputStream in, int targetLength)
			throws IOException, IllegalArgumentException {
		if (targetLength < 0) {
			throw new IllegalArgumentException(
					"length cannot be less than zero");
		}
		try {
			byte[] buffer = new byte[targetLength];
			readInto(in, buffer, 0, targetLength);
			return buffer;
		} catch (OutOfMemoryError e) {
			throw new IOException("Cannot allocate " + targetLength + " bytes");
		}
	}

	public String readFile(String filePath) throws IOException {

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
	  
	public static void main(String[] args) throws Exception {
		TestGway tg = new TestGway();
		tg.openConnection();

		// 读取文件
		try {
			String file = "C:\\git\\hb-pospoc-mule\\argos-api\\src\\main\\api\\argos-biz\\argos-payment\\Examples\\create-tender.xml";
			String file1 = "C:\\git\\hb-pospoc-mule\\argos-api\\src\\main\\api\\argos-biz\\argos-payment\\Examples\\Acknowledgement.xml";
			String file2 = "C:\\git\\hb-pospoc-mule\\argos-api\\src\\main\\api\\argos-biz\\argos-payment\\Examples\\ReceiptPrinted.xml";
			SimpleDateFormat time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String TimeString = time.format(new java.util.Date());
			//System.out.println(TimeString);
			tg.send(tg.readFile(file));
			
			while (true) {
				
				if (tg.getRequestId() != null && tg.isPrintFlag()) {
					String printStr = tg.readFile(file2);
					org.jdom.Document document = tg.updateRequestId(printStr, tg.getRequestId());	
					Element rootElement = document.getRootElement();
					Element editElement = rootElement.getChild("ReceiptPrinted", G8);
					if(editElement!=null){
						editElement.getChild("TenderId", G8).setText(tg.getTenderId());
					}else{						
						System.out.println(editElement);	
					}
					tg.send(new XMLOutputter().outputString(document));
					break;
				}else if(tg.isCompleteFlag()){					
					break;
				}
			}
				
			while (true) {
				
				if (tg.getRequestId() != null && tg.isCompleteFlag()) {					
					String ack = tg.readFile(file1);
					org.jdom.Document document = tg.updateRequestId(ack, tg.getRequestId());					
                    tg.send(new XMLOutputter().outputString(document));
                    break;
				}
				
				
			}

		} catch (Exception e) {
		}
	}
	
	
	public org.jdom.Document  updateRequestId(String xml, String reqId) throws JDOMException, IOException{
									
		ByteArrayInputStream xmlStr = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		SAXBuilder builder = new SAXBuilder(false);
		org.jdom.Document document = builder.build(xmlStr);
		Element rootElement = document.getRootElement();
		Element editElement = rootElement.getChild("RequestId", G8);
		if(editElement!=null){
		  editElement.setText(reqId);
		}else{						
			System.out.println(editElement);	
		}
		return document;
	}
	
	
	

	public String getTenderId() {
		return tenderId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public boolean isPrintFlag() {
		return printFlag;
	}

	public void setPrintFlag(boolean printFlag) {
		this.printFlag = printFlag;
	}

	public boolean isCompleteFlag() {
		return completeFlag;
	}

	public void setCompleteFlag(boolean completeFlag) {
		this.completeFlag = completeFlag;
	}

	public void setTenderId(String tenderId) {
		this.tenderId = tenderId;
	}

	
	
	
}