# Distributed architecture with JAVA RMI

## Purpose !

When a task or a process is requiring too much ressources, it is sometimes convenient to split and share the work between peers for a better benefit of the distributed systems so saving time is ensured. Goal of this work is to show an example of an architecture that uses a "smart" Load Balancer giving jobs to different processing servers via Java RMI calls. Servers return a result to the LB that checks the result in a secure mode and doesn't if secure mode is deactivated.


## Requirements

Java environnement set up on every station

## Getting Started

- Begin by cloning/forking the repo.

### Compilation

Simply execute the 'ant' command from root directory (Apache Ant is required on the system)

```
ant
```

!! This needs to be done on every server and LB you use for testing purpose !!


### Load Balancer Conf File

The syntax of LB's configuration file must be like the following :

```
  name-server-1 @server1 @port-rmiregistry-server1
  name-server-2 @server2 @port-rmiregistry-server2
  ...

```

Parameters :
  - name-server-X : Name of the server during the start up.

  - @serverX : IP adress of the server.

  - @port-rmiregistry-serverX : Port number used to join the RMI service.

If you have launched 3 different servers so your configuration file must look like the following :

```
  Example with default config.txt :

  toto 132.207.12.177 5023
  toto2 132.207.12.179 5023
  toto3 132.207.12.183 5023
```

### Execution of processing servers

- Start the RMI registry with the rmiregistry command from the bin folder. Choose a port between 5000 5050 (5023 if a good default port value !). Don't forget to add an '&' to execute the rmiregistry outside of the current bash.

```
/opt/java/jdk8.x86_64/bin/rmiregistry 5023 & 
```

- Execute the following command :

```
chmod +x ./serveur
```

- Start the server with the following command 

./serveur nameOfServer nbrOperationMax thresholdError registryAdress registryPort

Example :

```
./serveur foo 10 0 0 5023  
```

- Start as many server you like on separate station (preferably for accurate network delay accounting)

### Execution of the Load Balancer

- Before starting the LB, make sure of having set configuration file. See Load Balancer configuration file section.

- Execute the following command :

```
chmod +x ./repartiteur ./repartiteurNonSecur
```

- Start the LB with the following command 

./repartiteur {operations-file}.txt config.txt

Example :

```
./repartiteur donnees-4172.txt config.txt  
```

- Wait for the result to show up on the screen

## Resources

This method has been implemented during Distributed Systems and Cloud Computing course (INF4410) within Ecole Polytechnique Montreal.
