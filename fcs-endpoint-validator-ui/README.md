# Vaadin frontend application for FCS/SRU Endpoint Validator

It requires Java 17 or newer and node.js 10.16 or newer.

To run the project, run `mvn spring-boot:run` and open [http://localhost:8080](http://localhost:8080) in browser.

To update to the latest available Vaadin release, issue `mvn versions:update-properties`

Some useful links:

- [Feature overview](https://vaadin.com/flow)
- [Documentation](https://vaadin.com/docs/flow/Overview.html)
- [Tutorials](https://vaadin.com/tutorials?q=tag:Flow)
- [Component Java integrations and examples](https://vaadin.com/components)

## Build Dockerimage

```bash
# got back to root of multi-module fcs-endpoint-validator
cd ../

# (optionally) check if there are unexpected files picked up by docker
# > docker build -t docker-show-context https://github.com/pwaller/docker-show-context.git
# > docker run --rm -v $PWD:/data docker-show-context

docker build -f fcs-endpoint-validator-ui/Dockerfile .
```
