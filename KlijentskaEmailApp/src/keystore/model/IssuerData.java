package app;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
import org.apache.xml.security.keys.content.KeyName;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.api.services.gmail.Gmail;

import keystoreReader.KeyStoreReader;
import support.MailHelper;
import support.MailWritter;
import util.Base64;
import util.GzipUtil;
import util.IVHelper;

public class WriteMail extends MailClient {
	private static final String USER_A_JKS = "./data/usera.jks";
	private static final String USER_B_JKS = "./data/userb.jks";
	private static final String userBAlias = "userb";
	private static final String userAAlias = "usera";
	private static final String userBPass = "123";
	private static final String userAPass = "123";
	
	static {
		// staticka inicijalizacija
		Security.addProvider(new BouncyCastleProvider());
		org.apache.xml.security.Init.init();
	}
	
	public static void main(String[] args) {
		try {
			Gmail service = getGmailService();

			// Unos podataka
			System.out.println("Insert a reciever:");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String reciever = reader.readLine();
			
			System.out.println("Insert a subject:");
			String subject = reader.readLine();

			System.out.println("Insert body:");
			String body = reader.readLine();
			
			// kreiraj xml dokument
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("mail");

			Element mailSubject = doc.createElement("subjectMail");
			Element mailBody = doc.createElement("bodyMail");
			
			
			mailSubject.setTextContent(subject);
			mailBody.setTextContent(body);	
			rootElement.appendChild(mailSubject);
			rootElement.appendChild(mailBody);
			
			doc.appendChild(rootElement);

			// dokument pre enkripcije
			String xml = xmlAsString(doc);
			System.out.println("Mail pre enkripcije: " + xml);
			
			// generisanje tajnog (session) kljuca
			SecretKey secretKey = generateSessionKey();

			// citanje keystore-a kako bi se izvukao sertifikat primaoca
			// i kako bi se dobio njegov javni kljuc
			PublicKey publicKey = getPublicKey();
			
			// inicijalizacija radi sifrovanja teksta mail-a
			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.TRIPLEDES);
			xmlCipher.init(XMLCipher.ENCRYPT_MODE, secretKey);

			// inicijalizacija radi sifrovanja tajnog (session) kljuca javnim RSA kljucem
			XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
			keyCipher.init(XMLCipher.WRAP_MODE, publicKey);
			
			// kreiranje EncryptedKey objekta koji sadrzi enkriptovan tajni
			// (session) kljuc
			EncryptedKey encryptedKey = keyCipher.encryptKey(doc, secretKey);
			System.out.println("Kriptovan tajni kljuc: " + encryptedKey);
			
						
			//kreiranje KeyInfo objekta, postavljanje naziva i enkriptovanog tajnog kljuca
			KeyInfo keyInfo = new KeyInfo(doc); 
			// postavljamo naziv
			keyInfo.add(new KeyName(doc, "encryptedKey"));
			// postavljamo kriptovani kljuc
			keyInfo.add(encryptedKey);
			
			//kreiranje EncryptedData objekata, postavljanje KeyInfo objekata
			EncryptedData encData = xmlCipher.getEncryptedData();
			encData.setKeyInfo(keyInfo);
				
			//potpisivanje dokumenta
			WriteMail sign = new WriteMail();
			sign.signing(doc);
			
			//funkcija sluzi za proveru kad se izmeni email body
			/*Element bodyNV = (Element) doc.getElementsByTagName("mailBody").item(0);
			bodyNV.setTextContent("Email je promenjen.");*/
			
			//kriptovati sadrzaj dokumenta
			xmlCipher.doFinal(doc, rootElement,true );

			// Slanje poruke
			String encryptedXml = xmlAsString(doc);
			System.out.println("Mail posle enkripcije: " + encryptedXml);

			String cipherSubject = cipherData(secretKey,subject);
			
			MimeMessage mimeMessage = MailHelper.createMimeMessage(reciever, cipherSubject, encryptedXml);
			MailWritter.sendMessage(service, "me", mimeMessage);
			System.out.println("Mail je poslat");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static String xmlAsString(Document doc) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		String output = writer.getBuffer().toString().replaceAll("\n|\r", "");

		return output;
	}

	private static String cipherData(SecretKey secretKey,String data) {
		try {
			String compressedData = Base64.encodeToString(GzipUtil.compress(data));
			Cipher aesCipherEnc = Cipher.getInstance("AES/CBC/PKCS5Padding");
			
			//inicijalizacija za sifrovanje 
			IvParameterSpec ivParameterSpec2 = IVHelper.createIV();
			aesCipherEnc.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec2);
			
			byte[] cipherData = aesCipherEnc.doFinal(compressedData.getBytes());
			String cipherDataStr = Base64.encodeToString(cipherData);
			System.out.println("Kriptovan text: " + cipherDataStr);
			
			return cipherDataStr;
			
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//generisi tajni (session) kljuc
	private static SecretKey generateSessionKey() {
		
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("TRIPLEDES");
			keyGenerator.init(168);
			SecretKey secretKey = keyGenerator.generateKey();
			return secretKey;
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		}
		return null;
	}

	//iz sertifikata korisnika B izvuci njegov javni kljc
	private static PublicKey getPublicKey() {
		
		KeyStoreReader keyStoreReader = new KeyStoreReader();
		try {
			
			keyStoreReader.readKeyStore(USER_A_JKS, userBAlias, userAPass.toCharArray() , userBPass.toCharArray());
		
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		
		PublicKey userBPK = keyStoreReader.readPublicKey();
		return userBPK;
	}
	
	private static PrivateKey getPrivateKey(){
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
			//ucitavanje keyStore
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(USER_A_JKS));
			keyStore.load(in, userAPass.toCharArray());
			
			if(keyStore.isKeyEntry(userAAlias)) {
				PrivateKey privateKey = (PrivateKey) keyStore.getKey(userAAlias, userAPass.toCharArray());
				return privateKey;
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
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
			return null;
		} 
	}
	
	private X509Certificate getCertificate() {
		try {
			//kreiramo instancu KeyStore
			KeyStore ks = KeyStore.getInstance("JKS", "SUN");
			//ucitavamo podatke
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(USER_B_JKS));
			ks.load(in, userBPass.toCharArray());
			
			if(ks.isKeyEntry(userBAlias)) {
				X509Certificate cert = (X509Certificate) ks.getCertificate(userAAlias);
				System.out.println("cert "+cert.getSignature());
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
	private Document signDocument(Document doc, PrivateKey privateKey, X509Certificate cert) {
		try {
			Element rootEl = doc.getDocumentElement();
			
			//kreira se signature objekat
			XMLSignature signature = new XMLSignature(doc, null, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);
			//kreiraju se transformacije nad dokumentom
			Transforms transforms = new Transforms(doc);
			    
			//iz potpisa uklanja Signature element
			//Ovo je potrebno za enveloped tip po specifikaciji
			transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
			//normalizacija
			transforms.addTransform(Transforms.TRANSFORM_C14N_WITH_COMMENTS);
			    
			//potpisuje se citav dokument (zato je URI : "")
			signature.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);
			    
			//U KeyInfo se postavalja Javni kljuc samostalno i citav sertifikat
			signature.addKeyInfo(cert.getPublicKey());
			signature.addKeyInfo((X509Certificate) cert);
			System.out.println("sign: " + signature);
			System.out.println("sig.keyinfo " + signature.getKeyInfo());
			    
			//poptis je child root elementa
			rootEl.appendChild(signature.getElement());
			System.out.println("sign pre kriptovanja: " + signature);   

			System.out.println("sign signature: " + signature.getSignatureValue());
			//potpisivanje
			signature.sign(privateKey);
			System.out.println("sign kriptovani: " + signature);

			System.out.println("sign signature ps: " + signature.getSignatureValue());
			
			return doc;
	    } catch (TransformationException e) {
			e.printStackTrace();
			return null;
		} catch (XMLSignatureException e) {
			e.printStackTrace();
			return null;
		} catch (DOMException e) {
			e.printStackTrace();
			return null;
		} catch (XMLSecurityException e) {
			e.printStackTrace();
			return null;
		}
	}
	private void signing(Document document){
		PrivateKey privateKey = getPrivateKey();
		X509Certificate cert = getCertificate();
		System.out.println("Signing....");
		document = signDocument(document, privateKey, cert);
	}
	
}
