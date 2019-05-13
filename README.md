# FreePastry
## What is it?
**Pastry** is a generic, scalable and efficient substrate for peer-to-peer applications. Pastry nodes form a decentralized, self-organizing and fault-tolerant overlay network within the Internet. Pastry provides efficient request routing, deterministic object location, and load balancing in an application-independent manner. Furthermore, Pastry provides mechanisms that support and facilitate application-specific object replication, caching, and fault recovery.

## Pastry overview

Pastry provides the following capabilities. First, each node in the Pastry network has a unique, uniform random identifier (nodeId) in a circular 128-bit identifier space. When presented with a message and a numeric 128-bit key, a Pastry node efficiently routes the message to the node with a nodeId that is numerically closest to the key, among all currently live Pastry nodes. The expected number of forwarding steps in the Pastry overlay network is O(log N), while the size of the routing table maintained in each Pastry node is only O(log N) in size (where N is the number of live Pastry nodes in the overlay network). At each Pastry node along the route that a message takes, the application is notified and may perform application-specific computations related to the message.

Second, each Pastry node keeps track of its L immediate neighbors in the nodeId space (called the leaf set), and notifies applications of new node arrivals, node failures and node recoveries within the leaf set. Third, Pastry takes into account locality (proximity) in the underlying Internet; it seeks to minimize the distance messages travel, according to a scalar proximity metric like the ping delay. Pastry is completely decentralized, scalable, and self-organizing; it automatically adapts to the arrival, departure and failure of nodes. P2P applications built upon Pastry can utilize its capabilities in many ways, including:

* **Mapping application objects to Pastry nodes**: Application-specific objects are assigned unique, uniform random identifiers (objIds) and mapped to the k, (k >= 1) nodes with nodeIds numerically closest to the objId. The number k reflects the application's desired degree of replication for the object.
* **Inserting objects**: Application-specific objects can be inserted by routing a Pastry message, using the objId as the key. When the message reaches a node with one of the k closest nodeIds to the objId, that node replicates the object among the other k-1 nodes with closest nodeIds (which are, by definition, in the same leaf set for k <= L/2).
* **Accessing objects**: Application-specific objects can be looked up, contacted, or retrieved by routing a Pastry message, using the objId as the key. By definition, the message is guaranteed to reach a node that maintains a replica of the requested object unless all k nodes with nodeIds closest to the objId have failed.
* **Availability and persistence**: Applications interested in availability and persistence of application-specific objects maintain the following invariant as nodes join, fail and recover: object replicas are maintained on the k nodes with numerically closest nodeIds to the objId, for k > 1. The fact that Pastry maintains leaf sets and notifies applications of changes in the set's membership simplifies the task of maintaining this invariant.
* **Diversity**: The assignment of nodeIds is uniform random, and cannot be corrupted by an attacker. Thus, with high probability, nodes with adjacent nodeIds are diverse in geographic location, ownership, jurisdiction, network attachment, etc. Therefore, the probability that such a set of nodes is conspiring or suffers from correlated failures is low even for modest set sizes. This minimizes the probability of a simultaneous failure of all k nodes that maintain an object replica. Likewise, quorum-based protocols can be used to securely update and query the state of replicated objects, despite the presence of a limited number of malicious nodes in the system.
* **Load balancing**: Both nodeIds and objIds are randomly assigned and uniformly distributed in the 128-bit Pastry identifier space. Without requiring any global coordination, this results in a good first-order balance of storage requirements and query load among the Pastry nodes, as well as network load in the underlying Internet.
* **Object caching**: Applications can cache objects on the Pastry nodes encountered along the paths taken by insert and lookup messages. Subsequent lookup requests whose paths intersect are served the cached copy. (Pastry's network locality properties make it likely that messages routed with the same key from nearby nodes converge early, thus lookups are likely to intercept nearby cached objects.) This distributed caching offloads the k nodes that hold the primary replicas of an object, and it minimizes client delays and network traffic by dynamically caching copies near interested clients.
* **Efficient, scalable information dissemination**: Applications can perform efficient multicast using reverse path forwarding along the tree formed by the routes from clients to the node with nodeId numerically closest to a given objId. Pastry's network locality properties ensure that the resulting multicast trees are efficient; i.e., they result in efficient data delivery and resource usage in the underlying Internet.