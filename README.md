# Delinquency Management

Setup:
======

1) oc new-project dm-kafka
   Install the AMQ Streams operator and deploy a kafka instance.
2) oc new-project dm-rhpam
   Install Business Automation Operator and deploy a KIE instance.
   2.1) Once deployment completes, import https://github.com/snandakumar87/delinquency-mgmt.git to Business central. Build and Deploy.
3) oc new-project dm-reporting
   Install Elastic operator and deploy a elastic cluster and Kibana instance.
4) Make sure the adjust the PAM URL in the CustomerCase.java, CustomerRelationshipManagement.java 
5) sh provision.sh

   
   
   
