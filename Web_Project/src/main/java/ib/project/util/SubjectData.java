package ib.project.util;


import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;

/**
 * Generise sertifikat koji potpisuje neko drugi
 */

public class SignedCertificateGenerator {

	private static String KEY_STORE_FILE = "./data/rootca.jks";

	public  SignedCertificateGenerator(String name, char[] subjectPassword, char[] issuerPassword) {
		
		String commonName = name;
		String surname = name;
		String givenName = name;
		String orgName = name + " organisation";
		String orgUnit = name + " unit";
		String country = "RS";
		String email = name + "@gmail.com";

		KeyStoreReader keyStoreReader = new KeyStoreReader();
		
		// osnovni podaci za subject-a
		X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
		builder.addRDN(BCStyle.CN, commonName);
		builder.addRDN(BCStyle.SURNAME, surname);
		builder.addRDN(BCStyle.GIVENNAME, givenName);
		builder.addRDN(BCStyle.O, orgName);
		builder.addRDN(BCStyle.OU, orgUnit);
		builder.addRDN(BCStyle.C, country);
		builder.addRDN(BCStyle.E, email);
		// UID (USER ID) je ID korisnika
		builder.addRDN(BCStyle.UID, "123");

		CertificateGenerator cg = new CertificateGenerator();

		KeyPair keyPair = cg.generateKeyPair();

		Date startDate = null;
		Date endDate = null;
		final java.util.Calendar cal = GregorianCalendar.getInstance();
		startDate = cal.getTime();
		cal.setTime(startDate);
		cal.add(GregorianCalendar.YEAR, 2); // sertifikat traje 2 godine od
		// datuma kreiranja
		endDate = cal.getTime();
		String sn = "1";

		IssuerData issuerData = null;
		try {
			issuerData = keyStoreReader
					.readKeyStore(KEY_STORE_FILE, "CA", issuerPassword, issuerPassword);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// kreiraju se podaci za vlasnika
		SubjectData subjectData = new SubjectData(keyPair.getPublic(),
				builder.build(), sn, startDate, endDate);

		// generise se sertifikat
		X509Certificate cert = cg.generateCertificate(issuerData, subjectData);

		// kreira se keystore, ucitava ks fajl, dodaje kljuc i sertifikat i
		// sacuvaju se izmene
		KeyStoreWriter keyStoreWriter = new KeyStoreWriter();
		keyStoreWriter.loadKeyStore(null, (commonName + "1").toCharArray());
		keyStoreWriter.write(commonName, keyPair.getPrivate(), subjectPassword, cert);
		keyStoreWriter.saveKeyStore("./data/" + commonName + ".jks", subjectPassword);
		
		
		try {
			cert.verify(keyStoreReader.readPublicKey());
			System.out.println("Validacija uspešna.");
		} catch (InvalidKeyException | CertificateException
				| NoSuchAlgorithmException | NoSuchProviderException
				| SignatureException e) {
			System.out.println("Validacija neuspešna");
			e.printStackTrace();
		}
	}

}
