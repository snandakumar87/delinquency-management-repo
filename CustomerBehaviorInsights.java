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


public class CustomerBehaviorInsights extends RouteBuilder {
    private String consumerMaxPollRecords = "50000";
    private String consumerCount = "3";
    private String consumerSeekTo = "beginning";
    private String consumerGroup = "customerinsights";
    private  String kafkaBootstrap = "my-cluster-kafka-brokers.dm-kafka.svc.cluster.local:9092";

    @Override
    public void configure() throws Exception {

        ProcessEvents processEvents = new ProcessEvents();
        from("kafka:" + "transaction-history" + "?brokers=" + kafkaBootstrap + "&maxPollRecords="
                + consumerMaxPollRecords + "&seekTo=" + "beginning"
                + "&groupId=" + consumerGroup)
                .process(processEvents)
                .log("${header.kafka.KEY}")
                .to("kafka:"+"customer-behavior"+ "?brokers=" + kafkaBootstrap);

    }


    private final class ProcessEvents implements Processor {

        String responseBodyStart = "{\n" +
                "  \"customerBehaviorAnalysisInstanceReference\": \"CBAIR703629\",\n" +
                "  \"customerBehaviorAnalysisInstanceRecord\": {\n" +
                "    \"customerInsightAnalysisSchedule\": \"realtime\",\n" +
                "    \"customerBehaviorAnalysisInsightsRecord\": {\n" +
                "      \"customerInsightType\": \"delinquencyindex\",\n" +
                "      \"customerInsight\":";
        String responseBodyConnector = ",\n" +
                "      \"customerInsightCalculationDate\":";
        String getResponseBodyEnd = "}\n" +
                "  }\n" +
                "}";


        @Override
        public void process(Exchange exchange) throws Exception {

            Format f = new SimpleDateFormat("MM/dd/yy");
            String strDate = f.format(new Date());
            System.out.println("Current Date = "+strDate);

            String responseString = responseBodyStart+"\"LOW\""+responseBodyConnector+"\""+strDate+"\""+getResponseBodyEnd;
            System.out.println(responseString);
            exchange.getIn().setBody(responseString);

        }
    }


}
