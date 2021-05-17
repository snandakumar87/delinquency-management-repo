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



public class CustomerCase extends RouteBuilder {
    private String consumerMaxPollRecords = "50000";
    private String consumerCount = "3";
    private String consumerSeekTo = "beginning";
    private String consumerGroup = "customercase";
    private String kafkaBootstrap = "my-cluster-kafka-brokers.dm-kafka.svc.cluster.local:9092";

    String KIE_SERVER_URL = "http://insecure-rhpam7-kieserver-rhpam-user1.apps.cluster-efcf.efcf.sandbox1751.opentlc.com/";


    @Override
    public void configure() throws Exception {


        from("kafka:" + "delinquent-customer-case" + "?brokers=" + kafkaBootstrap + "&maxPollRecords="
                + consumerMaxPollRecords + "&seekTo=" + "beginning"
                + "&groupId=" + consumerGroup)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Authorization", simple("Basic cGFtQWRtaW46cmVkaGF0cGFtMSE="))
                .setHeader("Content-Type", simple("application/json"))
                .setHeader("Accept", simple("application/json"))
                .to("http://insecure-rhpam7-kieserver-rhpam-user1.apps.cluster-efcf.efcf.sandbox1751.opentlc.com/services/rest/server/containers/delinquency-case_1.0.0-SNAPSHOT/cases/delinquency-case.delinquency-process/instances")
        ;



    }




}







