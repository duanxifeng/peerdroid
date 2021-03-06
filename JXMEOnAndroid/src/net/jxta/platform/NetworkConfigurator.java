/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must
 *     include the following acknowledgment: "This product includes software
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology."
 *     Alternately, this acknowledgment may appear in the software itself, if
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *     not be used to endorse or promote products derived from this software
 *     without prior written permission. For written permission, please contact
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals
 *  on behalf of Project JXTA. For more information on Project JXTA, please see
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.platform;


import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.protocol.HTTPAdv;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.protocol.RdvConfigAdv.RendezVousConfiguration;
import net.jxta.impl.protocol.RelayConfigAdv;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.TransportAdvertisement;
import javax.security.cert.CertificateException;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * NetworkConfigurator provides a simple programmatic interface for JXTA configuration.
 * <p/>
 * By default, it defines an edge configuration with TCP in auto mode w/port
 * range 9701-9799, multicast enabled on group "224.0.1.85", and port 1234,
 * HTTP transport with only out going enabled.
 * <p/>
 * By default a new PeerID is always generated. This can be overridden via
 * {@link NetworkConfigurator#setPeerID} method or loading a PlatformConfig via
 * {@link NetworkConfigurator#load}.
 * <p/>
 * A facility is provided to initialize a configuration by loading from a
 * existing configuration. This provides limited platform configuration lifecycle
 * management as well as configuration change management.
 * <p/>
 * Also by default, this class sets the default platform configurator to
 * is no operation configurator intended to prevent any other configurators from
 * being invoked, including the AWT ConfigDialog.
 * <p/>
 * NetworkConfigurator makes use of classes from the {@code net.jxta.impl.*}
 * packages. Applications are very strongly encouraged to avoid importing these
 * classes as their interfaces may change without notice in future JXTA releases.
 * The NetworkConfigurator API abstracts the configruation implementation details
 * and will provide continuity and stability i.e. the NetworkConfigurator API
 * won't change and it will automatically accomodate changes to service
 * configuration.
 * <p/>
 * <em> Configuration example :</em>
 * <pre>
 *     NetworkConfigurator config = new NetworkConfigurator();
 *     if (!config.exists()) {
 *         // Create a new configuration with a new name, principal, and pass
 *         config.setName("New Name");
 *         config.setPrincipal("username");
 *         config.setPassword("password");
 *         try {
 *             //persist it
 *             config.save();
 *         } catch (IOException io) {
 *             // deal with the io error
 *         }
 *      } else {
 *        // Load the pre-existing configuration
 *        File pc = new File(config.getHome(), "PlatformConfig");
 *        try {
 *            config.load(pc.toURI());
 *            // make changes if so desired
 *             ..
 *             ..
 *            // store the PlatformConfig under the default home
 *            config.save();
 *        } catch (CertificateException ce) {
 *            // In case the root cert is invalid, this creates a new one
 *            try {
 *                  //principal
 *                  config.setPrincipal("principal");
 *                  //password to encrypt private key with
 *                  config.setPassword("password");
 *                  config.save();
 *              } catch (Exception e) {
 *                  e.printStackTrace();
 *              }
 *        }
 * <p/>
 * </pre>
 *
 * @since JXTA JSE 2.4
 */
public class NetworkConfigurator {

    // begin configuration modes

    /**
     * Relay client Mode
     */
    public final static int RELAY_OFF = 1 << 2;

    /**
     * Relay client Mode
     */
    public final static int RELAY_CLIENT = 1 << 3;

    /**
     * Relay client Mode
     */
    public final static int RELAY_SERVER = 1 << 4;

    /**
     * Proxy Server Mode
     */
    public final static int PROXY_SERVER = 1 << 5;

    /**
     * TCP transport client state
     */
    public final static int TCP_CLIENT = 1 << 6;

    /**
     * TCP transport state
     */
    public final static int TCP_SERVER = 1 << 7;

    /**
     * HTTP transport client state
     */
    public final static int HTTP_CLIENT = 1 << 8;

    /**
     * HTTP transport server state
     */
    public final static int HTTP_SERVER = 1 << 9;

    /**
     * IP multicast transport state
     */
    public final static int IP_MULTICAST = 1 << 10;

    /**
     * Rendezvous Mode
     */
    public final static int RDV_SERVER = 1 << 11;

    /**
     * RendezVousService Client
     */
    public final static int RDV_CLIENT = 1 << 12;

    /**
     * RendezVousService Ad-Hoc mode
     */
    public final static int RDV_AD_HOC = 1 << 13;

    /**
     * Default AD-HOC configuration
     */
    public final static int ADHOC_NODE = TCP_CLIENT | TCP_SERVER | IP_MULTICAST | RDV_AD_HOC | RELAY_OFF;

    /**
     * Default Edge configuration
     */
    public final static int EDGE_NODE = TCP_CLIENT | TCP_SERVER | HTTP_CLIENT | IP_MULTICAST | RDV_CLIENT | RELAY_CLIENT;

    /**
     * Default Rendezvous configuration
     */
    public final static int RDV_NODE = RDV_SERVER | TCP_CLIENT | TCP_SERVER | HTTP_SERVER;

    /**
     * Default Relay configuration
     */
    public final static int RELAY_NODE = RELAY_SERVER | TCP_CLIENT | TCP_SERVER | HTTP_SERVER;

    // end configuration modes

    /**
     * Default mode
     */
    protected transient int mode = EDGE_NODE;

    /**
     * Default PlatformConfig Peer Description
     */
    protected transient String description = "Platform Config Advertisement created by : " + NetworkConfigurator.class.getName();

    /**
     * the location which will serve as the parent for all stored items used
     * by JXTA.
     */
    private transient URI storeHome = null;

    /**
     * Default peer name
     */
    protected transient String name = "unknown";

    /**
     * Password value used to generate root centificate
     */
    protected transient String password = null;

    /**
     * Default PeerID
     */
    protected transient PeerID peerid = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
    //protected transient PeerID peerid = null;
    /**
     * Principal value used to generate root certificate
     */
    protected transient String principal = null;

    /**
     * public certificate
     */
    protected transient X509Certificate cert = null;

    /**
     * Subject private key
     */
    protected transient PrivateKey subjectPkey = null;

    /**
     * Freestanding keystore location
     */
    protected transient URI keyStoreLocation = null;

    /**
     * Proxy Service Document
     */
    protected transient XMLElement proxyConfig;

    /**
     * Rendezvous Config Advertisement
     */
    protected transient RdvConfigAdv rdvConfig;

    /**
     * Default Rendezvous Seeding URI
     */
    protected URI rdvSeedingURI = null;

    /**
     * Relay Config Advertisement
     */
    protected transient RelayConfigAdv relayConfig;

    /**
     * Default Relay Seeding URI
     */
    protected transient URI relaySeedingURI = null;

    /**
     * TCP Config Advertisement
     */
    protected transient TCPAdv tcpConfig;

    /**
     * default TCP transport state
     */
    protected transient boolean tcpEnabled = true;

    /**
     * HTTP Config Advertisement
     */
    protected transient HTTPAdv httpConfig;

    /**
     * default HTTP transport state
     */
    protected transient boolean httpEnabled = true;

    /**
     * Creates NetworkConfigurator instance with default AD-HOC configuration
     *
     * @param storeHome the URI to persistent store
     * @return NetworkConfigurator instance with default AD-HOC configuration
     */
    public static NetworkConfigurator newAdHocConfiguration(URI storeHome) {
        return new NetworkConfigurator(ADHOC_NODE, storeHome);
    }

    /**
     * Creates NetworkConfigurator instance with default Edge configuration
     *
     * @param storeHome the URI to persistent store
     * @return NetworkConfigurator instance with default AD-HOC configuration
     */
    public static NetworkConfigurator newEdgeConfiguration(URI storeHome) {
        return new NetworkConfigurator(EDGE_NODE, storeHome);
    }


    /**
     * Creates the default NetworkConfigurator. The configuration is stored  with a default configuration mode of EDGE_NODE
     * @throws URISyntaxException 
     */
    public NetworkConfigurator() throws URISyntaxException {
    	 this(EDGE_NODE, new File("/data/data/it.p2p/files/jxta").toURI());
    	//this(EDGE_NODE, new File("/data/data/it.jxme.chat/files/jxta").toURI());
    	//this(EDGE_NODE, new URI("/data/data/it.jxme.chat/.jxta"));
    }

    /**
     * Creates a NetworkConfigurator with the default configuration of the
     * specified mode. <p/>Valid modes include ADHOC_NODE, EDGE_NODE, RDV_NODE
     * PROXY_NODE, RELAY_NODE, RDV_RELAY_PROXY_NODE, or any combination of
     * specific configuration.<p/> e.g. RDV_NODE | HTTP_CLIENT
     *
     * @param mode      the new configuration mode
     * @param storeHome the URI to persistent store
     * @see #setMode
     */
    public NetworkConfigurator(int mode, URI storeHome) {
    	//Log.d(P2PStreaming.TAG, "NetworkConfigurator 0");
        setStoreHome(storeHome);
        //Log.d(P2PStreaming.TAG, "NetworkConfigurator 1");
        httpConfig = createHttpAdv();
        //Log.d(P2PStreaming.TAG, "NetworkConfigurator 2");
        rdvConfig = createRdvConfigAdv();
        //Log.d(P2PStreaming.TAG, "NetworkConfigurator 3");
        relayConfig = createRelayConfigAdv();
        //Log.d(P2PStreaming.TAG, "NetworkConfigurator 4");
        proxyConfig = createProxyAdv();
        //Log.d(P2PStreaming.TAG, "NetworkConfigurator 5");
        tcpConfig = createTcpAdv();
        //Log.d(P2PStreaming.TAG, "NetworkConfigurator 6");
        setMode(mode);
        //Log.d(P2PStreaming.TAG, "NetworkConfigurator 7");
    }


    /**
     * Set the current directory for configuration and cache persistent store
     * <p/>(default is $CWD/.jxta)
     * <p/>
     * <dt>Simple example :</dt>
     * <pre>
     *  <code>
     *   //Create an application home
     *   File appHome = new File(System.getProperty("JXTA_HOME", ".cache"));
     *   //Create an instance home under the application home
     *   File instanceHome = new File(appHome, instanceName);
     *   jxtaConfig.setHome(instanceHome);
     *   </code>
     * </pre>
     *
     * @param home the new home value
     * @see #getHome
     */
    public void setHome(File home) {
        this.storeHome = home.toURI();
    }

    /**
     * Returns the current directory for configuration and cache persistent
     * store. This is the same location as returned by {@link #getStoreHome()}
     * which is more general than this method.
     *
     * @return Returns the current home directory
     * @see #setHome
     */
    public File getHome() {
        if ("file".equalsIgnoreCase(storeHome.getScheme())) {
            return new File(storeHome);
        } else {
            throw new UnsupportedOperationException("Home location is not a file:// URI : " + storeHome);
        }
    }

    /**
     * Returns the location which will serve as the parent for all stored items
     * used by JXTA.
     *
     * @return The location which will serve as the parent for all stored
     *         items used by JXTA.
     */
    public URI getStoreHome() {
        return storeHome;
    }


    /**
     * Sets the location which will serve as the parent for all stored items
     * used by JXTA.
     *
     * @param newHome store home
     */
    public void setStoreHome(URI newHome) {
        // Fail if the URI is not absolute.
    	//Log.d(P2PStreaming.TAG, "set store home 0");
        if (!newHome.isAbsolute()) {
            throw new IllegalArgumentException("Only absolute URIs accepted for store home location.");
        }
        //Log.d(P2PStreaming.TAG, "set store home 1");
        // Fail if the URI is Opaque.
        if (newHome.isOpaque()) {
            throw new IllegalArgumentException("Only hierarchical URIs accepted for store home location.");
        }
        //Log.d(P2PStreaming.TAG, "set store home 2");
        // FIXME this should be removed when 1488 is committed
        if (!"file".equalsIgnoreCase(newHome.getScheme())) {
            throw new IllegalArgumentException("Only file based URI currently supported");
        }
        //Log.d(P2PStreaming.TAG, "set store home 3");
        // Adds a terminating /
        if (!newHome.toString().endsWith("/")) {
            newHome = URI.create(newHome.toString() + "/");
        }
        //Log.d(P2PStreaming.TAG, "set store home 4");
        storeHome = newHome;
    }

    /**
     * Sets PlaformConfig Peer Description element
     *
     * @param description the peer description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Toggles HTTP transport state
     *
     * @param enabled if true, enables HTTP transport
     */
    public void setHttpEnabled(boolean enabled) {
        this.httpEnabled = enabled;
        if (!httpEnabled) {
            httpConfig.setClientEnabled(false);
            httpConfig.setServerEnabled(false);
        }
    }

    /**
     * Toggles the HTTP transport server (incoming) mode
     *
     * @param incoming toggles HTTP transport server mode
     */
    public void setHttpIncoming(boolean incoming) {
        httpConfig.setServerEnabled(incoming);
    }

    /**
     * Toggles the HTTP transport client (outgoing) mode
     *
     * @param outgoing toggles HTTP transport client mode
     */
    public void setHttpOutgoing(boolean outgoing) {
        httpConfig.setClientEnabled(outgoing);
    }

    /**
     * Sets the HTTP listening port (default 9901)
     *
     * @param port the new HTTP port value
     */
    public void setHttpPort(int port) {
        httpConfig.setPort(port);
    }

    /**
     * Sets the HTTP interface Address to bind the HTTP transport to
     * <p/>e.g. "192.168.1.1"
     *
     * @param address the new address value
     */
    public void setHttpInterfaceAddress(String address) {
        httpConfig.setInterfaceAddress(address);
    }

    /**
     * Sets the HTTP JXTA Public Address
     * e.g. "192.168.1.1:9700"
     *
     * @param address   the HTTP transport public address
     * @param exclusive determines whether an address is advertised exclusively
     */
    public void setHttpPublicAddress(String address, boolean exclusive) {
        httpConfig.setServer(address);
        httpConfig.setPublicAddressOnly(exclusive);
    }

    /**
     * Sets the ID which will be used for new net peer group instances.
     * <p/>
     * <p/>By Setting an alternate infrastructure PeerGroup ID (aka NetPeerGroup),
     * it prevents heterogeneous infrastructure PeerGroups from intersecting.
     * <p/>This is highly recommended practice for application deployment
     *
     * @param id the new infrastructure PeerGroupID as a string
     * @see net.jxta.peergroup.PeerGroupFactory#setNetPGID
     */
    public void setInfrastructureID(ID id) {
        if (id == null || id.equals(ID.nullID)) {
            throw new IllegalArgumentException("PeerGroupID can not be null");
        }

    }

    /**
     * Sets the ID which will be used for new net peer group instances.
     * <p/>
     * <p/>By Setting an alternate infrastructure PeerGroup ID (aka NetPeerGroup),
     * it prevents heterogeneous infrastructure PeerGroups from intersecting.
     * <p/>This is highly recommended practice for application deployment
     *
     * @param idStr the new infrastructure PeerGroupID as a string
     * @see net.jxta.peergroup.PeerGroupFactory#setNetPGID
     */
    public void setInfrastructureID(String idStr) {
        if (idStr == null || idStr.length() == 0) {
            throw new IllegalArgumentException("PeerGroupID string can not be empty or null");
        }

        PeerGroupID pgid = (PeerGroupID) ID.create(URI.create(idStr));

        setInfrastructureID(pgid);
    }

    /**
     * Sets the current node configuration mode.
     * <p/>The default mode is EDGE, unless modified at construction time.
     * A node configuration mode defined a preset configuration
     * parameters based on a operating mode. i.e. an EDGE mode, enable
     * client/server side tcp, multicast, client side http, RelayService
     * client mode.
     * <p/> Valid modes include EDGE, RDV_SERVER,
     * RELAY_OFF, RELAY_CLIENT, RELAY_SERVER, PROXY_SERVER, or any combination
     * of which.<p/> e.g. RDV_SERVER + RELAY_SERVER
     *
     * @param mode the new configuration mode
     * @see #getMode
     */
    public void setMode(int mode) {
    	//Log.d(P2PStreaming.TAG, "setMode");
        this.mode = mode;
        if ((mode & PROXY_SERVER) == PROXY_SERVER && ((mode & RELAY_SERVER) != RELAY_SERVER)) {

            /*
             // A proxy must at the very least support relaying
             if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
             LOG.log(Level.INFO, "Mode defined PROXY_SERVER, and no RELAY_SERVER, adding RELAY_SERVER support");
             }
             */
            mode = mode | RELAY_SERVER;
        }

        // RELAY config
        relayConfig.setClientEnabled((mode & RELAY_CLIENT) == RELAY_CLIENT);
        relayConfig.setServerEnabled((mode & RELAY_SERVER) == RELAY_SERVER);
        
        //Log.d(P2PStreaming.TAG, "setMode");

        // RDV_SERVER
        if ((mode & RDV_SERVER) == RDV_SERVER) {
            rdvConfig.setConfiguration(RendezVousConfiguration.RENDEZVOUS);
        } else if ((mode & RDV_CLIENT) == RDV_CLIENT) {
            rdvConfig.setConfiguration(RendezVousConfiguration.EDGE);
        } else if ((mode & RDV_AD_HOC) == RDV_AD_HOC) {
            rdvConfig.setConfiguration(RendezVousConfiguration.AD_HOC);
        }

        // TCP
        tcpConfig.setClientEnabled((mode & TCP_CLIENT) == TCP_CLIENT);
        tcpConfig.setServerEnabled((mode & TCP_SERVER) == TCP_SERVER);

        // HTTP
        httpConfig.setClientEnabled((mode & HTTP_CLIENT) == HTTP_CLIENT);
        httpConfig.setServerEnabled((mode & HTTP_SERVER) == HTTP_SERVER);

        // Multicast
        tcpConfig.setMulticastState((mode & IP_MULTICAST) == IP_MULTICAST);

        // EDGE
        if (mode == EDGE_NODE) {
            rdvConfig.setConfiguration(RendezVousConfiguration.EDGE);
        }
    }

    /**
     * Returns the current configuration mode
     * <p/>The default mode is EDGE, unless modified at construction time or through
     * Method {@link NetworkConfigurator#setMode}.  A node configuration mode defined a preset configuration
     * parameters based on a operating mode. i.e. an EDGE mode, enable
     * client/server side tcp, multicast, client side http, RelayService
     * client mode.
     *
     * @return mode  the current mode value
     * @see #setMode
     */
    public int getMode() {
        return mode;
    }

    /**
     * Sets the IP group multicast packet size
     *
     * @param size the new multicast packet
     */
    public void setMulticastSize(int size) {
        tcpConfig.setMulticastSize(size);
    }

    /**
     * Gets the IP group multicast packet size
     *
     * @return the multicast packet
     */
    public int getMulticastSize() {
        return tcpConfig.getMulticastSize();
    }

    /**
     * Sets the IP group multicast address (default 224.0.1.85)
     *
     * @param mcastAddress the new multicast group address
     * @see #setMulticastPort
     */
    public void setMulticastAddress(String mcastAddress) {
        tcpConfig.setMulticastAddr(mcastAddress);
    }

    /**
     * Sets the IP group multicast port (default 1234)
     *
     * @param port the new IP group multicast port
     * @see #setMulticastAddress
     */
    public void setMulticastPort(int port) {
        tcpConfig.setMulticastPort(port);
    }

    /**
     * Sets the node name
     *
     * @param name node name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the node name
     *
     * @return node name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the Principal for the peer root certificate
     *
     * @param principal the new principal value
     * @see #setPassword
     * @see #getPrincipal
     * @see #setPrincipal
     */
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * Gets the Principal for the peer root certificate
     *
     * @return principal  if a principal is set, null otherwise
     * @see #setPassword
     * @see #getPrincipal
     * @see #setPrincipal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Sets the public certificate for this configuration.
     *
     * @param cert the new cert value
     */
    public void setCertificate(X509Certificate cert) {
        this.cert = cert;
    }

    /**
     * Returns the public certificate for this configuration.
     *
     * @return X509Certificate
     */
    public X509Certificate getCertificate() {
        return cert;
    }

    /**
     * Sets the Subject private key
     *
     * @param subjectPkey the subject private key
     */
    public void setPrivateKey(PrivateKey subjectPkey) {
        this.subjectPkey = subjectPkey;
    }

    /**
     * Gets the Subject private key
     *
     * @return the subject private key
     */
    public PrivateKey getPrivateKey() {
        return this.subjectPkey;
    }

    /**
     * Set freestanding keystore location
     *
     * @param keyStoreLocation the absolute location of the freestanding keystore
     */
    public void setKeyStoreLocation(URI keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
    }

    /**
     * Get the freestanding keystore location
     *
     * @return the location of the freestanding keystore
     */
    public URI getKeyStoreLocation() {
        return keyStoreLocation;
    }

    /**
     * Sets the password used to sign the private key of the root certificate
     *
     * @param password the new password value
     * @see #setPassword
     * @see #getPrincipal
     * @see #setPrincipal
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the password used to sign the private key of the root certificate
     *
     * @return password  if a password is set, null otherwise
     * @see #setPassword
     * @see #getPrincipal
     * @see #setPrincipal
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the PeerID (by default, a new PeerID is generated).
     * <p/>Note: Persist the PeerID generated, or use load()
     * to avoid overridding a node's PeerID between restarts.
     *
     * @param peerid the new <code>net.jxta.peer.PeerID</code>
     */
    public void setPeerID(PeerID peerid) {
        this.peerid = peerid;
    }

    /**
     * Get the PeerID
     *
     * @return peerid  the <code>net.jxta.peer.PeerID</code> value
     */
    public PeerID getPeerID() {
        return this.peerid;
    }

    /**
     * Sets Rendezvous Seeding URI
     * <p/>e.g. http://rdv.jxtahosts.net/cgi-bin/rendezvous.cgi?3
     *
     * @param seedURI Rendezvous service seeding URI
     */
    public void addRdvSeedingURI(URI seedURI) {
        rdvConfig.addSeedingURI(seedURI);
    }

    /**
     * Sets the RelayService maximum number of simultaneous relay clients
     *
     * @param relayMaxClients the new relayMaxClients value
     */
    public void setRelayMaxClients(int relayMaxClients) {
        if ((relayMaxClients != -1) && (relayMaxClients <= 0)) {
            throw new IllegalArgumentException("Relay Max Clients : " + relayMaxClients + " must be > 0");
        }
        relayConfig.setMaxClients(relayMaxClients);
    }

    /**
     * Sets the RelayService Seeding URI
     * <p/>e.g. http://rdv.jxtahosts.net/cgi-bin/relays.cgi?3
     * <p/>A seeding URI (when read) is expected to provide a list of
     * physical endpoint addresse(s) to relay peers
     *
     * @param seedURI RelayService seeding URI
     */
    public void addRelaySeedingURI(URI seedURI) {
        relayConfig.addSeedingURI(seedURI);
    }

    /**
     * Sets the RendezVousService maximum number of simultaneous rendezvous clients
     *
     * @param rdvMaxClients the new rendezvousMaxClients value
     */
    public void setRendezvousMaxClients(int rdvMaxClients) {
        if ((rdvMaxClients != -1) && (rdvMaxClients <= 0)) {
            throw new IllegalArgumentException("Rendezvous Max Clients : " + rdvMaxClients + " must be > 0");
        }
        rdvConfig.setMaxClients(rdvMaxClients);
    }

    /**
     * Toggles TCP transport state
     *
     * @param enabled if true, enables TCP transport
     */
    public void setTcpEnabled(boolean enabled) {
        this.tcpEnabled = enabled;
        if (!tcpEnabled) {
            tcpConfig.setClientEnabled(false);
            tcpConfig.setServerEnabled(false);
        }
    }

    /**
     * Sets the TCP transport listening port (default 9701)
     *
     * @param port the new tcpPort value
     */
    public void setTcpPort(int port) {
        tcpConfig.setPort(port);
    }

    /**
     * Sets the lowest port on which the TCP Transport will listen if configured
     * to do so. Valid values are <code>-1</code>, <code>0</code> and
     * <code>1-65535</code>. The <code>-1</code> value is used to signify that
     * the port range feature should be disabled. The <code>0</code> specifies
     * that the Socket API dynamic port allocation should be used. For values
     * <code>1-65535</code> the value must be equal to or less than the value
     * used for end port.
     *
     * @param start the lowest port on which to listen.
     */
    public void setTcpStartPort(int start) {
        tcpConfig.setStartPort(start);
    }

    /**
     * Return the highest port on which the TCP Transport will listen if
     * configured to do so. Valid values are <code>-1</code>, <code>0</code> and
     * <code>1-65535</code>. The <code>-1</code> value is used to signify that
     * the port range feature should be disabled. The <code>0</code> specifies
     * that the Socket API dynamic port allocation should be used. For values
     * <code>1-65535</code> the value must be equal to or greater than the value
     * used for start port.
     *
     * @param end the new TCP end port
     */
    public void setTcpEndPort(int end) {
        tcpConfig.setEndPort(end);
    }

    /**
     * Toggles TCP transport server (incoming) mode (default is on)
     *
     * @param incoming the new TCP server mode
     */
    public void setTcpIncoming(boolean incoming) {
        tcpConfig.setServerEnabled(incoming);
    }

    /**
     * Toggles TCP transport client (outgoing) mode (default is true)
     *
     * @param outgoing the new tcpOutgoing value
     */
    public void setTcpOutgoing(boolean outgoing) {
        tcpConfig.setClientEnabled(outgoing);
    }

    /**
     * Sets the TCP transport interface address
     * <p/>e.g. "192.168.1.1"
     *
     * @param address the TCP transport interface address
     */
    public void setTcpInterfaceAddress(String address) {
        tcpConfig.setInterfaceAddress(address);
    }

    /**
     * Sets the node public address
     * <p/>e.g. "192.168.1.1:9701"
     * <p/>This address is the physical address defined in a node's
     * AccessPointAdvertisement.  This often required for NAT'd/FW nodes
     *
     * @param address   the TCP transport public address
     * @param exclusive public address advertised exclusively
     */
    public void setTcpPublicAddress(String address, boolean exclusive) {
        tcpConfig.setServer(address);
        tcpConfig.setPublicAddressOnly(exclusive);
    }

    /**
     * Toggles whether to use IP group multicast (default is true)
     *
     * @param mutlicastOn the new useMulticast value
     */
    public void setUseMulticast(boolean mutlicastOn) {
        tcpConfig.setMulticastState(mutlicastOn);
    }

    /**
     * Determines whether to restrict RelayService leases to those defined in
     * the seed list
     *
     * @param useOnlyRelaySeeds restrict RelayService lease to seed list
     */
    public void setUseOnlyRelaySeeds(boolean useOnlyRelaySeeds) {
        relayConfig.setUseOnlySeeds(useOnlyRelaySeeds);
    }

    /**
     * Determines whether to restrict RendezvousService leases to those defined in
     * the seed list
     *
     * @param useOnlyRendezvouSeeds restrict RendezvousService lease to seed list
     */
    public void setUseOnlyRendezvousSeeds(boolean useOnlyRendezvouSeeds) {
        rdvConfig.setUseOnlySeeds(useOnlyRendezvouSeeds);
    }

    /**
     * Adds RelayService peer seed address
     * <p/>A RelayService seed is defined as a physical endpoint address
     * <p/>e.g. http://192.168.1.1:9700, or tcp://192.168.1.1:9701
     *
     * @param seedURI the relay seed URI
     */
    public void addSeedRelay(URI seedURI) {
        relayConfig.addSeedRelay(seedURI.toString());
    }

    /**
     * Adds Rendezvous peer seed, physical endpoint address
     * <p/>A RendezVousService seed is defined as a physical endpoint address
     * <p/>e.g. http://192.168.1.1:9700, or tcp://192.168.1.1:9701
     *
     * @param seedURI the rendezvous seed URI
     */
    public void addSeedRendezvous(URI seedURI) {
        rdvConfig.addSeedRendezvous(seedURI);
    }

    /**
     * Returns true if a PlatformConfig file exist under store home
     *
     * @return true if a PlatformConfig file exist under store home
     */
    public boolean exists() {
        URI platformConfig = storeHome.resolve("PlatformConfig");

        try {
        	//Log.d(P2PStreaming.TAG,"NetworkConfigurator-exists TRY CALL " + platformConfig.getPath());
      
        	return null != read(platformConfig);
        } catch (IOException failed) {
        	
        	//Log.d(P2PStreaming.TAG,"NetworkConfigurator-exists CATCH CALL ");
        	
            return false;
        }
    }

    /**
     * Sets the PeerID for this Configuration
     *
     * @param peerIdStr the new PeerID as a string
     */
    public void setPeerId(String peerIdStr) {
        this.peerid = (PeerID) ID.create(URI.create(peerIdStr));
    }

    /**
     * Sets the new RendezvousService seeding URI as a string.
     * <p/>A seeding URI (when read) is expected to provide a list of
     * physical endpoint address to rendezvous peers
     *
     * @param seedURIStr the new rendezvous seed URI as a string
     */
    public void addRdvSeedingURI(String seedURIStr) {
        rdvConfig.addSeedingURI(URI.create(seedURIStr));
    }

    /**
     * Sets the new RelayService seeding URI as a string.
     * <p/>A seeding URI (when read) is expected to provide a list of
     * physical endpoint address to relay peers
     *
     * @param seedURIStr the new RelayService seed URI as a string
     */
    public void addRelaySeedingURI(String seedURIStr) {
        relayConfig.addSeedingURI(URI.create(seedURIStr));
    }

    /**
     * Sets the List relaySeeds represented as Strings
     * <p/>A RelayService seed is defined as a physical endpoint address
     * <p/>e.g. http://192.168.1.1:9700, or tcp://192.168.1.1:9701
     *
     * @param seeds the Set RelayService seed URIs as a string
     */
    public void setRelaySeedURIs(List seeds) {
        relayConfig.clearSeedRelays();
        for (Iterator it = seeds.iterator(); it.hasNext();) {
            String seedStr = (String) it.next();
            relayConfig.addSeedRelay(new EndpointAddress(seedStr));
        }
    }

    /**
     * Sets the relaySeeds represented as Strings
     * <p/>A seeding URI (when read) is expected to provide a list of
     * physical endpoint address to relay peers
     *
     * @param seedURIs the List relaySeeds represented as Strings
     */
    public void setRelaySeedingURIs(Set seedURIs) {
        relayConfig.clearSeedingURIs();
        for (Iterator it = seedURIs.iterator(); it.hasNext();) {
            String seedStr = (String) it.next();
            relayConfig.addSeedingURI(URI.create(seedStr));
        }
    }

    /**
     * Clears the List of RelayService seeds
     */
    public void clearRelaySeeds() {
        relayConfig.clearSeedRelays();
    }

    /**
     * Clears the List of RelayService seeding URIs
     */
    public void clearRelaySeedingURIs() {
        relayConfig.clearSeedingURIs();
    }

    /**
     * Sets the List of RendezVousService seeds represented as Strings
     * <p/>A RendezvousService seed is defined as a physical endpoint address
     * <p/>e.g. http://192.168.1.1:9700, or tcp://192.168.1.1:9701
     *
     * @param seeds the Set of rendezvousSeeds represented as Strings
     */
    public void setRendezvousSeeds(Set seeds) {
        rdvConfig.clearSeedRendezvous();
        for (Iterator it = seeds.iterator(); it.hasNext();) {
            String seedStr = (String) it.next();
            rdvConfig.addSeedRendezvous(URI.create(seedStr));
        }
    }

    /**
     * Sets the List of RendezVousService seeds represented as Strings
     * <p/>A seeding URI (when read) is expected to provide a list of
     * physical endpoint address to relay peers
     *
     * @param seedURIs the List rendezvousSeeds represented as Strings
     */
    public void setRendezvousSeedURIs(List seedURIs) {
        rdvConfig.clearSeedingURIs();
        for (Iterator it = seedURIs.iterator(); it.hasNext();) {
            String seedStr = (String) it.next();
            rdvConfig.addSeedingURI(URI.create(seedStr));
        }
    }

    /**
     * Clears the List of RendezVousService seeds
     */
    public void clearRendezvousSeeds() {
        rdvConfig.clearSeedRendezvous();
    }

    /**
     * Clears the List of RendezVousService seeds
     */
    public void clearRendezvousSeedURIs() {
        rdvConfig.clearSeedingURIs();
    }

    /**
     * Load a configuration from the specified store home uri
     * <p/>
     * e.g. file:/export/dist/EdgeConfig.xml, e.g. http://configserver.net/configservice?Edge
     *
     * @return The loaded configuration.
     * @throws IOException          if an i/o error occurs
     * @throws CertificateException if the MembershipService is invalid
     */
    public ConfigParams load() throws IOException, CertificateException {
        return load(storeHome.resolve("PlatformConfig"));
    }

    /**
     * Load a configuration from a specified uri
     * <p/>
     * e.g. file:/export/dist/EdgeConfig.xml, e.g. http://configserver.net/configservice?Edge
     *
     * @param uri the URI to PlatformConfig
     * @return The loaded configuration.
     * @throws IOException          if an i/o error occurs
     * @throws CertificateException if the MemebershipService is invalid
     */
    public ConfigParams load(URI uri) throws IOException, CertificateException {
        if (uri == null) {
            throw new IllegalArgumentException("URI can not be null");
        }

        PlatformConfig platformConfig = read(uri);

        name = platformConfig.getName();
        peerid = platformConfig.getPeerID();
        description = platformConfig.getDescription();

        // TCP
        XMLElement param = (XMLElement) platformConfig.getServiceParam(PeerGroup.tcpProtoClassID);
        tcpEnabled = !param.getChildren("isOff").hasMoreElements();
        Enumeration tcpChilds = param.getChildren(TransportAdvertisement.getAdvertisementType());

        // get the TransportAdv from either TransportAdv or tcpConfig
        if (tcpChilds.hasMoreElements()) {
            param = (XMLElement) tcpChilds.nextElement();
        } else {
            throw new IllegalStateException("Missing TCP Advertisment");
        }
        tcpConfig = (TCPAdv) AdvertisementFactory.newAdvertisement(param);

        /*
         if (tcpConfig.isClientEnabled()) {
         mode = mode | TCP_CLIENT;
         }
         if (tcpConfig.isServerEnabled()) {
         mode = mode | TCP_SERVER;
         }*/

        // HTTP
        try {
            param = (XMLElement) platformConfig.getServiceParam(PeerGroup.httpProtoClassID);
            httpEnabled = !param.getChildren("isOff").hasMoreElements();

            Enumeration httpChilds = param.getChildren(TransportAdvertisement.getAdvertisementType());

            // get the TransportAdv from either TransportAdv
            if (httpChilds.hasMoreElements()) {
                param = (XMLElement) httpChilds.nextElement();
            } else {
                throw new IllegalStateException("Missing HTTP Advertisment");
            }
            // Read-in the adv as it is now.
            httpConfig = (HTTPAdv) AdvertisementFactory.newAdvertisement(param);

            /* if (httpConfig.isClientEnabled()) {
             mode = mode | HTTP_CLIENT;
             }
             if (httpConfig.isServerEnabled()) {
             mode = mode | HTTP_SERVER;
             }
             */
        } catch (Exception failure) {
            IOException ioe = new IOException("error processing the HTTP config advertisement");
            ioe.initCause(failure);
            throw ioe;
        }

        // Rendezvous
        try {
            param = (XMLElement) platformConfig.getServiceParam(PeerGroup.rendezvousClassID);
            // backwards compatibility
            param.addAttribute("type", RdvConfigAdv.getAdvertisementType());
            rdvConfig = (RdvConfigAdv) AdvertisementFactory.newAdvertisement(param);
            if (rdvConfig.getConfiguration() == RendezVousConfiguration.AD_HOC) {
                mode = mode | RDV_AD_HOC;
            } else if (rdvConfig.getConfiguration() == RendezVousConfiguration.EDGE) {
                mode = mode | RDV_CLIENT;
            } else if (rdvConfig.getConfiguration() == RendezVousConfiguration.RENDEZVOUS) {
                mode = mode | RDV_SERVER;
            }
        } catch (Exception failure) {
            IOException ioe = new IOException("error processing the rendezvous config advertisement");
            ioe.initCause(failure);
            throw ioe;
        }

        // Relay
        try {
            param = (XMLElement) platformConfig.getServiceParam(PeerGroup.relayProtoClassID);
            if (param != null && !param.getChildren("isOff").hasMoreElements()) {
                mode = mode | RELAY_OFF;
            }
            // backwards compatibility
            param.addAttribute("type", RelayConfigAdv.getAdvertisementType());
            relayConfig = (RelayConfigAdv) AdvertisementFactory.newAdvertisement(param);

        } catch (Exception failure) {
            IOException ioe = new IOException("error processing the relay config advertisement");
            ioe.initCause(failure);
            throw ioe;
        }

        return platformConfig;
    }

    /**
     * Persists a PlatformConfig advertisement under getStoreHome()+"/PlaformConfig"
     * <p/>
     * Home may be overridden by a call to setHome()
     *
     * @throws IOException If there is a failure saving the PlatformConfig.
     * @see #load
     */
    public void save() throws IOException {
        httpEnabled = (httpConfig.isClientEnabled() || httpConfig.isServerEnabled());
        tcpEnabled = (tcpConfig.isClientEnabled() || tcpConfig.isServerEnabled());
        ConfigParams advertisement = getPlatformConfig();
        OutputStream out = null;

        try {
            if ("file".equalsIgnoreCase(storeHome.getScheme())) {
                File saveDir = new File(storeHome);
                saveDir.mkdirs();

                // Sadly we can't use URL.openConnection() to create the
                // OutputStream for file:// URLs. bogus.
                out = new FileOutputStream(new File(saveDir, "PlatformConfig"));
            } else {
                out = storeHome.resolve("PlatformConfig").toURL().openConnection().getOutputStream();
            }

            XMLDocument aDoc = (XMLDocument) advertisement.getDocument(MimeMediaType.XMLUTF8);
            OutputStreamWriter os = new OutputStreamWriter(out, "UTF-8");

            aDoc.sendToWriter(os);
            os.flush();
        } finally {
            if (null != out) {
                out.close();
            }
        }
    }

    /**
     * Returns a XMLDocument representation of an Advertisement
     *
     * @param enabled whether the param doc is enabled, adds a "isOff"
     *                element if disabled
     * @param adv     the Advertisement to retrieve the param doc from
     * @return the parmDoc value
     */
    protected XMLDocument getParmDoc(boolean enabled, Advertisement adv) {
        XMLDocument parmDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
        XMLDocument doc = (XMLDocument) adv.getDocument(MimeMediaType.XMLUTF8);

        StructuredDocumentUtils.copyElements(parmDoc, parmDoc, doc);
        if (!enabled) {
            parmDoc.appendChild(parmDoc.createElement("isOff"));
        }

        return parmDoc;
    }

    /**
     * Creates an HTTP transport advertisement
     *
     * @return an HTTP transport advertisement
     */
    protected HTTPAdv createHttpAdv() {
        httpConfig = (HTTPAdv) AdvertisementFactory.newAdvertisement(HTTPAdv.getAdvertisementType());
        //Log.d(P2PStreaming.TAG, "create HTTP adv 1");
        httpConfig.setProtocol("http");
        httpConfig.setPort(9700);
        httpConfig.setClientEnabled((mode & HTTP_CLIENT) == HTTP_CLIENT);
        httpConfig.setServerEnabled((mode & HTTP_SERVER) == HTTP_SERVER);
        return httpConfig;
    }

    /**
     * Creates a ProxyService configuration advertisement
     *
     * @return ProxyService configuration advertisement
     */
    protected XMLDocument createProxyAdv() {
        return (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
    }

    /**
     * Creates a RendezVousService configuration advertisement with default values (EDGE)
     *
     * @return a RdvConfigAdv
     */
    protected RdvConfigAdv createRdvConfigAdv() {
        rdvConfig = (RdvConfigAdv) AdvertisementFactory.newAdvertisement(RdvConfigAdv.getAdvertisementType());
        if (mode == RDV_AD_HOC) {
            rdvConfig.setConfiguration(RendezVousConfiguration.AD_HOC);
        } else if ((mode & RDV_CLIENT) == RDV_CLIENT) {
            rdvConfig.setConfiguration(RendezVousConfiguration.EDGE);
        } else if ((mode & RDV_SERVER) == RDV_SERVER) {
            rdvConfig.setConfiguration(RendezVousConfiguration.RENDEZVOUS);
        }
        // A better alternative is to reference rdv service defaults (currently private)
        // rdvConfig.setMaxClients(200);
        return rdvConfig;
    }

    /**
     * Creates a RelayService configuration advertisement with default values (EDGE)
     *
     * @return a RelayConfigAdv
     */
    protected RelayConfigAdv createRelayConfigAdv() {
        relayConfig = (RelayConfigAdv) AdvertisementFactory.newAdvertisement(RelayConfigAdv.getAdvertisementType());
        relayConfig.setUseOnlySeeds(false);
        relayConfig.setClientEnabled((mode & RELAY_CLIENT) == RELAY_CLIENT || mode == EDGE_NODE);
        relayConfig.setServerEnabled((mode & RELAY_SERVER) == RELAY_SERVER);
        return relayConfig;
    }

    /**
     * Creates an TCP transport advertisement with the platform default values.
     * multicast on, 224.0.1.85:1234, with a max packet size of 16K
     *
     * @return a TCP transport advertisement
     */
    protected TCPAdv createTcpAdv() {
        tcpConfig = (TCPAdv) AdvertisementFactory.newAdvertisement(TCPAdv.getAdvertisementType());
        tcpConfig.setProtocol("tcp");
        tcpConfig.setInterfaceAddress(null);
        tcpConfig.setPort(9701);
        tcpConfig.setStartPort(9701);
        tcpConfig.setEndPort(9799);
        tcpConfig.setMulticastAddr("224.0.1.85");
        tcpConfig.setMulticastPort(1234);
        tcpConfig.setMulticastSize(16384);
        tcpConfig.setMulticastState((mode & IP_MULTICAST) == IP_MULTICAST);
        tcpConfig.setServer(null);
        tcpConfig.setClientEnabled((mode & TCP_CLIENT) == TCP_CLIENT);
        tcpConfig.setServerEnabled((mode & TCP_SERVER) == TCP_SERVER);
        return tcpConfig;
    }

    /**
     * Returns a PlatformConfig which represents a platform configuration.
     * <p/>Fine tuning is achieved through accessing each configured advertisement
     * and achieved through accessing each configured advertisement and modifying
     * each object directly.
     *
     * @return the PeerPlatformConfig Advertisement
     */
    public ConfigParams getPlatformConfig() {
        PlatformConfig advertisement = (PlatformConfig) AdvertisementFactory.newAdvertisement(
                PlatformConfig.getAdvertisementType());

        advertisement.setName(name);
        advertisement.setDescription(description);
        if (peerid != null) {
            advertisement.setPeerID(peerid);
        }

        if (tcpConfig != null) {
            boolean enabled = tcpEnabled && (tcpConfig.isServerEnabled() || tcpConfig.isClientEnabled());
            advertisement.putServiceParam(PeerGroup.tcpProtoClassID, getParmDoc(enabled, tcpConfig));
        }

        if (httpConfig != null) {
            boolean enabled = httpEnabled && (httpConfig.isServerEnabled() || httpConfig.isClientEnabled());
            advertisement.putServiceParam(PeerGroup.httpProtoClassID, getParmDoc(enabled, httpConfig));
        }

        if (relayConfig != null) {
            boolean isOff = ((mode & RELAY_OFF) == RELAY_OFF) || (relayConfig.isServerEnabled() && relayConfig.isClientEnabled());
            XMLDocument relayDoc = (XMLDocument) relayConfig.getDocument(MimeMediaType.XMLUTF8);

            if (isOff) {
                relayDoc.appendChild(relayDoc.createElement("isOff"));
            }
            advertisement.putServiceParam(PeerGroup.relayProtoClassID, relayDoc);
        }

        if (rdvConfig != null) {
            XMLDocument rdvDoc = (XMLDocument) rdvConfig.getDocument(MimeMediaType.XMLUTF8);
            advertisement.putServiceParam(PeerGroup.rendezvousClassID, rdvDoc);
        }

        return advertisement;
    }

    /**
     * @param location The location of the platform config.
     * @return The platformConfig
     * @throws IOException Thrown for failures reading the PlatformConfig.
     */
    private PlatformConfig read(URI location) throws IOException {
        URL url;

        try {
            url = location.toURL();
        } catch (MalformedURLException mue) {
            IllegalArgumentException failure = new IllegalArgumentException("Failed to convert URI to URL");

            failure.initCause(mue);
            throw failure;
        }

        InputStream input = url.openStream();
        try {
        
        	XMLDocument document = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, input);
            
           // Log.d(P2PStreaming.TAG,"NetworkConfigurator-read TRY INPUTSTREAM");
            
            return (PlatformConfig) AdvertisementFactory.newAdvertisement(document);
        
        } finally {
            input.close();
        }
    }

    /**
     * Holds the construction tunables for the Net Peer Group. This consists of
     * the peer group id, the peer group name and the peer group description.
     */
    static class NetGroupTunables {

        final ID id;
        final String name;
        final XMLElement desc;

        /**
         * Constructor for loading the default Net Peer Group construction
         * tunables.
         */
        NetGroupTunables() {
            id = PeerGroupID.defaultNetPeerGroupID;
            name = "NetPeerGroup";
            desc = (XMLElement) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "desc", "default Net Peer Group");
        }

        /**
         * Constructor for loading the default Net Peer Group construction
         * tunables.
         *
         * @param pgid   the PeerGroupID
         * @param pgname the group name
         * @param pgdesc the group description
         */
        NetGroupTunables(ID pgid, String pgname, XMLElement pgdesc) {
            id = pgid;
            name = pgname;
            desc = pgdesc;
        }

        /**
         * Constructor for loading the Net Peer Group construction
         * tunables from the provided resource bundle.
         *
         * @param rsrcs    The resource bundle from which resources will be loaded.
         * @param defaults default values
         */
        NetGroupTunables(ResourceBundle rsrcs, NetGroupTunables defaults) {
            ID idTmp;
            String nameTmp;
            XMLElement descTmp;

            try {
                String idTmpStr = rsrcs.getString("NetPeerGroupID").trim();

                if (idTmpStr.startsWith(ID.URNNamespace + ":")) {
                    idTmpStr = idTmpStr.substring(5);
                }
                idTmp = IDFactory.fromURI(new URI(ID.URIEncodingName + ":" + ID.URNNamespace + ":" + idTmpStr));
                nameTmp = rsrcs.getString("NetPeerGroupName").trim();
                descTmp = (XMLElement) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "desc", rsrcs.getString("NetPeerGroupDesc").trim());
            } catch (Exception failed) {
                if (null != defaults) {
                    idTmp = defaults.id;
                    nameTmp = defaults.name;
                    descTmp = defaults.desc;
                } else {
                    throw new IllegalStateException("NetPeerGroup tunables not defined or could not be loaded.");
                }
            }

            id = idTmp;
            name = nameTmp;
            desc = descTmp;
        }
    }
}
