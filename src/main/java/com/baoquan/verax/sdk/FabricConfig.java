/*
 *  Copyright 2016, 2017 IBM, DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.baoquan.verax.sdk;

import com.baoquan.verax.sdk.utils.FabricUtils;
import com.baoquan.verax.sdk.utils.ProviderUserUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.helper.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;


public class FabricConfig {
    private static final Log logger = LogFactory.getLog(FabricConfig.class);
    private static final String DEFAULT_CONFIG = "config/fabric.properties";
    private static final String GOSSIPWAITTIME = "org.hyperledger.fabric.sdk.GossipWaitTime";
    private static final String INVOKEWAITTIME = "org.hyperledger.fabric.sdk.InvokeWaitTime";
    private static final String DEPLOYWAITTIME = "org.hyperledger.fabric.sdk.DeployWaitTime";
    private static final String PROPOSALWAITTIME = "org.hyperledger.fabric.sdk.ProposalWaitTime";
    private static final String EVENTHUBCONNECTIONWAITTIME="org.hyperledger.fabric.sdk.eventhub_connection.wait_time";
    private static final String INTEGRATION_ORG = "org.hyperledger.fabric.sdk.integration.org.";
    private static final Pattern orgPat = Pattern.compile("^" + Pattern.quote(INTEGRATION_ORG) + "([^\\.]+)\\.mspid$");

    private static final String INTEGRATIONTESTSTLS = "org.hyperledger.fabric.sdk.tls";

    private static final Properties sdkProperties = new Properties();

    private boolean runningTLS;
    private boolean runningFabricCATLS;
    private boolean runningFabricTLS;
    private static FabricConfig config;
    private static final String CERT_PATH = "config/channel";
    private static final HashMap<String, FabricOrg> sampleOrgs = new HashMap<>();

    private FabricConfig() {
        try {
        	File loadFile = new File(DEFAULT_CONFIG).getAbsoluteFile();
        	FileInputStream configProps = new FileInputStream(loadFile);
            sdkProperties.load(configProps);

            runningTLS = Boolean.valueOf(sdkProperties.getProperty(INTEGRATIONTESTSTLS));
            runningFabricCATLS = runningTLS;
            runningFabricTLS = runningTLS;

            for (Map.Entry<Object, Object> x : sdkProperties.entrySet()) {
                final String key = (String) x.getKey() ;
                final String val = (String) x.getValue();

                if (key.startsWith(INTEGRATION_ORG)) {
                	
                    Matcher match = orgPat.matcher(key);

                    if (match.matches() && match.groupCount() == 1) {
                        String orgName = match.group(1).trim();
                        sampleOrgs.put(orgName, new FabricOrg(orgName, val.trim()));

                    }
                }
            }
            
            for (Map.Entry<String, FabricOrg> org : sampleOrgs.entrySet()) {
                final FabricOrg sampleOrg = org.getValue();
                final String orgName = org.getKey();

                String peerNames = sdkProperties.getProperty(INTEGRATION_ORG + orgName + ".peer_locations");
                String[] ps = peerNames.split("[ \t]*,[ \t]*");
                for (String peer : ps) {
                    String[] nl = peer.split("[ \t]*@[ \t]*");
                    sampleOrg.addPeerLocation(nl[0], grpcTLSify(nl[1]));
                }

                final String domainName = sdkProperties.getProperty(INTEGRATION_ORG + orgName + ".domname");

                sampleOrg.setDomainName(domainName);

                String ordererNames = sdkProperties.getProperty(INTEGRATION_ORG + orgName + ".orderer_locations");
                ps = ordererNames.split("[ \t]*,[ \t]*");
                for (String peer : ps) {
                    String[] nl = peer.split("[ \t]*@[ \t]*");
                    sampleOrg.addOrdererLocation(nl[0], grpcTLSify(nl[1]));
                }

                String eventHubNames = sdkProperties.getProperty(INTEGRATION_ORG + orgName + ".eventhub_locations");
                ps = eventHubNames.split("[ \t]*,[ \t]*");
                for (String peer : ps) {
                    String[] nl = peer.split("[ \t]*@[ \t]*");
                    sampleOrg.addEventHubLocation(nl[0], grpcTLSify(nl[1]));
                }
//                sampleOrg.setCALocation(sdkProperties.getProperty((INTEGRATIONTESTS_ORG + org.getKey() + ".ca_location")));
            }
        } catch (IOException e) { 
        	e.printStackTrace();
        } 

    }

    private String grpcTLSify(String location) {
        location = location.trim();
        Exception e = Utils.checkGrpcUrl(location);
        if (e != null) {
            throw new RuntimeException(String.format("Bad TEST parameters for grpc url %s", location), e);
        }
        return runningFabricTLS ?
                location.replaceFirst("^grpc://", "grpcs://") : location;

    }

    private String httpTLSify(String location) {
        location = location.trim();

        return runningFabricCATLS ?
                location.replaceFirst("^http://", "https://") : location;
    }


    public static FabricConfig getInstance() {
        if (null == config) {
            config = new FabricConfig();
        }
        return config;
    }

    private String getProperty(String property) {
        String ret = sdkProperties.getProperty(property);
        if (null == ret) {
            logger.warn(String.format("No configuration value found for '%s'", property));
        }
        return ret;
    }

    public int getTransactionWaitTime() {
        return Integer.parseInt(getProperty(INVOKEWAITTIME));
    }

    public int getDeployWaitTime() {
        return Integer.parseInt(getProperty(DEPLOYWAITTIME));
    }

    public int getGossipWaitTime() {
        return Integer.parseInt(getProperty(GOSSIPWAITTIME));
    }

    public long getProposalWaitTime() {
        return Integer.parseInt(getProperty(PROPOSALWAITTIME));
    }

  public  long getEVENTHUBCONNECTIONWAITTIME(){
    return Integer.parseInt(getProperty(EVENTHUBCONNECTIONWAITTIME));
  }

  public void initOrgs() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException {
    	Collection<FabricOrg> orgs = Collections.unmodifiableCollection(sampleOrgs.values());
         
         for (FabricOrg org : orgs) {
 			final String orgName = org.getName();
 			final String domainName = org.getDomainName();

 			File privateFileDir = new File(format("%s/crypto-config/peerOrganizations/%s/users/Admin@%s/msp/keystore",CERT_PATH,domainName, domainName));
 			File certificateFile = new File(format("%s/crypto-config/peerOrganizations/%s/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem",CERT_PATH, domainName,domainName,domainName));
 			// peer admin 挂载证书
 			FabricUser peerOrgAdmin = ProviderUserUtil.getUser(orgName + "Admin",org.getMSPID(), FabricUtils.findFileSk(privateFileDir),certificateFile);
 			org.setPeerAdmin(peerOrgAdmin);
 		}
    }

    public FabricOrg getIntegrationSampleOrg(String name) {
        return sampleOrgs.get(name);
    }


    public Properties getPeerProperties(String name) {
        return getEndPointProperties("peer", name);
    }

    public Properties getOrdererProperties(String name) {
        return getEndPointProperties("orderer", name);
    }

    public Properties getEventHubProperties(String name) {
        return getEndPointProperties("peer", name); //uses same as named peer
    }

    private Properties getEndPointProperties(final String type, final String name) {
        final String domainName = getDomainName(name);
        String crtpath = format("%s/crypto-config/%sOrganizations/%s/%ss/%s/tls/server.crt",CERT_PATH, type , domainName , type , name);
        File cert = new File(crtpath);
        
        Properties ret = new Properties();
        ret.setProperty("pemFile", cert.getAbsolutePath());
        ret.setProperty("hostnameOverride", name);
        ret.setProperty("sslProvider", "openSSL");
        ret.setProperty("negotiationType", "TLS");
        return ret;
    }


    private String getDomainName(final String name) {
        int dot = name.indexOf(".");
        if (-1 == dot) {
            return null;
        } else {
            return name.substring(dot + 1);
        }
    }


}
