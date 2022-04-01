package org.camunda.demo;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.ZeebeClientLifecycle;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


/**
 * This is basically a small Java Backend Application (SpringBoot)
 * wich can interact with Camunda Zeebe Engine (Camunda Cloud)
 * and can be called via REST API
 */
@SpringBootApplication
@EnableZeebeClient
@RestController
public class Application {

    @Autowired
    ZeebeClientLifecycle client;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * This is a REST POST request mapping, that allows for file uploads (assumes .csv)
     * @param file
     * @return
     */
    @PostMapping(path = "/uploadcsv")
    public String uploadCSV(@RequestParam MultipartFile file ) {
        /**
         * prepare Map to programmatically access data (lookup entry for id)
         */
        Map<String,String> readPersons = new HashMap<>();
        /**
         * reads uploaded file into Buffer for further processing
         */
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            /**
             * iterates over file line by line from top to bottom
             */
            String line;
            boolean skipFirstLine = true;
            while ((line = br.readLine()) != null) {
                /**
                 * this part depends on how the CSV is structured, this example assumes the included persons.csv
                 * wich has exactly 3 columns e.g. 1;Tester;Bob
                 * splitting this String will result in an StringArray of exactly 3 entries ["1", "Tester", "Bob"]
                 */
                String[] lineColumns = line.split(";");
                if(lineColumns.length == 3 && !skipFirstLine) {
                    /**
                     * Maps allow for easier lookup, to look up via id its convenient to place id first
                     */
                    readPersons.put(lineColumns[0], lineColumns[1] + ", " + lineColumns[2]);
                }
                skipFirstLine = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * no we can look for a specific id if everything worked out
         * lets get person with id=2
         */
        String response = "received file";
        if(readPersons.containsKey("2")) {
            response += " and person with id=2 = " + readPersons.get("2");


            /**
             * Camunda Bonus part, lets start a process with this information
             */
            Map<String,Map<String,String>> processData = new HashMap<>();
            processData.put("readPersons", readPersons);
            try {
                long processInstanceId = client.newCreateInstanceCommand()
                        .bpmnProcessId("checkCSV")
                        .latestVersion()
                        .variables(processData)
                        .send()
                        .join()
                        .getProcessInstanceKey();
                response += " and processInstance started with id: " + processInstanceId;
            } catch(Exception e) {
                // fail silent
            }
        }
        return response;
    }

    @ZeebeWorker(type = "convertCSV", autoComplete = true)
    public void handleJob(final JobClient client, final ActivatedJob job) {
        Map<String,Object> jobVariables = job.getVariablesAsMap();
        System.out.println("job variable content: " + jobVariables.toString());
    }
}
