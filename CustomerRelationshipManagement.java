/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// camel-k: language=java

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.camel.*;
import java.util.*;




public class CustomerRelationshipManagement extends RouteBuilder {
    private String consumerMaxPollRecords = "50000";
    private String consumerCount = "3";
    private String consumerSeekTo = "beginning";
    private String consumerGroup = "customerrelationship";
    private String kafkaBootstrap = "my-cluster-kafka-brokers.dm-kafka.svc.cluster.local:9092";

    String KIE_SERVER_URL = "http://insecure-rhpam7-kieserver-rhpam-user1.apps.cluster-a246.a246.example.opentlc.com/";


    @Override
    public void configure() throws Exception {

        tearDown();

        ProcessEvents processEvents = new ProcessEvents();
        CustomerEventAggregator eventAgg = new CustomerEventAggregator();
        InsightsAggregator eventAgg1 = new InsightsAggregator();

        from("kafka:" + "transaction-history" + "?brokers=" + kafkaBootstrap + "&maxPollRecords="
                + consumerMaxPollRecords + "&seekTo=" + "beginning"
                + "&groupId=" + consumerGroup)
                .process(eventAgg)
                .to("direct:txn");

        from("kafka:" + "customer-behavior" + "?brokers=" + kafkaBootstrap + "&maxPollRecords="
                + consumerMaxPollRecords + "&seekTo=" + "beginning"
                + "&groupId=" + consumerGroup)
                .process(eventAgg1)
                .to("direct:txn");




        ProcessResult parseResponse = new ProcessResult();

        from("direct:txn")
                .aggregate(body().tokenize(), processEvents).completionInterval(500)
                .log("${body}")
                .choice()
                .when(simple("${body} == 'Duplicate Processing'")).log("Handle duplicates")
                    .otherwise()
                    .process(new InvokeRule())
                    .log("${body}")
                    .removeHeaders("*")
                    .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                    .setHeader("Authorization", simple("Basic cGFtQWRtaW46cmVkaGF0cGFtMSE="))
                    .setHeader("Content-Type", simple("application/json"))
                .setHeader("Accept", simple("application/json"))
                .to("http://rhpam7-kieserver-rhpam-user1.apps.cluster-efcf.efcf.sandbox1751.opentlc.com/services/rest/server/containers/delinquency-check_1.0.0-SNAPSHOT/dmn?throwExceptionOnFailure=false")
                .bean(parseResponse,"parseResponse")
                .filter(simple("${body} == true"))
                .bean(parseResponse,"headerParse")
                .to("kafka:"+"delinquent-customer-case"+ "?brokers=" + kafkaBootstrap);


        ;


    }

    static Map<String, CheckDelinquencyDecision> entryMap = new HashMap<>();

    public void tearDown() {
        if (entryMap.size() > 100) {
            entryMap = new HashMap();
        }
    }

    private class ProcessEvents implements AggregationStrategy {


        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {


            CheckDelinquencyDecision checkDelinquencyDecision = entryMap.get(newExchange.getIn().getHeader("kafka.KEY").toString());
            try {
                if (null != newExchange && newExchange.getProperty("valueMap") != null) {
                    if (checkDelinquencyDecision == null) {
                        checkDelinquencyDecision = new CheckDelinquencyDecision();
                    }
                    Map startMap = (Map) newExchange.getProperty("valueMap");
                    Map relationshipInstanceRecord = (Map) startMap.get("relationshipInstanceRecord");
                    Map customerRelationshipEventRecord = (Map) relationshipInstanceRecord.get("customerRelationshipEventRecord");
                    Map cardTransaction = (Map) customerRelationshipEventRecord.get("cardTransaction");
                    checkDelinquencyDecision.setTransactionAmount(cardTransaction.get("cardTransactionAmount").toString());
                    newExchange.getIn().setHeader("customerReference",newExchange.getIn().getHeader("sdreferenceid"));
                    newExchange.getIn().setHeader("cardTransactionAmount",cardTransaction.get("cardTransactionAmount").toString());

                }

                if (null != newExchange && newExchange.getProperty("insightsMap") != null) {
                    if (checkDelinquencyDecision == null) {
                        checkDelinquencyDecision = new CheckDelinquencyDecision();
                    }
                    Map startMap = (Map) newExchange.getProperty("insightsMap");
                    Map customerBehaviorAnalysisInstanceRecord = (Map) startMap.get("customerBehaviorAnalysisInstanceRecord");
                    Map customerBehaviorAnalysisInsightsRecord = (Map) customerBehaviorAnalysisInstanceRecord.get("customerBehaviorAnalysisInsightsRecord");
                    checkDelinquencyDecision.setPredictionIndex(customerBehaviorAnalysisInsightsRecord.get("customerInsight").toString());
                    newExchange.getIn().setHeader("predictionIndex",customerBehaviorAnalysisInsightsRecord.get("customerInsight").toString());
                    newExchange.getIn().setHeader("customerClass","SILVER");


                }
                checkDelinquencyDecision.setCustomerClass("SILVER");
                checkDelinquencyDecision.setCustomerReferenceNumber(newExchange.getIn().getHeader("kafka.KEY").toString());
                if (null != newExchange) {
                    entryMap.put(newExchange.getIn().getHeader("kafka.KEY").toString(), checkDelinquencyDecision);
                }

                if (checkDelinquencyDecision.getPredictionIndex() != null && checkDelinquencyDecision.getTransactionAmount() != null) {
                    newExchange.getIn().setBody(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(checkDelinquencyDecision));
                    return newExchange;
                } else {
                    newExchange.getIn().setBody("Duplicate Processing");
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            return newExchange;
        }
    }

    private final class CheckDelinquencyDecision {
        private String transactionAmount;
        private String predictionIndex;
        private String customerClass;
        private String customerReferenceNumber;

        public String getCustomerReferenceNumber() {
            return customerReferenceNumber;
        }

        public void setCustomerReferenceNumber(String customerReferenceNumber) {
            this.customerReferenceNumber = customerReferenceNumber;
        }


        public String getTransactionAmount() {
            return transactionAmount;
        }

        public void setTransactionAmount(String transactionAmount) {
            this.transactionAmount = transactionAmount;
        }

        public String getPredictionIndex() {
            return predictionIndex;
        }

        public void setPredictionIndex(String predictionIndex) {
            this.predictionIndex = predictionIndex;
        }

        public String getCustomerClass() {
            return customerClass;
        }

        public void setCustomerClass(String customerClass) {
            this.customerClass = customerClass;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "CheckDelinquencyDecision{" +
                    "transactionAmount='" + transactionAmount + '\'' +
                    ", predictionIndex='" + predictionIndex + '\'' +
                    ", customerClass='" + customerClass + '\'' +
                    ", customerReferenceNumber='" + customerReferenceNumber + '\'' +
                    '}';
        }
    }

    private final class CustomerEventAggregator implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {

            java.util.Map valueMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(exchange.getIn().getBody().toString(), java.util.HashMap.class);
            exchange.setProperty("valueMap", valueMap);


        }
    }

    private final class InsightsAggregator implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {

            java.util.Map valueMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(exchange.getIn().getBody().toString(), java.util.HashMap.class);
            exchange.setProperty("insightsMap", valueMap);


        }
    }

    private final class InvokeRule implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {

            HashMap checkDelinquencyDecision = new com.fasterxml.jackson.databind.ObjectMapper().readValue(exchange.getIn().getBody().toString(), HashMap.class);

            String requestStart = "{\n" +
                    "  \"model-namespace\" : \"https://kiegroup.org/dmn/_5124A533-4437-4556-8495-34A6AD7B75A9\",\n" +
                    "  \"model-name\" : \"delinquencymanagementdmn\",\n" +
                    "  \"decision-name\" : [ ],\n" +
                    "  \"decision-id\" : [ ],\n" +
                    "  \"dmn-context\" : {\"Customer Class\" : \"";
            String requestEnd = "}\n" +
                    "}";

            exchange.getIn().setBody(requestStart + checkDelinquencyDecision.get("customerClass") + "\",\"Prediction Index\":\"" + checkDelinquencyDecision.get("predictionIndex") +
                    "\",\"Percentage available balance\":" + checkDelinquencyDecision.get("transactionAmount") + requestEnd);
            exchange.setProperty("delinquency",checkDelinquencyDecision);


        }
    }

//        public boolean delinquencyCheckParser(String body) throws Exception{
//            System.out.println(body);
//            try{
//                java.util.HashMap response = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body,java.util.HashMap.class);
//                System.out.println(response);
//                Map resultString = (Map)response.get("result");
//                System.out.println(resultString);
//
//                Map decisionResult = (Map)response.get("decision-results");
//                System.out.println(decisionResult);
//                return false;
//            }
//            catch(Exception e) {
//                e.printStackTrace();
//
//            }
//            return false;
//        }


    private final class ProcessResult {


        public boolean parseResponse(String body)  {
            try {

//                System.out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body));
                java.util.HashMap response = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, java.util.HashMap.class);
                System.out.println("inside response"+response);
                Map resultString = (Map) response.get("result");
//                System.out.println(resultString);

                Map decisionResult = (Map) resultString.get("dmn-evaluation-result");
//                System.out.println(decisionResult);

                Map decisionRes = (Map) decisionResult.get("decision-results");
//                System.out.println(decisionRes);

                Map res = (Map) decisionRes.get("_D47E2F64-50CE-4402-B832-EE7E21FD8715");
//                System.out.println(res);

                Boolean resString = (Boolean) res.get("result");
//                System.out.println(resString);
                return resString;


            }catch(Exception e) {
                return true;

            }


        }
        public String headerParse(Exchange exchange) throws Exception{
            System.out.println("Header print"+exchange.getProperty("delinquency"));

            Map checkDelinquencyDecision = (Map)exchange.getProperty("delinquency");
            String caseBody = "{\"case-data\":{ \"customerReference\":\""+checkDelinquencyDecision.get("customerReferenceNumber")+"\",\"defaultSegmentation\":\""+
                    checkDelinquencyDecision.get("predictionIndex")+"\",\"transactionAmount\":\""+checkDelinquencyDecision.get("transactionAmount")+"\",\"customerClass\":\""+
                    checkDelinquencyDecision.get("customerClass")+"\"}}";
            return caseBody;
        }

    }


}







