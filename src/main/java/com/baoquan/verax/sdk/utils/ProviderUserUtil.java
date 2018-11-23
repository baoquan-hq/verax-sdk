package com.baoquan.verax.sdk.utils;

import com.baoquan.verax.sdk.FabricUser;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Enrollment;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

public class ProviderUserUtil {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static FabricUser getUser(String name, String mspId, File privateKeyFile, File certificateFile)
			throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
			FabricUser sampleUser = new FabricUser(name);
			sampleUser.setMspId(mspId);
			String certificate = new String(IOUtils.toByteArray(new FileInputStream(certificateFile)), "UTF-8");
			PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privateKeyFile)));
			sampleUser.setEnrollment(new FabricUserEnrollement(privateKey, certificate));
			return sampleUser;
	}

	

	static PrivateKey getPrivateKeyFromBytes(byte[] data)
			throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
		final Reader pemReader = new StringReader(new String(data));
		final PrivateKeyInfo pemPair;
		
		try (PEMParser pemParser = new PEMParser(pemReader)) {
			pemPair = (PrivateKeyInfo) pemParser.readObject();
		}

		PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
				.getPrivateKey(pemPair);

		return privateKey;
	}

	static final class FabricUserEnrollement implements Enrollment, Serializable {

		private static final long serialVersionUID = -2784835212445309006L;
		private final PrivateKey privateKey;
		private final String certificate;

		FabricUserEnrollement(PrivateKey privateKey, String certificate) {
			this.certificate = certificate;
			this.privateKey = privateKey;
		}

		@Override
		public PrivateKey getKey() {
			return privateKey;
		}

		@Override
		public String getCert() {
			return certificate;
		}
	}

}