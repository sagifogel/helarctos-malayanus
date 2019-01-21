Helarctos Malayanus (A.K.A Sun Bear) is a tool for processing a CSV file containing space reservation data, in a streaming fashion.
 
Each line represents a reservation of a unique space.<br/>
There are four columns in each line: Capacity, Monthly Price, Start Day, and End Day.<br/> 
The fourth column "End Day" could be empty, meaning the space is indefinitely reserved starting from the Start Day.<br/>

The output of the report is:

1. What is the revenue for the month? Revenue is calculated according to the monthly price of the reserved space.
   If a space is partially reserved for a given month, the revenue should be prorated based on the monthly price.

2. What is the total capacity of the unreserved spaces for the month?
   Space is considered reserved if it was reserved even for a single day for the given month.

### The following are prerequisites to running the application:

1. Scala 2.12
2. Java 8
3. sbt

### How to run the application:

- Via sbt: sbt run [path to data generator]
- Via IDEA: 
    - Load the project
    - Refresh sbt dependencies
    - Go to "Edit Configuration"
    - Select the `+` sign to add a new app (or just CTRL + SHIFT + F9 on the `StreamRunner` file):
        - Main class: `helarctosmalayanus.StreamRunner`
        - Use classpath on module: `helarctos-malayanus`
        - JRE: should be 1.8 (Java 8)
