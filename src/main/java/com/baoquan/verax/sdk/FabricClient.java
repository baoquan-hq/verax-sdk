package com.baoquan.verax.sdk;

import com.baoquan.verax.sdk.exceptions.ServerException;
import com.baoquan.verax.sdk.utils.FabricUtils;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FabricClient {

  private static final FabricConfig config = FabricConfig.getInstance();

  private ChaincodeID chaincodeID;

  private String channelID = "mychannel";

  private String chaincodeName = "myhash";

  private String  version = "1.0";

  private String orgName;

  private HFClient hfClient;

  private Channel channel;

  private FabricOrg fabricOrg;

  public FabricClient() throws Exception{
    config.initOrgs();
  }
  public HFClient getHfClient() throws CryptoException, InvalidArgumentException{
    if(null==hfClient){
      hfClient=FabricUtils.getClient(getFabricOrg().getPeerAdmin());
    }
    return hfClient;
  }

  public void setHfClient(HFClient hfClient) {
    this.hfClient = hfClient;
  }

  public Channel getChannel() throws Exception  {
    if(null==channel){
      channel=FabricUtils.getExistingChannel(channelID, getFabricOrg(), getHfClient(), config);
      channel.setTransactionWaitTime(config.getTransactionWaitTime());
      channel.setDeployWaitTime(config.getDeployWaitTime());

    }
    return channel;
  }

  public void setChannel(Channel channel){
    this.channel = channel;
  }

  public ChaincodeID getChaincodeID(){
    if(null == chaincodeID){
      this.chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName).setVersion(version).build();
    }
    return chaincodeID;
  }

  public void setChaincodeID(ChaincodeID chaincodeID){
    this.chaincodeID = chaincodeID;
  }

  public String getChannelID(){
    return channelID;
  }

  public void setChannelID(String channelID){
    this.channelID = channelID;
  }

  public String getChaincodeName(){
    return chaincodeName;
  }

  public void setChaincodeName(String chaincodeName){
    this.chaincodeName = chaincodeName;
  }

  public String getVersion(){
    return version;
  }

  public void setVersion(String version){
    this.version = version;
  }

  public static FabricConfig getConfig(){
    return config;
  }

  public String getOrgName(){
    return orgName;
  }

  public void setOrgName(String orgName){
    this.orgName = orgName;
  }

  public FabricOrg getFabricOrg(){
    if(null==fabricOrg){
      this.fabricOrg = config.getIntegrationSampleOrg(orgName);
    }
    return fabricOrg;
  }

  public void setFabricOrg(FabricOrg fabricOrg){
    this.fabricOrg = fabricOrg;
  }

  public String  sendTransaction(String hash,String attestationId) throws Exception{
    Collection<ProposalResponse> successful = new LinkedList<>();
    Collection<ProposalResponse> transactionPropResp = null;

      transactionPropResp = FabricUtils.sendProposalToPeers(getChannel(), getHfClient(),
        getChaincodeID(), "put", new String[]{hash,attestationId}, config.getProposalWaitTime());

    for (ProposalResponse response : transactionPropResp) {
      if (response.getStatus() == ProposalResponse.Status.SUCCESS) {

        successful.add(response);
      } else {
          throw  new ServerException(response.getMessage(),System.currentTimeMillis());
      }
    }

    return sendTransactionToOrderer(getChannel(), successful);

  }

  public Collection<ProposalResponse> sendQuery(Channel channel, String key , HFClient client , ChaincodeID chaincodeID) throws InvalidArgumentException, ProposalException{
    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
    queryByChaincodeRequest.setArgs(new String[] { "query", key });
    queryByChaincodeRequest.setFcn("invoke");
    queryByChaincodeRequest.setChaincodeID(chaincodeID);

    Map<String, byte[]> tm2 = new HashMap<>();
    tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
    tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
    queryByChaincodeRequest.setTransientMap(tm2);

    Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest,channel.getPeers());
    return queryProposals;
  }

  /**
   * 发送交易到orderer
   */
  protected String sendTransactionToOrderer(Channel channel, Collection<ProposalResponse> successful)
    throws Exception{
    return channel.sendTransaction(successful).thenApply(transactionEvent -> {
      try {
        return transactionEvent.getTransactionID();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).exceptionally(e -> {
      e.printStackTrace();
      return null;
    }).get(config.getTransactionWaitTime(), TimeUnit.SECONDS);
  }

}
