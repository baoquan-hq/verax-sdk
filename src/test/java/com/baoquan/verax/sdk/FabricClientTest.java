package com.baoquan.verax.sdk;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.baoquan.verax.sdk.utils.FabricUtils;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Before;
import org.junit.Test;

public class FabricClientTest {

	private static final FabricConfig config = FabricConfig.getInstance();
	private static final File CHAINCODE_DIR = new File("config/chaincode");
	String testTxID = null; // save the CC invoke TxID and use in queries

  private static FabricClient fabricClient;

	


  @Before
	public void initClient() throws Exception {
    fabricClient= new FabricClient();
    fabricClient.setOrgName("peerOrg1");
    fabricClient.setChaincodeName("myhash");
    fabricClient.setVersion("1.0");
    fabricClient.setChannelID("mychannel");

	}
  @Test
  public void sendTransaction() throws Exception{
    String txID = fabricClient.sendTransaction("ff2d5960e239692c834c35307223f45542f35330235009d5a6aec5f7bc19f1fb", "0000702BD0C942DFB2AFF06CE1590A09");
    System.out.println("++++++++++++++++++++:"+txID);
  }



	static void out(String format, Object... args) {
		System.err.flush();
		System.out.flush();
		System.out.println(format(format, args));
		System.err.flush();
		System.out.flush();
	}

}
