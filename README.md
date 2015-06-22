# mailserver
POP3 Mailserver written in Java

The following POP3 commands are supported: `USER, PASS, QUIT, STAT, LIST, RETR, DELE, NOOP, RSET, TOP` and `UIDL`. For more information see [POP3 (RFC 1939)](https://www.ietf.org/rfc/rfc1939.txt).

## Setup
* Run mkdl.sql to set up the database.
* Create a class Login that contains three public static String fields:
  * `HOST`      the URL the database is located in the form `jdbc:mysql://<url>`
  * `USERNAME`  the MySQL username
  * `PASSWORD`  the MySQL password

## Run
* The port and timeout can be specified on the command line: `Pop3Server [PORT] [TIMEOUT]`
* The default port is 110 and timeout is 60,000 ms or 10 minutes.
