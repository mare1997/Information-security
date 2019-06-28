package app;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.keyresolver.implementations.RSAKeyValueResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509CertificateResolver;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import keystore.model.IssuerData;
import keystoreReader.KeyStoreReader;
import support.MailHelper;
import support.MailReader;



public class ReadMail extends MailClient {
	public static long PAGE_SIZE = 3;
	public static boolean ONLY_FIRST_PAGE = true;
	private static final String USER_A_JKS = "./data/usera.jks";
	private static final String USER_B_JKS = "./data/userb.jks";
	private static final String userBAlias = "userb";
	private static final String userAAlias = "usera";
	private static final String userBPass = "123";
	private static final String userAPass = "123";
	
	
	static {
		//staticka inicijalizacija
        Security.addProvider(new BouncyCastleProvider());
        org.apache.xml.security.Init.init();
	}
	
	public static void main(String[] args) throws Exception {
		 // Build a new authorized API client service.
        Gmail service = getGmailService();
        ArrayList<MimeMessage> mimeMessages = new ArrayList<MimeMessage>();
        
        String user = "me";
        String query = "is:unread label:INBOX";
        
        //Izlistavanje prvih PAGE_SIZE mail-ova prve stranice.
        List<Message> messages = MailReader.listMessagesMatchingQuery(service, user, query, PAGE_SIZE, ONLY_FIRST_PAGE);
        for(int i=0; i<messages.size(); i++) {
        	Message fullM = MailReader.getMessage(service, user, messages.get(i).getId());
        	
        	MimeMessage mimeMessage;
			try {
				
				mimeMessage = MailReader.getMimeMessage(service, user, fullM.getId());
				
				System.out.println("\nMessage number " + i);
				System.out.println("From: " + mimeMessage.getHeader("From", null));
				System.out.println("Subject: " + mimeMessage.getSubject());
				System.out.println("Body: " + MailHelper.getText(mimeMessage));
				System.out.println("\n");
				
				mimeMessages.add(mimeMessage);
	        
			} catch (MessagingException e) {
				e.printStackTrace();
			}	
        }
        
      //odabir mail-a od strane korisnika
        System.out.println("Select a message to decrypt:");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        String answerStr = reader.readLine();
	    Integer answer = Integer.parseInt(answerStr);
	    
		MimeMessage chosenMessage = mimeMessages.get(answer);
	    
        //izvlacenje teksta mail-a koji je trenutno u obliku stringa
		String xmlAsString = MailHelper.getText(chosenMessage);
		
		//kreiranje XML dokumenta na osnovu stringa
		Document doc = createXMlDocument(xmlAsString);
				
		Element element = (Element)doc.getElementsByTagName("mail").item(0);
				
		// citanje keystore-a kako bi se izvukao sertifikat primaoca
		// i kako bi se dobio njegov tajni kljuc
		PrivateKey privateKey = getPrivateKey();
		
		//desifrovanje tajnog (session) kljuca pomocu privatnog kljuca
		//xmlcipher.aes_128 dodano
		XMLCipher xmlCipher = XMLCipher.getInstance();
		//Key kek = (Key)xmlCipher.getEncryptedKey();
		xmlCipher.init(XMLCipher.DECRYPT_MODE, null);
		
		//trazi se prvi EncryptedData element i izvrsi dekriptovanje
		EncryptedData encryptedData = xmlCipher.loadEncryptedData(doc, element);
		KeyInfo ki = encryptedData.getKeyInfo();
		EncryptedKey encKey = ki.itemEncryptedKey(0);
		
		XMLCipher keyCipher = XMLCipher.getInstance();
		keyCipher.init(XMLCipher.UNWRAP_MODE, privateKey);
		Key key = keyCipher.decryptKey(encKey,encryptedData.getEncryptionMethod().getAlgorithm());
		xmlCipher.init(XMLCipher.DECRYPT_MODE, key);
		xmlCipher.setKEK(key);
		
		//dekriptuje se
		//pri cemu se prvo dekriptuje tajni kljuc, pa onda njime podaci		
		xmlCipher.doFinal(doc, element,true);
		
		//provera potpisa
		ReadMail verify = new ReadMail();
		verify.verify(doc);
		
		
		System.out.println("\nSubject text: " + doc.getElementsByTagName("subjectMail").item(0).getTextContent());
		System.out.println("Body text: " + doc.getElementsByTagName("bodyMail").item(0).getTextContent());
       
	}
	private static String xmlAsString(Document doc) throws TransformerException{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
		
		return output;
	}
	
	private static Document createXMlDocument(String xmlAsString){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
		factory.setNamespaceAware(true);
		DocumentBuilder builder;  
		Document doc = null;
		try {  
		    builder = factory.newDocumentBuilder();  
		    doc = builder.parse(new InputSource(new StringReader(xmlAsString)));  
		} catch (Exception e) {  
		    e.printStackTrace();  
		} 
		return doc;
	}
	
	// iz sertifikata korisnika B izvuci njegov tajni kljc 
	private static PrivateKey getPrivateKey() {
		KeyStoreReader keyStoreReader = new KeyStoreReader();
		IssuerData issuerData = null;

		try {
			issuerData = keyStoreReader.readKeyStore(USER_B_JKS, userBAlias, userBPass.toCharArray() , userBPass.toCharArray());
			PrivateKey privateKey = issuerData.getPrivateKey();
			return privateKey;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		
		
		return null;
	}
	
	public void verify(Document doc) {
		boolean res = verifySignature(doc);
		System.out.println("\nVerification = " + res);
	}
	
	private static boolean verifySignature(Document doc) {
		try {
			//Pronalazi se prvi Signature element
			NodeList signatures = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
			Element signatureEl = (Element) signatures.item(0);
			
			//kreira se signature objekat od elementa
			XMLSignature signature = new XMLSignature(signatureEl, null);
			
			//preuzima se key info
			KeyInfo keyInfo = signature.getKeyInfo();
			
			//ako postoji
			if(keyInfo != null) {
				//registruju se resolver-i za javni kljuc i sertifikat
				keyInfo.registerInternalKeyResolver(new RSAKeyValueResolver());
			    keyInfo.registerInternalKeyResolver(new X509CertificateResolver());
			    
			    //ako sadrzi sertifikat
			    if(keyInfo.containsX509Data() && keyInfo.itemX509Data(0).containsCertificate()) { 
			    	X509Certificate  cert =(X509Certificate) readCertificate();
			        //ako postoji sertifikat, provera potpisa
			        if(cert != null) {
			        	//return signature.checkSignatureValue((X509Certificate) cert);
			        	if(signature.checkSignatureValue((X509Certificate) cert))
			        		return true;
			        	else 
			        		return false;
			        }
			        else
			        	return false;
			    }
			    else
			    	return false;
			}
			else
				return false;
		
		} catch (XMLSignatureException e) {
			e.printStackTrace();
			return false;
		} catch (XMLSecurityException e) {
			e.printStackTrace();
			return false;
		}
	}
	private static X509Certificate readCertificate() {
		try {
			//kreiramo instancu KeyStore
			KeyStore ks = KeyStore.getInstance("JKS", "SUN");
			
			//ucitavamo podatke
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(USER_B_JKS));
			ks.load(in, userBPass.toCharArray());
			
			if(ks.isKeyEntry(userBAlias)) {
				X509Certificate  cert =(X509Certificate ) ks.getCertificate(userAAlias);
				return cert;
				
			}
			else
				return null;
			
		} catch (KeyStoreException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
			return null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (CertificateException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} 
	}
	
}
