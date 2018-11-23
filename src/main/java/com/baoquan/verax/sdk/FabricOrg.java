package com.baoquan.verax.sdk;

import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;

import java.util.*;

public class FabricOrg {
	
    private String name;
    private String mspid;
    private Map<String, User> userMap = new HashMap<>();
    private Map<String, String> peerLocations = new HashMap<>();
    private Map<String, String> ordererLocations = new HashMap<>();
    private Map<String, String> eventHubLocations = new HashMap<>();
    private Set<Peer> peers = new HashSet<>();
    
    private FabricUser admin;
    private FabricUser peerAdmin;
    private String domainName;

    public FabricOrg(String name, String mspid) {
        this.name = name;
        this.mspid = mspid;
    }

    public FabricUser getAdmin() {
        return admin;
    }

    public void setAdmin(FabricUser admin) {
        this.admin = admin;
    }

    public String getMSPID() {
        return mspid;
    }

    public void addPeerLocation(String name, String location) {

        peerLocations.put(name, location);
    }

    public void addOrdererLocation(String name, String location) {

        ordererLocations.put(name, location);
    }

    public void addEventHubLocation(String name, String location) {

        eventHubLocations.put(name, location);
    }

    public String getPeerLocation(String name) {
        return peerLocations.get(name);

    }

    public String getOrdererLocation(String name) {
        return ordererLocations.get(name);

    }

    public String getEventHubLocation(String name) {
        return eventHubLocations.get(name);

    }

    public Set<String> getPeerNames() {

        return Collections.unmodifiableSet(peerLocations.keySet());
    }


    public Set<String> getOrdererNames() {

        return Collections.unmodifiableSet(ordererLocations.keySet());
    }

    public Set<String> getEventHubNames() {

        return Collections.unmodifiableSet(eventHubLocations.keySet());
    }

    public String getName() {
        return name;
    }

    public void addUser(FabricUser user) {
        userMap.put(user.getName(), user);
    }

    public User getUser(String name) {
        return userMap.get(name);
    }

    public Collection<String> getOrdererLocations() {
        return Collections.unmodifiableCollection(ordererLocations.values());
    }

    public Collection<String> getEventHubLocations() {
        return Collections.unmodifiableCollection(eventHubLocations.values());
    }

    public Set<Peer> getPeers() {
        return Collections.unmodifiableSet(peers);
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
    }

    public FabricUser getPeerAdmin() {
        return peerAdmin;
    }

    public void setPeerAdmin(FabricUser peerAdmin) {
        this.peerAdmin = peerAdmin;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainName() {
        return domainName;
    }
}
