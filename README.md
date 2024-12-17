# integration-sample


### Setup the queue manager

A queue manager can be deployed using the provided resource in the assets.  
The queue manager that will be created is using ephemeral storage and the channel authentication securities have been removed.  
A user admin with simple password "passw0rd" is configured to access the web console.  

1. Create the queue manager authentication for the web console
```
oc apply -f qmgr-mq-web-cm.yaml
```
2. Create the queue manager script that will be used to configure the queues and channels:
```
oc apply -f qmgr-mq-mqsc.yaml
```
3. Create the queue manager:
```
oc apply -f qmgr-cr.yaml
```

To check if the queue manager is correctly created:
```
oc get queuemanager
NAME       PHASE
mqm-demo   Running
```
To get the route to access the console:
```
oc get route mqm-demo-ibm-mq-web -ojsonpath={.status.ingress[].host}
```

<img src="assets/images/mq-web-ui.png" width="80%" >

The queue LQ.PRODUCT.ORDERS will receive messages from the Java putter application and Kafka Connect will get them and publish them into the Kafka topic "PRODUCT.ORDERS".   

### Build the Java Putter application

The Java putter application is a small modification from the [ibmmq kafka demos](https://github.com/dalelane/kafka-demos/tree/master/apps/ibmmq). The Putter source has been modified to have multiple products inside one message.  
The structure of the generated message stay as the original one and is compliant with the "message-schema.xsd".   

Compile the java project by issuing the following command in the putterapp directory:  
```
mvn clean install
```
The java build will be provided as jar files in the target directory.  

Openshift provides a convenient method for deploying binary Java applications. Other than deploying application’s source code, it can also deploy Jar file directly. To build the Java putter app using the following command:  


```shell
oc new-build --name=java-mq-demo --binary=true --image-stream=openshift/java:openjdk-17-ubi8 --strategy=source
oc start-build java-mq-demo --from-file=./target/mq-demo-app-0.0.1-jar-with-dependencies.jar --follow
```
The deployment of the application it self from the image will be done using a k8s deployment object. We have to provide some configuration properties through the deployment.  
Adapt the [deployment object](assets/putterapp/ocp-deploy.yaml) using the following input:

Depending of the namespace where your image has been deployed you might have to change the image location:
```yaml
spec:
    spec:
      containers:
        - name: putter
          image: 'image-registry.openshift-image-registry.svc:5000/<namespace>/java-mq-demo:latest'
```
The environment variable are used to provide amongst others the required information to reach MQ.  
- MQ_HOST: is the host where the MQ channel is listening to. It can be provided by the service: "<qmgr-service>.<namespace>.svc.cluster.local". The qmgr service is automatically created when deploying the queue manager. The one used in the asset is "mqm-demo-ibm-mq". You might need to adapt the namespace.
- MAX_PRODUCTS: this is the maximum of products that can be put in one message. The value is generated randomly.

Issue the command:
```shell
oc apply -f ocp-deploy.yaml
``` 

The value of the replica and the interval time can be changed on the deployment to increase or reduce the number of messages sent to MQ.  

Check the deployment of the application with
```
oc get deploy | grep putter
oc get po | grep putter
oc logs <puutter-pod>
```

Check that messages arrives on your MQ QueueManager.
The message should have the following layout:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<ordermessage>
<id>4c18bde6-c85d-4e7c-be9a-4ae707fec4f4</id>
<customer><id>b66a2c5b-c4a9-43b7-b895-1a50fa878ceb</id><name><firstname>Ruthanne</firstname></name><name><lastname>Crooks</lastname></name><phone number="07582 096060" type="mobile"/></customer>
<creditcard><number>6007-2215-7465-1941</number><expiry>04/26</expiry></creditcard>
<products><id>a0747868-b05f-44f1-8d48-1c8400bab04d</id><description>Fantastic Iron Pants</description><cost>33.31</cost><quantity>1</quantity></products>
<ordertime>2024-12-16 14:47:16.438</ordertime></ordermessage>
```

If you want to stop the application putting messages, the replica in the deployment can be changed to "0". Once adapted, run the command "oc apply -f ocp-deploy.yaml" to apply the changes. 

