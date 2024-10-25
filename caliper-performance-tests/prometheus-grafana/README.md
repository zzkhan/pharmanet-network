# Prometheus/Grafana

Caliper is able to send information to a Prometheus server, via a Push Gateway. The server is available for direct query via http queries or viewing with Grafana.

Caliper clients send information under the following labels:
 - caliper_tps
 - caliper_latency
 - caliper_send_rate
 - caliper_txn_submitted
 - caliper_txn_success
 - caliper_txn_failure
 - caliper_txn_pending

Each of the above items are tagged with the client number, test name, and test round

### Docker Compose file

- **docker-compose-fabric.yml** - Used to spin up a prometheus/grafana system that will scrape from the Prometheus Push Gateway and exported fabric metrics on:
  - orderer.example.com:9443
  - peer0.org1.example.com:9444
  - peer0.org2.example.com:9445
  - peer0.org3.example.com:9446
  - peer0.org4.example.com:9447
  - peer0.org5.example.com:9448
     
     
     


  

