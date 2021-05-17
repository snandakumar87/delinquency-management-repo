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


public class CustomerEventHistory extends RouteBuilder {
    private String consumerMaxPollRecords = "50000";
    private String consumerCount = "3";
    private String consumerSeekTo = "beginning";
    private String consumerGroup = "customereventhistory";
    private  String kafkaBootstrap = "my-cluster-kafka-brokers.dm-kafka.svc.cluster.local:9092";

    @Override
    public void configure() throws Exception {

        GetCardTransactions getCardTransactions = new GetCardTransactions();
        from("kafka:" + "credit-charge-card" + "?brokers=" + kafkaBootstrap + "&maxPollRecords="
                + consumerMaxPollRecords + "&seekTo=" + "beginning"
                + "&groupId=" + consumerGroup)
                .process(getCardTransactions)
                .log("${header.kafka.KEY}")
                .to("kafka:"+"transaction-history"+ "?brokers=" + kafkaBootstrap);

    }


    private final class GetCardTransactions implements Processor {

        String responseBodyStart = "{\n" +
                "  \"customerEventLogInstanceReference\": \"CELIR795204\",\n" +
                "  \"relationshipInstanceReference\": \"RIR779135\",\n" +
                "  \"relationshipInstanceRecord\": {\n" +
                "    \"customerRelationshipEventType\": \"CARDTRANSACTION\",\n" +
                "    \"customerRelationshipEventRecord\": ";
        String responseBodyConnector = ",\n" +
                "    \"customerRelationshipEventAction\": \"UPDATE\",\n" +
                "    \"dateTimeLocation\":\"";

        @Override
        public void process(Exchange exchange) throws Exception {

                java.util.Map valueMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(exchange.getIn().getBody().toString(), java.util.HashMap.class);

                java.util.Map creditInstanceMap = (java.util.HashMap) valueMap.get("cardTransactionInstanceRecord");

                java.util.Map creditCardTransaction = (java.util.Map) creditInstanceMap.get("cardTransaction");

                if(creditCardTransaction.get("cardTransactionType").equals("CARDTRANSACTION")) {


                    String response = responseBodyStart + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(creditInstanceMap) + responseBodyConnector + creditCardTransaction.get("cardTransactionDateTime").toString() + "\"}}";
                    System.out.println(response);
                    exchange.getIn().setBody(response);
                }


        }
    }
}
