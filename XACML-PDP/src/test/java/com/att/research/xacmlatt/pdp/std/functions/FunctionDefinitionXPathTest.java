package com.att.research.xacmlatt.pdp.std.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdRequest;
import com.att.research.xacml.std.StdStatus;
import com.att.research.xacml.std.datatypes.DataTypes;
import com.att.research.xacml.std.datatypes.XPathExpressionWrapper;
import com.att.research.xacml.std.dom.DOMRequest;
import com.att.research.xacmlatt.pdp.policy.ExpressionResult;
import com.att.research.xacmlatt.pdp.policy.FunctionArgument;
import com.att.research.xacmlatt.pdp.policy.FunctionArgumentAttributeValue;
import com.att.research.xacmlatt.pdp.std.StdEvaluationContext;
import com.att.research.xacmlatt.pdp.std.StdFunctions;

/**
 * Test of PDP Functions (See XACML core spec section A.3)
 * 
 * TO RUN - use jUnit
 * In Eclipse select this file or the enclosing directory, right-click and select Run As/JUnit Test
 * 
 * @author glenngriffin
 *
 */
public class FunctionDefinitionXPathTest {
	
	//
	// Strings for the Request contents
	//
	
	String reqStrMainStart = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" 
			+ "<Request xsi:schemaLocation=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17" 
			+ " http://docs.oasis-open.org/xacml/3.0/xacml-core-v3-schema-wd-17.xsd\"" 
			+ " ReturnPolicyIdList=\"false\""
			+ " CombinedDecision=\"false\""
			+ " xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\""
			+ " xmlns:md=\"http://www.medico.com/schemas/record\""
			+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
			+ "	<Attributes Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\">"
			+ "		<Attribute IncludeInResult=\"false\" AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\">"
			+ "			<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">Julius Hibbert</AttributeValue>"
			+ "		</Attribute>"
			+ "		<Attribute IncludeInResult=\"false\" AttributeId=\"urn:oasis:names:tc:xacml:2.0:conformance-test:test-attr\">"
			+ "			<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">   This  is IT!  </AttributeValue>"
			+ "		</Attribute>"
			+ "		<Attribute IncludeInResult=\"false\" AttributeId=\"urn:oasis:names:tc:xacml:2.0:conformance-test:test-attr\">"
			+ "			<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">   This  is IT!  </AttributeValue>"
			+ "		</Attribute>"
			+ "</Attributes>";
	      
	String reqStrResourceStart =   "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">";

	String reqStrContentMdRecord =
		" <Content>" +
			"<md:record>" +
                "<md:hospital_info>" +
                   "<md:name>ABC Hospital</md:name>" +
                    "<md:department>Surgery</md:department>" +
                "</md:hospital_info>" +
                "<md:patient_info>" +
                    "<md:name>Bart Simpson</md:name>" +
                    "<md:age>60</md:age>" +
                    "<md:sex>male</md:sex>" +
                    "<md:health_insurance>123456</md:health_insurance>" +
                "</md:patient_info>" +
                "<md:diagnosis_info>" +
                    "<md:diagnosis>" +
                        "<md:item type=\"primary\">Gastric Cancer</md:item>" +
                        "<md:item type=\"secondary\">Hyper tension</md:item>" +
                    "</md:diagnosis>" +
                    "<md:pathological_diagnosis>" +
                        "<md:diagnosis>" +
                            "<md:item type=\"primary\">Well differentiated adeno carcinoma</md:item>" +
                        "</md:diagnosis>" +
                        "<md:date>2000-10-05</md:date>" +
                        "<md:malignancy type=\"yes\"/>" +
                    "</md:pathological_diagnosis>" +
                "</md:diagnosis_info>  " +             
           " </md:record>" +
         "</Content>";
	String reqStrMalformedContent = 
			" <Content>" +
					"<md:record>" +
		                "<md:hospital_info>" +
		                   "<md:name>ABC Hospital</md:name>" +
		                        "<md:malignancy type=\"yes\"/>" +
		    "</Content>";
	String reqStrResourceEnd = "    <Attribute IncludeInResult=\"false\" AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\">"
			+ "		<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">http://medico.com/record/patient/BartSimpson</AttributeValue>"
			+ "  </Attribute>"
			+ "</Attributes> ";
	String reqStrActionStart =   "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\">";

	String reqStrActionEnd = "<Attribute IncludeInResult=\"false\" AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\">"
			+ "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</AttributeValue>"
			+ "</Attribute>"
			+ "</Attributes> ";
	String reqStrEnvironmentStartEnd = "  <Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\" />";
	String reqStrMainEnd = " </Request>";

	
	// combined strings for convenience
	String reqStrMainResourceStart = reqStrMainStart + reqStrResourceStart;
	String reqStrResourceAllEnd = reqStrResourceEnd + reqStrActionStart + reqStrActionEnd + reqStrEnvironmentStartEnd + reqStrMainEnd;
	
	
	/*
	 * variables useful in the following tests
	 */
	List<FunctionArgument> arguments = new ArrayList<FunctionArgument>();
	
	
	
	// Name Spaces used in the XML as part of these examples (copied from the Conformance tests) - needed for compiling XPaths
	NamespaceContext nameSpaceContext = new NamespaceContext() {
	    @Override
	    public Iterator<String> getPrefixes(String arg0) { return null;}

	    @Override
	    public String getPrefix(String arg0) {return null;}

	    @Override
	    public String getNamespaceURI(String arg0) {
	        if("md".equals(arg0)) {
	            return "http://www.medico.com/schemas/record";
	        } else if ("xacml-context".equals(arg0)) {
	        	return "urn:oasis:names:tc:xacml:3.0:context:schema:os";
	        } else if ("xsi".equals(arg0)) {
	        	return "http://www.w3.org/2001/XMLSchema-instance";
	        }
	        return null;
	    }
	};
	
	
	
	//
	// XPath Function Attributes available for use in tests  (representing appropriate fragment of Policy)
	//
	
	FunctionArgumentAttributeValue attrXnull = null;
	FunctionArgumentAttributeValue attrXEmpty = null;
	FunctionArgumentAttributeValue attrXNoCategory = null;
	FunctionArgumentAttributeValue attrXNoValue = null;
	FunctionArgumentAttributeValue attrXSlashSlashMdRecord = null;
	FunctionArgumentAttributeValue attrXSlashSlashStar = null;
	FunctionArgumentAttributeValue attrXSlashSlashMdName = null;
	FunctionArgumentAttributeValue attrXSlashSlashMdMalignancy = null;
	FunctionArgumentAttributeValue attrXNotInRequest = null;
	FunctionArgumentAttributeValue attrXSlashSlashMdRecordSlashStar = null;
	FunctionArgumentAttributeValue attrXMdPatientInfo = null;
	
	FunctionArgumentAttributeValue attrBadType = null;

	// String version of attrs for use in Deprecated functions
	FunctionArgumentAttributeValue attrStrnull = null;
	FunctionArgumentAttributeValue attrStrEmpty = null;
	FunctionArgumentAttributeValue attrStrNoCategory = null;
	FunctionArgumentAttributeValue attrStrNoValue = null;
	FunctionArgumentAttributeValue attrStrSlashSlashMdRecord = null;
	FunctionArgumentAttributeValue attrStrSlashSlashStar = null;
	FunctionArgumentAttributeValue attrStrSlashSlashMdName = null;
	FunctionArgumentAttributeValue attrStrSlashSlashMdMalignancy = null;
	FunctionArgumentAttributeValue attrStrNotInRequest = null;
	FunctionArgumentAttributeValue attrStrSlashSlashMdRecordSlashStar = null;
	FunctionArgumentAttributeValue attrStrMdPatientInfo = null;
	
	
	//
	// REQUEST objects available for use in tests
	//
	Request requestEmpty = new StdRequest(StdStatus.STATUS_OK);
	Request requestMdRecord = null;
	Request requestDoubleResources = null;
	Request requestResourceActionContent = null;
	Request requestContentInAction = null;


	
	
	/**
	 * Set up all variables in one place because it is complicated (lots of steps needed for each attribute)
	 */
	public FunctionDefinitionXPathTest() {
		try {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xpath = xPathFactory.newXPath();
			xpath.setNamespaceContext(nameSpaceContext);
			
			// Create XPaths to use in expressions
			XPathExpressionWrapper xEmpty = new XPathExpressionWrapper("");
			XPathExpressionWrapper xSlashSlashMdRecord = new XPathExpressionWrapper(xpath.compile("//md:record"));
			XPathExpressionWrapper xSlashSlashStar = new XPathExpressionWrapper(xpath.compile("//*"));
			XPathExpressionWrapper xSlashSlashMdName = new XPathExpressionWrapper(xpath.compile("//md:name"));
			XPathExpressionWrapper xSlashSlashMdMalignancy = new XPathExpressionWrapper(xpath.compile("//md:malignancy"));
			XPathExpressionWrapper xNotInRequest = new XPathExpressionWrapper(xpath.compile("value_Not_in_request"));
			XPathExpressionWrapper xSlashSlashMdRecordSlashStar = new XPathExpressionWrapper(xpath.compile("//md:record/*"));
			XPathExpressionWrapper xMdPatientInfo = new XPathExpressionWrapper(xpath.compile("md:patient_info"));

	
			
			// create Function Attributes out of the XPathExpressions
			attrXnull = new FunctionArgumentAttributeValue(null);
			attrXEmpty = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xEmpty));
			attrXNoCategory = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xSlashSlashMdRecord));
			attrXNoValue = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xEmpty,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrXSlashSlashMdRecord = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xSlashSlashMdRecord,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrXSlashSlashStar = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xSlashSlashStar,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrXSlashSlashMdName = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xSlashSlashMdName,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrXSlashSlashMdMalignancy = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xSlashSlashMdMalignancy,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrXNotInRequest = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xNotInRequest,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			
			attrXSlashSlashMdRecordSlashStar = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xSlashSlashMdRecordSlashStar,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrXMdPatientInfo = new FunctionArgumentAttributeValue(DataTypes.DT_XPATHEXPRESSION.createAttributeValue(xMdPatientInfo,
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));	
			
			
			// Deprecated versions of args
			attrStrnull = new FunctionArgumentAttributeValue(null);
			attrStrEmpty = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue(""));
			attrStrNoCategory = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("//md:record"));
			attrStrNoValue = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrStrSlashSlashMdRecord = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("//md:record",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrStrSlashSlashStar = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("//*",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrStrSlashSlashMdName = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("//md:name",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrStrSlashSlashMdMalignancy = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("//md:malignancy",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrStrNotInRequest = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("value_Not_in_request",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			
			attrStrSlashSlashMdRecordSlashStar = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("//md:record/*",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));
			attrStrMdPatientInfo = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("md:patient_info",
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource" )));	
			
			
			
			
			attrBadType = new FunctionArgumentAttributeValue(DataTypes.DT_STRING.createAttributeValue("some string"));
		
			
			
			// Request objects
			// to create a Request object the easiest way is to put the xml into a file and use the DOMRequest to load it.
			
			// single Content in the Resources section (normal valid request)
			String reqString = reqStrMainResourceStart + reqStrContentMdRecord + reqStrResourceAllEnd;
				File tFile = File.createTempFile("functionJunit", "request");
				BufferedWriter bw = new BufferedWriter(new FileWriter(tFile));
				bw.append(reqString);
				bw.flush();
				bw.close();
			requestMdRecord = DOMRequest.load(tFile);
				tFile.delete();
				
			// Resources included twice
			reqString = reqStrMainResourceStart + reqStrContentMdRecord + reqStrResourceEnd + reqStrResourceStart + reqStrContentMdRecord +reqStrResourceAllEnd;
				tFile = File.createTempFile("functionJunit", "request");
				bw = new BufferedWriter(new FileWriter(tFile));
				bw.append(reqString);
				bw.flush();
				bw.close();
			requestDoubleResources = DOMRequest.load(tFile);
				tFile.delete();
					
				
			// content included in both Resource and Action - ok
			reqString = reqStrMainResourceStart + reqStrContentMdRecord + reqStrResourceEnd + reqStrActionStart + reqStrContentMdRecord + reqStrActionEnd + reqStrMainEnd;
				tFile = File.createTempFile("functionJunit", "request");
				bw = new BufferedWriter(new FileWriter(tFile));
				bw.append(reqString);
				bw.flush();
				bw.close();	
			requestResourceActionContent = DOMRequest.load(tFile);
				tFile.delete();
				
			// Content included only in Action - missing content produces non-error result according to spec
			reqString = reqStrMainResourceStart + reqStrResourceEnd + reqStrActionStart + reqStrContentMdRecord + reqStrActionEnd + reqStrMainEnd;
				tFile = File.createTempFile("functionJunit", "request");
				bw = new BufferedWriter(new FileWriter(tFile));
				bw.append(reqString);
				bw.flush();
				bw.close();		
			requestContentInAction = DOMRequest.load(tFile);
				tFile.delete();
			
				
				
			// Test that Bad XML is caught
			@SuppressWarnings("unused")
			Request requestContentMisplaced = null;
			@SuppressWarnings("unused")
			Request requestMalformedContent = null;
			@SuppressWarnings("unused")
			Request requestDoubleContent = null;

			
			// Content included twice - error
			reqString = reqStrMainResourceStart + reqStrContentMdRecord + reqStrContentMdRecord +reqStrResourceAllEnd;
				tFile = File.createTempFile("functionJunit", "request");
				bw = new BufferedWriter(new FileWriter(tFile));
				bw.append(reqString);
				bw.flush();
				bw.close();
			try {
				requestDoubleContent = DOMRequest.load(tFile);
					tFile.delete();
			} catch (com.att.research.xacml.std.dom.DOMStructureException e) {
				// this is what it should do, so just continue
			} catch (Exception e) {
				fail("Unexpected exception for bad XML, e="+e);
			}	
				
			// Bad XML - Content not under a Category
			reqString = reqStrMainStart + reqStrContentMdRecord + reqStrResourceStart + reqStrResourceEnd + reqStrActionStart + reqStrActionEnd + reqStrMainEnd;
				tFile = File.createTempFile("functionJunit", "request");
				bw = new BufferedWriter(new FileWriter(tFile));
				bw.append(reqString);
				bw.flush();
				bw.close();
			try {
				requestContentMisplaced = DOMRequest.load(tFile);
					tFile.delete();
			} catch (com.att.research.xacml.std.dom.DOMStructureException e) {
				// this is what it should do, so just continue
			} catch (Exception e) {
				fail("Unexpected exception for bad XML, e="+e);
			}
				
			// Bad XML - Content is not valid XML
			reqString = reqStrMainResourceStart + reqStrMalformedContent + reqStrResourceAllEnd;
				tFile = File.createTempFile("functionJunit", "request");
				bw = new BufferedWriter(new FileWriter(tFile));
				bw.append(reqString);
				bw.flush();
				bw.close();
			try {
				requestMalformedContent = DOMRequest.load(tFile);
					tFile.delete();
			} catch (com.att.research.xacml.std.dom.DOMStructureException e) {
				// this is what it should do, so just continue
			} catch (Exception e) {
				fail("Unexpected exception for bad XML, e="+e);
			}
			
		} catch (Exception e) {
			fail("Constructor initializing variables, e="+ e + "  cause="+e.getCause());
		}
		
	}
	

	
	
	
	
	
	
	@Test
	public void testXpath_node_count() {


		
		FunctionDefinitionXPath<?> fd = (FunctionDefinitionXPath<?>) StdFunctions.FD_XPATH_NODE_COUNT;
		
		// check identity and type of the thing created
		assertEquals(XACML3.ID_FUNCTION_XPATH_NODE_COUNT, fd.getId());
		assertEquals(DataTypes.DT_XPATHEXPRESSION.getId(), fd.getDataTypeArgs().getId());
		assertEquals(DataTypes.DT_INTEGER.getId(), fd.getDataTypeId());
		
		// just to be safe...  If tests take too long these can probably be eliminated
		assertFalse(fd.returnsBag());
		assertEquals(new Integer(1), fd.getNumArgs());
		

		// match all elements within context
		arguments.clear();
		arguments.add(attrXSlashSlashStar);
		ExpressionResult res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		BigInteger resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("18"), resValue);
	
		// match exactly 1 element
		arguments.clear();
		arguments.add(attrXSlashSlashMdMalignancy);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("1"), resValue);
		
		// match a few but not all
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("2"), resValue);
		
		
		// verify variables using in other tests: count nodes immediately under md:record
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecordSlashStar);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("3"), resValue);
		
		// verify variables using in other tests: count number of records containing patient_info
		arguments.clear();
		arguments.add(attrXMdPatientInfo);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("1"), resValue);
		
		// verify variables using in other tests: count number of records containing md:name
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("2"), resValue);
		
		// verify variables using in other tests: count number of records containing md:malignancy
		arguments.clear();
		arguments.add(attrXSlashSlashMdMalignancy);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("1"), resValue);
		
		
		
		
		// match no element
		arguments.clear();
		arguments.add(attrXNotInRequest);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("0"), resValue);
		
		// Resources included twice
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestDoubleResources, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count More than one Content section for id 'urn:oasis:names:tc:xacml:3.0:attribute-category:resource'", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:syntax-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		

		// Content in both Resource and Action categories (ok)
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestResourceActionContent, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("2"), resValue);

		// Content only in Action category (missing in Resources -> 0 according to spec)
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestContentInAction, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (BigInteger)res.getValue().getValue();
		assertEquals(new BigInteger("0"), resValue);

		

		
//TODO - any other tests????
		
		// null Evaluation Context
		arguments.clear();
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Got null EvaluationContext", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null Request
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(null, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Got null Request in EvaluationContext", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null attribute
		arguments.clear();
		arguments.add(attrXnull);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Got null attribute at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// no value
		arguments.clear();
		arguments.add(attrXNoValue);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count XPathExpression returned null at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// no category
		arguments.clear();
		arguments.add(attrXNoCategory);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Got null Category at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:syntax-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		

		// too many args
		arguments.clear();
		arguments.add(attrXEmpty);
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Expected 1 arguments, got 2", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// too few args
		arguments.clear();
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Expected 1 arguments, got 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// bad arg type
		arguments.clear();
		arguments.add(attrBadType);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Expected data type 'xpathExpression' saw 'string' at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null args
		arguments.clear();
		arguments.add(null);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-count Got null argument at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
	}

	
	@Test
	public void testXpath_node_equal() {

		
		FunctionDefinitionXPath<?> fd = (FunctionDefinitionXPath<?>) StdFunctions.FD_XPATH_NODE_EQUAL;
		
		// check identity and type of the thing created
		assertEquals(XACML3.ID_FUNCTION_XPATH_NODE_EQUAL, fd.getId());
		assertEquals(DataTypes.DT_XPATHEXPRESSION.getId(), fd.getDataTypeArgs().getId());
		assertEquals(DataTypes.DT_BOOLEAN.getId(), fd.getDataTypeId());
		
		// just to be safe...  If tests take too long these can probably be eliminated
		assertFalse(fd.returnsBag());
		assertEquals(new Integer(2), fd.getNumArgs());
		
		
		// test normal success - exactly the same set
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		ExpressionResult res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		Boolean resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);
		
		// success - second list is subset of first list
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecordSlashStar);
		arguments.add(attrXMdPatientInfo);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);
		
		// success - first list is subset of second list
		arguments.clear();
		arguments.add(attrXMdPatientInfo);
		arguments.add(attrXSlashSlashMdRecordSlashStar);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);
		
		// success - second list contains children of first list
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		// success - first list contains children of second list
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		

		
		// two non-overlapping sets
		arguments.clear();
		arguments.add(attrXSlashSlashMdMalignancy);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		// first list contains nothing
		arguments.clear();
		arguments.add(attrXNotInRequest);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		// second list contains nothing
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXNotInRequest);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);		
		
		
		
//TODO	
		//?????
		///??????? add real tests
		//////
		
		
		// Resources included twice
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestDoubleResources, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal More than one Content section for id 'urn:oasis:names:tc:xacml:3.0:attribute-category:resource'", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:syntax-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		

		// Content in both Resource and Action categories (ok)
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestResourceActionContent, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);

		// Content only in Action category (missing in Resources -> 0 according to spec)
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestContentInAction, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		
		// null Evaluation Context
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Got null EvaluationContext", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null Request
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(null, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Got null Request in EvaluationContext", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null attribute
		arguments.clear();
		arguments.add(attrXnull);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Got null attribute at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXnull);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Got null attribute at arg index 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// no value
		arguments.clear();
		arguments.add(attrXNoValue);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal XPathExpression returned null at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXNoValue);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal XPathExpression returned null at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// no category
		arguments.clear();
		arguments.add(attrXNoCategory);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Got null Category at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:syntax-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXNoCategory);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal XPathExpression returned null at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// too many args
		arguments.clear();
		arguments.add(attrXEmpty);
		arguments.add(attrXEmpty);
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Expected 2 arguments, got 3", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// too few args
		arguments.clear();
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Expected 2 arguments, got 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Expected 2 arguments, got 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
			
		// bad arg type
		arguments.clear();
		arguments.add(attrBadType);
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Expected data type 'xpathExpression' saw 'string' at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXEmpty);
		arguments.add(attrBadType);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Expected data type 'xpathExpression' saw 'string' at arg index 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null args
		arguments.clear();
		arguments.add(attrXEmpty);
		arguments.add(null);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Got null argument at arg index 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(null);
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-equal Got null argument at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
	}
	
	
	
	
	@Test
	public void testXpath_node_match() {

		
		FunctionDefinitionXPath<?> fd = (FunctionDefinitionXPath<?>) StdFunctions.FD_XPATH_NODE_MATCH;
		
		// check identity and type of the thing created
		assertEquals(XACML3.ID_FUNCTION_XPATH_NODE_MATCH, fd.getId());
		assertEquals(DataTypes.DT_XPATHEXPRESSION.getId(), fd.getDataTypeArgs().getId());
		assertEquals(DataTypes.DT_BOOLEAN.getId(), fd.getDataTypeId());
		
		// just to be safe...  If tests take too long these can probably be eliminated
		assertFalse(fd.returnsBag());
		assertEquals(new Integer(2), fd.getNumArgs());
		
		
		// test normal success - exactly the same set
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		ExpressionResult res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		Boolean resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);
		
		// success - second list is subset of first list
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecordSlashStar);
		arguments.add(attrXMdPatientInfo);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);
		
		// success - first list is subset of second list
		arguments.clear();
		arguments.add(attrXMdPatientInfo);
		arguments.add(attrXSlashSlashMdRecordSlashStar);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);
		
		// success - second list contains children of first list
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);
		
		// success - first list contains children of second list
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		
		
		// two non-overlapping sets
		arguments.clear();
		arguments.add(attrXSlashSlashMdMalignancy);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		// first list contains nothing
		arguments.clear();
		arguments.add(attrXNotInRequest);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		// second list contains nothing
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXNotInRequest);
		res = fd.evaluate(new StdEvaluationContext(requestMdRecord, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);	
		
//TODO		
		//?????
		///??????? add real tests
		//////
		
		
		// Resources included twice
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestDoubleResources, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match More than one Content section for id 'urn:oasis:names:tc:xacml:3.0:attribute-category:resource'", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:syntax-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		

		// Content in both Resource and Action categories (ok)
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestResourceActionContent, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertTrue(resValue);

		// Content only in Action category (missing in Resources -> 0 according to spec)
		arguments.clear();
		arguments.add(attrXSlashSlashMdName);
		arguments.add(attrXSlashSlashMdName);
		res = fd.evaluate(new StdEvaluationContext(requestContentInAction, null, null), arguments);
		assertTrue(res.isOk());
		resValue = (Boolean)res.getValue().getValue();
		assertFalse(resValue);
		
		
		// null Evaluation Context
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Got null EvaluationContext", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null Request
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(null, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Got null Request in EvaluationContext", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null attribute
		arguments.clear();
		arguments.add(attrXnull);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Got null attribute at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXnull);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Got null attribute at arg index 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// no value
		arguments.clear();
		arguments.add(attrXNoValue);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match XPathExpression returned null at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXNoValue);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match XPathExpression returned null at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// no category
		arguments.clear();
		arguments.add(attrXNoCategory);
		arguments.add(attrXSlashSlashMdRecord);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Got null Category at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:syntax-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXSlashSlashMdRecord);
		arguments.add(attrXNoCategory);
		res = fd.evaluate(new StdEvaluationContext(requestEmpty, null, null), arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match XPathExpression returned null at index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// too many args
		arguments.clear();
		arguments.add(attrXEmpty);
		arguments.add(attrXEmpty);
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Expected 2 arguments, got 3", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// too few args
		arguments.clear();
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Expected 2 arguments, got 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Expected 2 arguments, got 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
			
		// bad arg type
		arguments.clear();
		arguments.add(attrBadType);
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Expected data type 'xpathExpression' saw 'string' at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(attrXEmpty);
		arguments.add(attrBadType);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Expected data type 'xpathExpression' saw 'string' at arg index 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		// null args
		arguments.clear();
		arguments.add(attrXEmpty);
		arguments.add(null);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Got null argument at arg index 1", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		arguments.clear();
		arguments.add(null);
		arguments.add(attrXEmpty);
		res = fd.evaluate(null, arguments);
		assertFalse(res.getStatus().isOk());
		assertEquals( "function:xpath-node-match Got null argument at arg index 0", res.getStatus().getStatusMessage());
		assertEquals("urn:oasis:names:tc:xacml:1.0:status:processing-error", res.getStatus().getStatusCode().getStatusCodeValue().stringValue());
		
		
		
	}
	

	
	//
	// DEPRECATED versions that use String arguments rather than XPATHEXPRESSIONs
	// are NOT supported due to ambiguity in the semantics between 2.0 (<Request> is root and has only one <Content> in resources)
	//	and 3.0 (<Content> is root and there are multiple <Content> sections in any category)
	//
	

	
}
