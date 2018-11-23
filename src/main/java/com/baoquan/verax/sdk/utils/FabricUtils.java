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
   * 获取客户端实例
   */
  public static HFClient getClient(User user) throws CryptoException, InvalidArgumentException {
    // Fabric客户端实例
    HFClient client = HFClient.createNewInstance();
    client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
    client.setUserContext(user);
    return client;
  }

    /* channel相关操作开始 */

  /**
   * 根据通道配置“新建”通道
   * @param name                 需要新建的通道的名称
   * @param orderer              通过哪个orderer新建通道
   * @param channelConfiguration 通道配置信息
   * @param signer               申请新建通道的用户，需要对通道配置进行签名
   * @param client               客户端实例
   * @return 新建的通道实例
   */
  public static Channel createChannel(String name, Orderer orderer, ChannelConfiguration channelConfiguration,
    User signer,
    HFClient client) throws InvalidArgumentException, TransactionException {
    byte[] configurationSignature = client.getChannelConfigurationSignature(channelConfiguration, signer);
    return client.newChannel(name, orderer, channelConfiguration, configurationSignature);
  }

  /**
   * 获取已存在通道实例
   */
  public static Channel getChannel(String name, HFClient client) throws InvalidArgumentException {
    Channel channel = client.getChannel(name);
    if (channel == null) {
      channel = client.newChannel(name);
    }
    return client.getChannel(name);
  }

  /**
   * 提取orderer配置，并创建orderer实例
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
   * 通道实例中添加orderers
   */
  public static void addOrderers(Channel channel, Collection<Orderer> orderers) throws InvalidArgumentException {
    for (Orderer orderer : orderers) {
      channel.addOrderer(orderer);
    }
  }

  /**
   * 提取peer配置，并创建peer实例
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
   * 通道中添加“新”peers
   * @param org     机构信息，包含peers名称列表
   * @param channel 通道实例
   * @param client  客户端实例
   * @param config  配置信息，包含peers配置
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
   * 通道实例中添加“已有”peers
   * @param org     机构信息，包含peers名称列表
   * @param channel 通道实例
   * @param client  客户端实例
   * @param config  配置信息，包含peers配置
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
    以下方法是对上面方法的组合封装
    可直接使用
    也可根据需要自行组合
     */

  /**
   * 获取一个指定的已存在的channel
   * 实例化chennel，配置orderers、peers、eventHubs
   */
  public static Channel getExistingChannel(String name, FabricOrg org, HFClient client, FabricConfig config)
    throws InvalidArgumentException, ProposalException, TransactionException {
    Collection<Orderer> orderers = getOrderers(org, client, config);

    //第一步：创建channel
    Channel newChannel = getChannel(name, client);
    //第二步：peer加入channel
    addPeers(org, newChannel, client, config);
    //第三步：channel add orderer
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
      //第一步：创建channel
      Channel newChannel = getChannel(name, client);
      //第三步：channel add orderer
      Collection<Orderer> orderers = getOrderers(org, client, config);
      addOrderers(newChannel, orderers);
      // eventHub ?
      addEventHubs(newChannel, org, client, config);
      channel = newChannel.initialize();
    }
    //第二步：peer加入channel
    joinPeers(org, channel, client, config);
    return channel;
  }

  /**
   * 以指定名称构建一个新的channel
   * 名称需与channel的配置文件里面一致
   * 通过orderer实例化chennel
   * 将org配置中的peers加入channel中
   * 配置orderers、peers、eventHubs
   */
  public static Channel getNewChannel(String name, FabricOrg org, HFClient client, FabricConfig config,
    ChannelConfiguration channelConfiguration)
    throws InvalidArgumentException, ProposalException, TransactionException {
    Collection<Orderer> orderers = getOrderers(org, client, config);
    Orderer orderer = orderers.iterator().next();
    orderers.remove(orderer);
    //第一步：创建channel
    Channel newChannel = createChannel(name, orderer, channelConfiguration, org.getPeerAdmin(), client);
    //第二步：peer加入channel
    joinPeers(org, newChannel, client, config);
    //第三步：channel add orderer
    addOrderers(newChannel, orderers);
    // eventHub ?
    addEventHubs(newChannel, org, client, config);
    return newChannel.initialize();
  }

    /* channel相关操作结束 */


    /* chaincode相关操作开始 */

  /**
   * 安装chaincode
   * 初次安装version=1
   * 指定version可以在升级的时候使用
   * 已存在channel中的chaincode，peer加入channel后会自动同步数据，只需要安装chaincode即可，会有延迟
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
   * 实例化chaincode
   * 安装chaincode后调用，只能调用一次
   * 执行init
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
   * 升级chaincode
   * 需要先安装完指定version的chaincode
   * 需要注意会再次执行init，根据需要修改init方法
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

    /* chaincode相关操作结束 */


    /* transaction相关操作开始 */

  /**
   * 发起提议到背书节点
   * @param args 包含要执行的方法名及参数，{methodName, parameters...} i.e.{ "move", "a", "b", "10" }
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
