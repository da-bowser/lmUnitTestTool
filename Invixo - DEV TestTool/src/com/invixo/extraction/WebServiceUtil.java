package com.invixo.extraction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.HttpException;
import com.invixo.common.util.HttpHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.main.special.GlobalParameters;

public class WebServiceUtil {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = WebServiceUtil.class.getName();
	private static final String ENDPOINT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");
	
	
	/**
	 * Call service: GetMessageBytesJavaLangStringIntBoolean
	 * @param messageKey
	 * @param version
	 * @return
	 * @throws ExtractorException
	 * @throws HttpException
	 * @throws IOException
	 */
	public static String lookupSapXiMessage(String messageKey, int version) throws ExtractorException, HttpException, IOException {
		final String SIGNATURE = "lookupSapXiMessage(String, int)";
		
		// Build request payload (service: getMessageBytesJavaLangStringIntBoolean)
		InputStream wsRequest = createRequestGetMessageBytesJavaLangStringIntBoolean(messageKey, version);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service request payload created for Message Key " + messageKey + " with version " + version);
		
		// Call Web Service fetching the SAP XI Message (XI header and payload)
		byte[] wsResponse = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, wsRequest.readAllBytes());
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service called");

		// Extract base64 encoded message from Web Service response
		String base64EncodedMessage = extractEncodedPayload(wsResponse);
		return base64EncodedMessage;
	}
	
	
	/**
	 * Call service: GetMessagesByIDs
	 * @param messageId
	 * @param icoName
	 * @return
	 * @throws HttpException
	 * @throws ExtractorException
	 */
	public static String lookupMessageKey(String messageId, String icoName) throws HttpException, ExtractorException {
		final String SIGNATURE = "lookupMessageKey(String, String)";
		
		// Create "GetMessagesByIDs" request
		byte[] getMessageByIdsRequestBytes = createRequestGetMessagesByIDs(messageId);
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetMessagesByIDs", true, icoName, "xml");
			Util.writeFileToFileSystem(file, getMessageByIdsRequestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> MultiMapping scenario: GetMessagesByIDs request message to be sent to SAP PO is stored here: " + file);
		}
		
		// Call web service (GetMessagesByIDs)
		byte[] getMessageByIdsResponseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, getMessageByIdsRequestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessagesByIDs) called");
		
		// Extract messageKey from response
		String messageKey = extractMessageKeyFromResponse(getMessageByIdsResponseBytes);
		return messageKey;
	}

	
	/**
	 * Call service: GetPredecessorMessageId
	 * @param messageId
	 * @param icoName
	 * @return
	 * @throws HttpException
	 * @throws ExtractorException
	 */
	static String lookupPredecessorMessageId(String messageId, String icoName) throws HttpException, ExtractorException {
		final String SIGNATURE = "lookupPredecessorMessageId(String, String)";
		
		// Create "GetPredecessorMessageId" request
		byte[] getMessagesWithPredecessorsRequestBytes = createRequestGetPredecessorMessageId(messageId);
		logger.writeDebug(LOCATION, SIGNATURE, "GetPredecessorMessageId request created");
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetPredecessorMessageId", true, icoName, "xml");
			Util.writeFileToFileSystem(file, getMessagesWithPredecessorsRequestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> MultiMapping scenario: GetPredecessorMessageId request message to be sent to SAP PO is stored here: " + file);
		}
					
		// Call web service (GetPredecessorMessageId)
		byte[] getPredecessorMessageIdResponseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, getMessagesWithPredecessorsRequestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetPredecessorMessageId) called");
		
		// Extract parentId from response
		String parentId = extractPredecessorIdFromResponse(getPredecessorMessageIdResponseBytes);
		return parentId;
	}

	
	static byte[] lookupSuccessorsBatch(ArrayList<String> messageIdList, String icoName) throws HttpException {
		final String SIGNATURE = "lookupSuccessorsBatch(ArrayList<String>, String)";
		
		// Create request for GetMessagesWithSuccessors
		byte[] requestBytes = createRequestGetMessagesWithSuccessors(messageIdList);
		logger.writeDebug(LOCATION, SIGNATURE, "GetMessagesWithSuccessors request created");
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetMessagesWithSuccessors", true, icoName, "xml");
			Util.writeFileToFileSystem(file, requestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> GetMessagesWithSuccessors request message to be sent to SAP PO is stored here: " + file);
		}
					
		// Call web service (GetMessagesWithSuccessors)
		byte[] responseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, requestBytes);		
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessagesWithSuccessors) called");	
		
		return responseBytes;
	}
		
	
	
	/**
	 * Extract Predecessor from GetPredecessorMessageId response.
	 * @param responseBytes
	 * @return
	 * @throws ExtractorException
	 */
	private static String extractPredecessorIdFromResponse(byte[] responseBytes) throws ExtractorException {
		final String SIGNATURE = "extractPredecessorIdFromResponse(byte[])";
		try {
	        String predecessorId = "";
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("Response".equals(currentElementName)) {
						predecessorId = eventReader.peek().asCharacters().getData();
					}
					break;
				}
			}
			
			// Return parentId found in response
			return predecessorId;
		} catch (XMLStreamException e) {
			String msg = "Error extracting parentIds from 'GetMessagesWithSuccessors' Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	
	
	
	private static String extractEncodedPayload(byte[] fileContent) throws ExtractorException {
		final String SIGNATURE = "extractEncodedPayload(byte[])";
		boolean fetchData = false;
		try {
			String response = "";
			XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(new ByteArrayInputStream(fileContent));
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().getLocalPart();
			    	if ("Response".equals(currentElementName)) {
			    		fetchData = true;
			    	}
			    	break;
			    case XMLStreamConstants.CHARACTERS:
			    	if (event.isCharacters() && fetchData) {		    	
				    	response += event.asCharacters().getData();
			    	}
			    	break;
			    case XMLStreamConstants.END_ELEMENT:
			    	if (fetchData) {
				    	fetchData = false;
			    	}
			    }
			}
			return response;
		} catch (XMLStreamException e) {
			String msg = "Error extracting encoded (base64) payload from response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		} 
	}
	
	
	/**
	 * Create a new XML Request based on the specified parameters matching Web Service method GetMessageBytesJavaLangStringIntBoolean. 
	 * @param messageKey
	 * @param version
	 * @return
	 * @throws ExtractorException
	 */
	static ByteArrayInputStream createRequestGetMessageBytesJavaLangStringIntBoolean(String messageKey, int version) throws ExtractorException {
		final String SIGNATURE = "createRequestGetMessageBytesJavaLangStringIntBoolean(String, int)";
		try {
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create SOAP Envelope start element
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			
			// Add namespaces to start element
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace("urn", "urn:AdapterMessageMonitoringVi");

			// Add SOAP Body start element
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			xmlWriter.writeStartElement("urn", "getMessageBytesJavaLangStringIntBoolean", "urn:AdapterMessageMonitoringVi");

			// Create element: messageKey
			xmlWriter.writeStartElement("urn", "messageKey", "urn:AdapterMessageMonitoringVi");
			xmlWriter.writeCharacters(messageKey);
			xmlWriter.writeEndElement();

			// Create element: version
			xmlWriter.writeStartElement("urn", "version", "urn:AdapterMessageMonitoringVi");
			xmlWriter.writeCharacters("" + version);
			xmlWriter.writeEndElement();

			// Create element: archive
			xmlWriter.writeStartElement("urn", "archive", "urn:AdapterMessageMonitoringVi");
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();

			// Close tags
			xmlWriter.writeEndElement(); // getMessageBytesJavaLangStringIntBoolean
			xmlWriter.writeEndElement(); // SOAP_ENV_BODY
			xmlWriter.writeEndElement(); // SOAP_ENV_ROOT

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			
			// Write to inputstream
			ByteArrayInputStream bais = new ByteArrayInputStream(stringWriter.toString().getBytes());

			return bais;
		} catch (XMLStreamException e) {
			String msg = "Error creating request payload for messageKey: " + messageKey + " with version " + version;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	

	/**
	 * Extract messageKey from GetMessagesByIDs response.
	 * @param responseBytes
	 * @return
	 * @throws ExtractorException
	 */
	static String extractMessageKeyFromResponse(byte[] responseBytes) throws ExtractorException {
		final String SIGNATURE = "extractMessageKeyFromResponse(byte[])";
		try {
	        String messageKey = "";
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("messageKey".equals(currentElementName)) {
						messageKey = eventReader.peek().asCharacters().getData();
					}
					break;
				}
			}
			
			return messageKey;
		} catch (XMLStreamException e) {
			String msg = "Error extracting messageKey from 'GetMessagesByIDs' Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}	
		
	
	/**
	 * Extract successors message keys from Web Service response.
	 * @param responseBytes					XML to extract data from 
	 * @param senderInterface
	 * @param receiverInterface
	 * @return								Map of <MsgKey, Parent MsgId>
	 * @throws ExtractorException
	 */
	static HashMap<String, String> extractSuccessorsBatch(byte[] responseBytes, String senderInterface, String receiverInterface) throws ExtractorException {
		final String SIGNATURE = "extractSuccessorsBatch(byte[], String, String)";
		try {
			HashMap<String, String> successors = new HashMap<String, String>();
	        String parentId = null;
	        String messageKey = null;
	        boolean hasRoot = false;
	        boolean receiverInterfaceElementFound = false;
	        boolean matchingReceiverInterfaceNameFound = false;
	        String currentSenderInterface = null;
	        String status = null;
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("AdapterFrameworkData".equals(currentElementName)) {
				        messageKey = null;
				        parentId = null;
				        matchingReceiverInterfaceNameFound = false;
				        receiverInterfaceElementFound = false;
				        hasRoot = false;
				        currentSenderInterface = null;
				        status = null;

					} else if ("status".equals(currentElementName)) {
						status = eventReader.peek().asCharacters().getData();
						
					} else if ("messageKey".equals(currentElementName)) {
						messageKey = eventReader.peek().asCharacters().getData();
						
					} else if ("parentID".equals(currentElementName)) {
						parentId = eventReader.peek().asCharacters().getData();

					} else if ("rootID".equals(currentElementName)) {
						hasRoot = true;

			    	} else if ("receiverInterface".equals(currentElementName)) {
			    		// We found the correct element
			    		receiverInterfaceElementFound = true;
			    		
			    	} else if("name".equals(currentElementName) && eventReader.peek().isCharacters() && receiverInterfaceElementFound) {
			    		String name = eventReader.peek().asCharacters().getData();
	
			    		// REASON: In case of message split we get all interfaces in the response payload
			    		// we only want the ones matching the Outbound or Inbound interfaces of the current ICO being processed.
			    		// Both outbound and inbound is required to track a FIRST message id to its related LAST messages which
			    		// is needed in a batch situation where multiple FIRST message ids are sent in the request.
			    		if ((name.equals(receiverInterface) || name.equals(senderInterface)) && receiverInterfaceElementFound) {
			    			// We found a match we want to add to our "splitMessageIds" map
			    			matchingReceiverInterfaceNameFound = true;
			    			
			    			// We are no longer interested in more data before next iteration
							receiverInterfaceElementFound = false;

							// Set interface name
							currentSenderInterface = eventReader.peek().asCharacters().getData();	// Sender and Receiver interface are always the same in WS response
						}
			    	}
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					String currentEndElementName = event.asEndElement().getName().getLocalPart();
					
					if ("AdapterFrameworkData".equals(currentEndElementName) && matchingReceiverInterfaceNameFound) {
											
						if (hasRoot && currentSenderInterface.equals(senderInterface)) {
							// In case of unfinished, positive processing status entries are required at later stage
							// so we add them and cross our fingers that later code handles everything correct !!!!!!!!!!!!!!!!!! NOT SO NICE
							// NOT IN FAVOUR OF THIS HORRIBLE CODE. DIS B QUIK "FIX", YES!
							if (!status.equals("success") && !status.equals("logVersion")) {
								successors.put(messageKey, parentId + "#" + status);	
							}
						} else {
							successors.put(messageKey, parentId + "#" + status);
						}
					}
					break;
				}
			}
			
			return successors;
		} catch (XMLStreamException e) {
			String msg = "Error extracting successors from Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}

	
	/**
	 * Create request message for GetMessagesWithSuccessors
	 * @param messageIds			List of Message IDs to get message details from. Map(key, value) = Map(original extract message id, inject message id)
	 * @return
	 */
	static byte[] createRequestGetMessagesWithSuccessors(Collection<String> messageIds) {
		final String SIGNATURE = "createRequestGetMessagesWithSuccessors(Collection<String>)";
		try {
			final String XML_NS_URN_PREFIX	= "urn";
			final String XML_NS_URN_NS		= "urn:AdapterMessageMonitoringVi";
			final String XML_NS_LANG_PREFIX	= "lang";
			final String XML_NS_LANG_NS		= "java/lang";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_URN_PREFIX, XML_NS_URN_NS);
			xmlWriter.writeNamespace(XML_NS_LANG_PREFIX, XML_NS_LANG_NS);

			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | getMessagesWithSuccessors
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "getMessagesWithSuccessors", XML_NS_URN_NS);

			// Create element: Envelope | Body | getMessagesWithSuccessors | messageIds
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "messageIds", XML_NS_URN_NS);

			// Add (inject) message id's to XML
	        for (String messageId : messageIds) {
				// Create element: Envelope | Body | getMessagesWithSuccessors | messageIds | String
				xmlWriter.writeStartElement(XML_NS_LANG_PREFIX, "String", XML_NS_LANG_NS);				
				xmlWriter.writeCharacters(messageId);
		        xmlWriter.writeEndElement();
	        }			
	        
	        // Close element: Envelope | Body | getMessagesWithSuccessors | messageIds
	        xmlWriter.writeEndElement();
	        
			// Create element: Envelope | Body | getMessagesWithSuccessors | archive
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "archive", XML_NS_URN_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();
			
			// Close tags
	        xmlWriter.writeEndElement(); // Envelope | Body | getMessagesWithSuccessors
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for GetMessagesWithSuccessors. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

	
	/**
	 * Create request message for GetPredecessorMessageId
	 * @param messageId			Message ID to get message details from.
	 * @return
	 */
	private static byte[] createRequestGetPredecessorMessageId(String messageId) {
		final String SIGNATURE = "createRequestGetMessagesWithSuccessors(Collection<String>)";
		try {
			final String XML_NS_URN_PREFIX	= "urn";
			final String XML_NS_URN_NS		= "urn:AdapterMessageMonitoringVi";
			final String XML_NS_LANG_PREFIX	= "lang";
			final String XML_NS_LANG_NS		= "java/lang";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_URN_PREFIX, XML_NS_URN_NS);
			xmlWriter.writeNamespace(XML_NS_LANG_PREFIX, XML_NS_LANG_NS);

			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | getPredecessorMessageId
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "getPredecessorMessageId", XML_NS_URN_NS);

			// Create element: Envelope | Body | getPredecessorMessageId | messageId
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "messageId", XML_NS_URN_NS);
			xmlWriter.writeCharacters(messageId);
	        xmlWriter.writeEndElement();			
	                
			// Create element: Envelope | Body | getPredecessorMessageId | direction
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "direction", XML_NS_URN_NS);
			xmlWriter.writeCharacters("OUTBOUND");
			xmlWriter.writeEndElement();
			
			// Close tags
	        xmlWriter.writeEndElement(); // Envelope | Body | getPredecessorMessageId
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for GetPredecessorMessageId. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	

	/**
	 * Create request message for GetMessagesByIDsRequest
	 * @param messageId
	 * @return
	 */
	private static byte[] createRequestGetMessagesByIDs(String messageId) {
		final String SIGNATURE = "createRequestGetMessagesByIDs(String)";
		try {
			final String XML_NS_URN_PREFIX	= "urn";
			final String XML_NS_URN_NS		= "urn:AdapterMessageMonitoringVi";
			final String XML_NS_LANG_PREFIX	= "lang";
			final String XML_NS_LANG_NS		= "java/lang";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_URN_PREFIX, XML_NS_URN_NS);
			xmlWriter.writeNamespace(XML_NS_LANG_PREFIX, XML_NS_LANG_NS);

			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | getMessagesByIDs
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "getMessagesByIDs", XML_NS_URN_NS);

			// Create element: Envelope | Body | getMessagesByIDs | messageIds
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "messageIds", XML_NS_URN_NS);

			// Create element: Envelope | Body | getMessagesByIDs | messageIds | String
			xmlWriter.writeStartElement(XML_NS_LANG_PREFIX, "String", XML_NS_LANG_NS);				
			xmlWriter.writeCharacters(messageId);
	        xmlWriter.writeEndElement();			
	        
	        xmlWriter.writeEndElement(); // Close element: Envelope | Body | getMessagesByIDs | messageIds
	        
			// Create element: Envelope | Body | getMessagesByIDs | referenceIds
	        xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "referenceIds", XML_NS_URN_NS);
	        xmlWriter.writeEndElement(); // Close element:  Envelope | Body | getMessagesByIDs | referenceIds
	        
			// Create element: Envelope | Body | getMessagesByIDs | correlationIds
	        xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "correlationIds", XML_NS_URN_NS);
	        xmlWriter.writeEndElement(); // Close element:  Envelope | Body | getMessagesByIDs | correlationIds
	        
			// Create element: Envelope | Body | getMessagesByIDs | archive
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "archive", XML_NS_URN_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();
			
			// Close tags
	        xmlWriter.writeEndElement(); // Envelope | Body | getMessagesByIDs
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for getMessagesByIDs. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

}
