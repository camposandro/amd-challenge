### The Asset Management Digital Challenge

There are some key aspects to consider for deployment of this application.

The in-memory database, for example, would not scale well in a real-life scenario. We should replace
it with an external data source, like a relational database (e.g. MySQL). On a larger scale, and
with an increasing number of functionalities, the existence of a schema would also avoid data
replication and decrease the hosting costs.

A CI/CD workflow would automate the deployment to different environments, which could be used for
developing, testing and hosting the production app. These environments could be easily created from
Docker containers with all the dependencies required for the app to run.

There should also be monitoring tools to trace transactions, log errors and track security
vulnerabilities. Integration with solutions like Grafana, for example, would even allow for
graphical visualization of some of these analytics.

TDD and BDD are good practices advocating the writing of tests before the writing of the actual
code. Their usage is highly recommended as they enable an increase of code coverage and, as a
consequence, reduce bugs.