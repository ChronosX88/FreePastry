/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice  University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/ 
package rice.tutorial.remotesocket;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;

import rice.Continuation;
import rice.environment.Environment;
import rice.environment.params.simple.SimpleParameters;
import rice.p2p.commonapi.*;
import rice.p2p.commonapi.appsocket.AppSocket;
import rice.p2p.commonapi.appsocket.AppSocketReceiver;
import rice.pastry.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.direct.*;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.socket.appsocket.AppSocketPastryNodeFactory;
import rice.pastry.socket.appsocket.SocketFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.pastry.standard.StandardAddress;

/**
 * This tutorial shows how to setup a FreePastry node using the Socket Protocol.
 * 
 * @author Jeff Hoye
 */
public class Tutorial {
  
  // this will keep track of our applications
  Vector<MyApp> apps = new Vector<MyApp>();
  
  /**
   * This constructor launches numNodes PastryNodes.  They will bootstrap 
   * to an existing ring if one exists at the specified location, otherwise
   * it will start a new ring.
   * 
   * @param bindport the local port to bind to 
   * @param bootaddress the IP:port of the node to boot from
   * @param numNodes the number of nodes to create in this JVM
   * @param env the environment for these nodes
   * @param useDirect true for the simulator, false for the socket protocol
   */
  public Tutorial(int bindport, InetSocketAddress bootaddress, int numNodes, Environment env, boolean useDirect) throws Exception {
    
    // Generate the NodeIds Randomly
    NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
    
    // construct the PastryNodeFactory
    AppSocketPastryNodeFactory factory = new AppSocketPastryNodeFactory(nidFactory, bindport, env);
    
    IdFactory idFactory = new PastryIdFactory(env);
    
    NodeHandle bootHandle = null;
    
    // loop to construct the nodes/apps
    for (int curNode = 0; curNode < numNodes; curNode++) {
      // This will return null if we there is no node at that location
  
      // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
      PastryNode node = factory.newNode();

      // construct a new MyApp
      MyApp app = new MyApp(node, idFactory);
      
      apps.add(app);

      node.boot(bootaddress);
      
      // the node may require sending several messages to fully boot into the ring
      synchronized(node) {
        while(!node.isReady() && !node.joinFailed()) {
          // delay so we don't busy-wait
          node.wait(500);
          
          // abort if can't join
          if (node.joinFailed()) {
            throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason()); 
          }
        }       
      }
      
      System.out.println("Finished creating new node "+node);      
    }
      
    // wait 1 second
    env.getTimeSource().sleep(1000);

    // get the SocketFactory
    SocketFactory sFactory = factory.getSocketFactory();
    
    // open the AppSocket
    sFactory.getAppSocket(bootaddress, StandardAddress.getAddress(MyApp.class,"myinstance",env), new Continuation<AppSocket, Exception>() {

      /**
       * Receive the new AppSocket.
       * @param result
       */
      public void receiveResult(AppSocket result) {
        System.out.println("Opened AppSocket "+result);
        final ByteBuffer out = ByteBuffer.wrap(rice.pastry.Id.build().toByteArray());

        result.register(false, true, -1, new AppSocketReceiver() {
          
          /**
           * Example of how to write some bytes
           */
          public void receiveSelectResult(AppSocket socket, boolean canRead, boolean canWrite) {   
            try {
              long ret = socket.write(out);    
              System.out.println("Wrote "+ret+" bytes.");
              // see if we are done
              if (out.hasRemaining()) {
                // keep writing
                socket.register(false, true, 30000, this); 
              } else {
                socket.shutdownOutput();           
                out.clear();
              }
            } catch (IOException ioe) {
              ioe.printStackTrace(); 
            }
          }
          
          /**
           * Called if there is a problem.
           */
          public void receiveException(AppSocket socket, Exception e) {
            e.printStackTrace();
          }
          
          /**
           * Called when the socket comes available.
           */
          public void receiveSocket(AppSocket socket) {
            // register for writing
            throw new RuntimeException("Should never be called.");                    
          }              
        });

      }
      
      /**
       * Handle a problem connecting.
       * @param exception
       */
      public void receiveException(Exception exception) {
        exception.printStackTrace();
        throw new RuntimeException("Not implemented. ");        
      }      
    }, null);    
  }

  /**
   * Usage: 
   * java [-cp FreePastry-<version>.jar] rice.tutorial.appsocket.Tutorial localbindport bootIP bootPort numNodes
   *   or
   * java [-cp FreePastry-<version>.jar] rice.tutorial.appsocket.Tutorial -direct numNodes
   * 
   * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001 10
   * example java rice.tutorial.DistTutorial -direct 10
   */
  public static void main(String[] args) throws Exception {
    try {
    
      boolean useDirect;
      if (args[0].equalsIgnoreCase("-direct")) {
        useDirect = true;
      } else {
        useDirect = false; 
      }
            
      // Loads pastry settings
      Environment env;
      if (useDirect) {
        env = Environment.directEnvironment();
      } else {
        env = new Environment(); 
        
        // disable the UPnP setting (in case you are testing this on a NATted LAN)
        env.getParameters().setString("nat_search_policy","never");      
      }
    
      int bindport = 0;
      InetSocketAddress bootaddress = null;
      
      // the number of nodes to use is always the last param
      int numNodes = Integer.parseInt(args[args.length-1]);    
      
      if (!useDirect) {
        // the port to use locally
        bindport = Integer.parseInt(args[0]);
        
        // build the bootaddress from the command line args
        InetAddress bootaddr = InetAddress.getByName(args[1]);
        int bootport = Integer.parseInt(args[2]);
        bootaddress = new InetSocketAddress(bootaddr,bootport);    
      }
      
      // launch our node!
      Tutorial dt = new Tutorial(bindport, bootaddress, numNodes, env, useDirect);
    } catch (Exception e) {
      // remind user how to use
      System.out.println("Usage:"); 
      System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.appsocket.Tutorial localbindport bootIP bootPort numNodes");
      System.out.println("  or");
      System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.appsocket.Tutorial -direct numNodes");
      System.out.println();
      System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001 10");
      System.out.println("example java rice.tutorial.DistTutorial -direct 10");
      throw e; 
    }
  }
}
