This is a simple approach to read a .csv file through a REST call and start a process with it

1. enter your CamundaCloud API client credentials in src/main/resources/application.yml
2. start up the Application via: mvn spring-boot:run -Dmaven.test.skip=true
3. open src/main/html/index.html
4. upload src/main/resources/persons.csv
5. check response
6. BONUS: deploy src/main/resources/checkCSV.bpmn on your cluster
7. Enjoy a started process instance with your .csv data available