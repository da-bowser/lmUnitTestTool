package com.invixo.main.special;

import org.apache.http.entity.ContentType;

import com.invixo.common.util.PropertyAccessor;

public class GlobalParameters {
	public static final String ENCODING 					= PropertyAccessor.getProperty("ENCODING");
	public static final boolean DEBUG 						= Boolean.parseBoolean(PropertyAccessor.getProperty("DEBUG"));
	public static final String FILE_DELIMITER 				= "#";
	public static final ContentType CONTENT_TYPE_TEXT_XML 	= ContentType.TEXT_XML.withCharset(ENCODING);
	public static final ContentType CONTENT_TYPE_APP_XML 	= ContentType.APPLICATION_XML.withCharset(ENCODING);
	
	
	// Parameter: base directory for all reading and writing to/from file system
	public static String PARAM_VAL_BASE_DIR 				= null;
	
	// SAP PO user/password
	public static String CREDENTIAL_USER					= null;
	public static String CREDENTIAL_PASS 					= null;
		
	// SAP PO URL PREFIX/START. Example result: http://ipod.invixo.com:50000/
	public static String SAP_PO_HTTP_HOST_AND_PORT			= null;
	
	// SAP PO host and port
	public static String PARAM_VAL_HTTP_HOST				= null;
	public static String PARAM_VAL_HTTP_PORT 				= null;
	
	// Parameter: SAP PO, name of Sender SOAP XI adapter
	public static String PARAM_VAL_XI_SENDER_ADAPTER 		= null;

	// Parameter: SAP PO name of Sender Component containing the SOAP XI adapter
	public static String PARAM_VAL_SENDER_COMPONENT 		= null;

}
