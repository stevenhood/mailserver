# mailserver
POP3 Mailserver in Java

[POP3 (RFC 1939)](https://www.ietf.org/rfc/rfc1939.txt)

## Setup
* Run mkdl.sql to set up the database.
* Create a class Login that contains three public static String fields:
  * HOST      the URL the database is located
  * USERNAME  the MySQL username
  * PASSWORD  the MySQL password

## Run
* The port and timeout can be specified on the command line: Pop3Server <port> <timeout>
* The default port is 110 and timeout is 60,000 ms or 10 minutes.
