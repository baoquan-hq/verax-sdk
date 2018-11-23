package com.baoquan.verax.sdk.utils;

import com.baoquan.verax.sdk.FabricConfig;
import com.baoquan.verax.sdk.FabricOrg;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FabricUtils {

  /**
   * ��ȡ�ͻ���ʵ��
   */
  public static HFClient getClient(User user) throws CryptoException, InvalidArgumentException {
    // Fabric�ͻ���ʵ��
    HFClient client = HFClient.createNewInstance();
    client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
    client.setUserContext(user);
    return client;
  }

    /* channel��ز�����ʼ */

  /**
   * ����ͨ�����á��½���ͨ��
   * @param name                 ��Ҫ�½���ͨ��������
   * @param orderer              ͨ���ĸ�orderer�½�ͨ��
   * @param channelConfiguration ͨ��������Ϣ
   * @param signer               �����½�ͨ�����û�����Ҫ��ͨ�����ý���ǩ��
   * @param client               �ͻ���ʵ��
   * @return �½���ͨ��ʵ��
   */
  public static Channel createChannel(String name, Orderer orderer, ChannelConfiguration channelConfiguration,
    User signer,
    HFClient client) throws InvalidArgumentException, TransactionException {
    byte[] configurationSignature = client.getChannelConfigurationSignature(channelConfiguration, signer);
    return client.newChannel(name, orderer, channelConfiguration, configurationSignature);
  }

  /**
   * ��ȡ�Ѵ���ͨ��ʵ��
   */
  public static Channel getChannel(String name, HFClient client) throws InvalidArgumentException {
    Channel channel = client.getChannel(name);
    if (channel == null) {
      channel = client.newChannel(name);
    }
    return client.getChannel(name);
  }

  /**
   * ��ȡorderer���ã�������ordererʵ��
   */
  public static Collection<Orderer> getOrderers(FabricOrg org, HFClient client, FabricConfig config)
    throws InvalidArgumentException {
    Collection<Orderer> orderers = new LinkedList<>();

    for (String orderName : org.getOrdererNames()) {
      Orderer orderer = client.newOrderer(orderName, org.getOrdererLocation(orderName),
        config.getOrdererProperties(orderName));
      orderers.add(orderer);
    }
    return orderers;
  }

  /**
   * ͨ��ʵ�������orderers
   */
  public static void addOrderers(Channel channel, Collection<Orderer> orderers) throws InvalidArgumentException {
    for (Orderer orderer : orderers) {
      channel.addOrderer(orderer);
    }
  }

  /**
   * ��ȡpeer���ã�������peerʵ��
   */
  public static Collection<Peer> getPeers(FabricOrg org, HFClient client, FabricConfig config)
    throws InvalidArgumentException {
    Collection<Peer> peers = new LinkedList<>();

    for (String peerName : org.getPeerNames()) {
      String peerLocation = org.getPeerLocation(peerName);
      Peer peer = client.newPeer(peerName, peerLocation, config.getPeerProperties(peerName));
      peers.add(peer);
    }
    return peers;
  }

  /**
   * ͨ������ӡ��¡�peers
   * @param org     ������Ϣ������peers�����б�
   * @param channel ͨ��ʵ��
   * @param client  �ͻ���ʵ��
   * @param config  ������Ϣ������peers����
   */
  public static void joinPeers(FabricOrg org, Channel channel, HFClient client, FabricConfig config)
    throws InvalidArgumentException,
    ProposalException {
    Collection<Peer> peers = getPeers(org, client, config);
    for (Peer peer : peers) {
      channel.joinPeer(peer);
      org.addPeer(peer);
    }
  }

  /**
   * ͨ��ʵ������ӡ����С�peers
   * @param org     ������Ϣ������peers�����б�
   * @param channel ͨ��ʵ��
   * @param client  �ͻ���ʵ��
   * @param config  ������Ϣ������peers����
   */
  public static void addPeers(FabricOrg org, Channel channel, HFClient client, FabricConfig config)
    throws InvalidArgumentException,
    ProposalException {
    Collection<Peer> peers = getPeers(org, client, config);
    for (Peer peer : peers) {
      channel.addPeer(peer);
      org.addPeer(peer);
    }
  }

  /**
   * add eventHubs
   */
  public static void addEventHubs(Channel channel, FabricOrg org, HFClient client, FabricConfig config)
    throws InvalidArgumentException {
    for (String eventHubName : org.getEventHubNames()) {
      EventHub eventHub = client.newEventHub(eventHubName, org.getEventHubLocation(eventHubName),
        config.getEventHubProperties(eventHubName));
      channel.addEventHub(eventHub);
    }
  }

    /*
    ���·����Ƕ����淽������Ϸ�װ
    ��ֱ��ʹ��
    Ҳ�ɸ�����Ҫ�������
     */

  /**
   * ��ȡһ��ָ�����Ѵ��ڵ�channel
   * ʵ����chennel������orderers��peers��eventHubs
   */
  public static Channel getExistingChannel(String name, FabricOrg org, HFClient client, FabricConfig config)
    throws InvalidArgumentException, ProposalException, TransactionException {
    Collection<Orderer> orderers = getOrderers(org, client, config);

    //��һ��������channel
    Channel newChannel = getChannel(name, client);
    //�ڶ�����peer����channel
    addPeers(org, newChannel, client, config);
    //��������channel add orderer
    addOrderers(newChannel, orderers);
    // eventHub ?
    addEventHubs(newChannel, org, client, config);
    return newChannel.initialize();
  }

  public static Channel getExistingChannelAndJoinPeers(String name, FabricOrg org, HFClient client,
    FabricConfig config)
    throws InvalidArgumentException, ProposalException, TransactionException {
    Channel channel = client.getChannel(name);
    if (channel == null) {
      //��һ��������channel
      Channel newChannel = getChannel(name, client);
      //��������channel add orderer
      Collection<Orderer> orderers = getOrderers(org, client, config);
      addOrderers(newChannel, orderers);
      // eventHub ?
      addEventHubs(newChannel, org, client, config);
      channel = newChannel.initialize();
    }
    //�ڶ�����peer����channel
    joinPeers(org, channel, client, config);
    return channel;
  }

  /**
   * ��ָ�����ƹ���һ���µ�channel
   * ��������channel�������ļ�����һ��
   * ͨ��ordererʵ����chennel
   * ��org�����е�peers����channel��
   * ����orderers��peers��eventHubs
   */
  public static Channel getNewChannel(String name, FabricOrg org, HFClient client, FabricConfig config,
    ChannelConfiguration channelConfiguration)
    throws InvalidArgumentException, ProposalException, TransactionException {
    Collection<Orderer> orderers = getOrderers(org, client, config);
    Orderer orderer = orderers.iterator().next();
    orderers.remove(orderer);
    //��һ��������channel
    Channel newChannel = createChannel(name, orderer, channelConfiguration, org.getPeerAdmin(), client);
    //�ڶ�����peer����channel
    joinPeers(org, newChannel, client, config);
    //��������channel add orderer
    addOrderers(newChannel, orderers);
    // eventHub ?
    addEventHubs(newChannel, org, client, config);
    return newChannel.initialize();
  }

    /* channel��ز������� */


    /* chaincode��ز�����ʼ */

  /**
   * ��װchaincode
   * ���ΰ�װversion=1
   * ָ��version������������ʱ��ʹ��
   * �Ѵ���channel�е�chaincode��peer����channel����Զ�ͬ�����ݣ�ֻ��Ҫ��װchaincode���ɣ������ӳ�
   */
  public static Collection<ProposalResponse> sendInstall(Set<Peer> peers, HFClient client, ChaincodeID chaincodeID,
    File chaincodeSourceLocation, long proposalWaitTime)
    throws InvalidArgumentException, ProposalException {

    InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
    installProposalRequest.setProposalWaitTime(proposalWaitTime);
    installProposalRequest.setChaincodeID(chaincodeID);
    installProposalRequest.setChaincodeSourceLocation(chaincodeSourceLocation);

    return client.sendInstallProposal(installProposalRequest, peers);
  }

  /**
   * ʵ����chaincode
   * ��װchaincode����ã�ֻ�ܵ���һ��
   * ִ��init
   */
  public static Collection<ProposalResponse> sendInstantiate(Channel channel, HFClient client,
    ChaincodeID chaincodeID,
    String[] initArgs, ChaincodeEndorsementPolicy chaincodeEndorsementPolicy, long proposalWaitTime)
    throws InvalidArgumentException, ChaincodeEndorsementPolicyParseException, IOException, ProposalException,
    InterruptedException, ExecutionException, TimeoutException {

    InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
    instantiateProposalRequest.setProposalWaitTime(proposalWaitTime);
    instantiateProposalRequest.setChaincodeID(chaincodeID);
    instantiateProposalRequest.setFcn("init");
    instantiateProposalRequest.setArgs(initArgs);

    Map<String, byte[]> tm = new HashMap<>(2);
    tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
    tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
    instantiateProposalRequest.setTransientMap(tm);

    instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

    return channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
  }

  /**
   * ����chaincode
   * ��Ҫ�Ȱ�װ��ָ��version��chaincode
   * ��Ҫע����ٴ�ִ��init��������Ҫ�޸�init����
   */
  public static Collection<ProposalResponse> sendUpgrade(Channel channel, HFClient client, ChaincodeID chaincodeID,
    String[] initArgs, ChaincodeEndorsementPolicy chaincodeEndorsementPolicy, long proposalWaitTime)
    throws InvalidArgumentException, ProposalException, IOException, ChaincodeEndorsementPolicyParseException,
    InterruptedException, ExecutionException, TimeoutException {

    UpgradeProposalRequest upgradeProposalRequest = client.newUpgradeProposalRequest();
    upgradeProposalRequest.setProposalWaitTime(proposalWaitTime);
    upgradeProposalRequest.setChaincodeID(chaincodeID);
    upgradeProposalRequest.setFcn("init");
    upgradeProposalRequest.setArgs(initArgs);

    Map<String, byte[]> tm = new HashMap<>(2);
    tm.put("HyperLedgerFabric", "UpgradeProposalRequest:JavaSDK".getBytes(UTF_8));
    tm.put("method", "UpgradeProposalRequest".getBytes(UTF_8));
    upgradeProposalRequest.setTransientMap(tm);

    upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

    return channel.sendUpgradeProposal(upgradeProposalRequest);
  }

    /* chaincode��ز������� */


    /* transaction��ز�����ʼ */

  /**
   * �������鵽����ڵ�
   * @param args ����Ҫִ�еķ�������������{methodName, parameters...} i.e.{ "move", "a", "b", "10" }
   */
  public static Collection<ProposalResponse> sendProposalToPeers(Channel channel, HFClient client,
    ChaincodeID chaincodeID, String fcn, String[] args, long proposalWaitTime)
    throws InvalidArgumentException, ProposalException, IOException, ChaincodeEndorsementPolicyParseException {
    TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
    transactionProposalRequest.setProposalWaitTime(proposalWaitTime);
    transactionProposalRequest.setChaincodeID(chaincodeID);
    transactionProposalRequest.setFcn(fcn);
    transactionProposalRequest.setArgs(args);

    Map<String, byte[]> tm2 = new HashMap<>(2);
    tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
    tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
    tm2.put("result", ":)".getBytes(UTF_8));
    transactionProposalRequest.setTransientMap(tm2);

    return channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
  }


  public static Collection<ProposalResponse> sendQuery(Channel channel, HFClient client,
    ChaincodeID chaincodeID, String fcn, String[] args, long proposalWaitTime)
    throws InvalidArgumentException, ProposalException {
    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
    queryByChaincodeRequest.setProposalWaitTime(proposalWaitTime);
    queryByChaincodeRequest.setChaincodeID(chaincodeID);
    queryByChaincodeRequest.setFcn(fcn);
    queryByChaincodeRequest.setArgs(args);

    Map<String, byte[]> tm2 = new HashMap<>(2);
    tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
    tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
    queryByChaincodeRequest.setTransientMap(tm2);

    return channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
  }

  public static Collection<ProposalResponse> sendQuery(Channel channel, HFClient client,
    ChaincodeID chaincodeID, String fcn, byte[][] args, long proposalWaitTime)
    throws InvalidArgumentException, ProposalException {
    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
    queryByChaincodeRequest.setProposalWaitTime(proposalWaitTime);
    queryByChaincodeRequest.setChaincodeID(chaincodeID);
    queryByChaincodeRequest.setFcn(fcn);
    queryByChaincodeRequest.setArgBytes(args);

    Map<String, byte[]> tm2 = new HashMap<>(2);
    tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
    tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
    queryByChaincodeRequest.setTransientMap(tm2);

    return channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
  }

  public static File findFileSk(File directory) {

    File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

    if (null == matches) {
      throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
    }

    if (matches.length != 1) {
      throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
    }

    return matches[0];

  }

}
