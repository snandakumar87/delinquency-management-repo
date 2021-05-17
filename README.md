# Delinquency Management

Setup:
======

1) oc new-project dm-kafka
   Install the AMQ Streams operator and deploy a kafka instance.
2) oc new-project dm-rhpam
   Install Business Automation Operator and deploy a KIE instance.
   2.1) Once deployment completes, import https://github.com/snandakumar87/delinquency-mgmt.git to Business central. Build and Deploy.
   2.2) import https://github.com/snandakumar87/dm-case to Business Central. Build and Deploy. (After the services are deployed, the url for the delinquency management and account recovery sd need to be updated on the case)
3) oc new-project dm-reporting
   Install Elastic operator and deploy a elastic cluster and Kibana instance.
   3.1) once deployed, import dm.ndjson on the Saved Object to view the Dashboard.
4) Make sure the adjust the PAM URL in the CustomerCase.java, CustomerRelationshipManagement.java 
5) sh provision.sh

Testing:
=======

POST <sd-credit-charge-card-url>/sd-credit-chrg-card/credit-charge-card/credit-charge-card/SDREF24334/credit-charge-card-fulfilment-arrangement/CR24334/cardtransaction/BQREF24334/update
   
{
  "creditChargeCardFulfilmentArrangementInstanceReference": "CCCFAIR786952",
  "cardTransactionInstanceReference": "CTIR783033",
  "cardTransactionInstanceRecord": {
    "cardTransaction": {
      "cardTransactionProductInstanceReference": "707676",
      "cardTransactionNetworkReference": "742083",
      "cardTransactionIssuingBankReference": "782147",
      "cardTransactionMerchantAcquiringBankReference": "764711",
      "cardTransactionType": "CARDTRANSACTION",
      "cardTransactionCurrency": "USD",
      "cardTransactionAmountType": "string",
      "cardTransactionAmount": "250",
      "cardTransactionMerchantReference": "761100",
      "cardTransactionLocationReference": "761538",
      "cardTransactionProductServiceReference": "764958",
      "cardTransactionDateTime": "09-22-2018",
      "cardTransactionFXConversionCharge": "250",
      "cardTransactionInterchargeFee": "250",
      "cardTransactionAuthorizationRecord": {}
    }
  },
  "cardTransactionUpdateActionTaskRecord": {},
  "cardTransactionUpdateActionRequest": "string"
}   

   
   
   
