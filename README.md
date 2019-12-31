# Word-Quizzle (WQ)
Network course final project at Unipi a.a. 2019/20

## Summary
The project is a system of italian-english translation challenges between users registered to the service.
Users can challenge their friends in a "translation race": the goal is to translate in a given amount of time the greater number of words from a given set of italian ones randomly chosen by the service.
The system offers also a social network facility to its users.
The application is implemented according to a client-server architecture.

# Implementation guidelines
 
## 1. Operations 
The server disposes this set of methods:
* register(nickname, password): requests registration for a new user. The server sends back a successful response code if the nickname is not yet assigned to any other user registered to the system and the password is not empty, an error code describing the problem otherwise. 
* login(nickname, password): registered user login to the service. The server answers with a successful response code when done correctly or with an error message if the user is already logged in or if a wrong password is provided.
* logout(nickname);
* addFriend(nickUser1, nickUser2): adds a friend to both user1 and user2 friend lists. The server sends back a successful response code when the operation is done or an error code if one of the nicknames doesn't exist or if the users are already friends.
 Optionally the server can implement a confirmation feature: user2 receives the request and can decide either to accept or decline the friendship.
* listFriends(nickname): ​lists all friends of a given user. The server response is a JSON representing the list of friends. 
* challenge(nickUser, nickFriend): nickUser wants to challenge nickFriend. Before starting the challenge, the server checks whether nickFriend is a friend of nickUser, if not sends back an error code and stops the operations. To start a challenge the server sends to nickFriend a challenge-acceptance request. This request has a TTL and it is automatically considered rejected if not answered in time. The challenge starts when userFriend accepts it.
    1. The service randomly chooses K words from a dictionary of N italian ones (N >= K) which will delivered one by one to the participants during the challenge. The two have a timeout of T to answer to all questions. Every time a player gives a translation for a word, either correct or not, the server sends the next one to that player.
    2. The game ends when either the two players answers the whole set of questions or the timeout is over.
    3. The server uses a **[third-party service](#2-Protocols-and-specifications)** to get the translations for a challenge. Every player gains X challenge points for each correct given translation and lose Y points for each wrong one; every unsolved question due to the timeout adds 0 points.
    4. The player with the highest score wins the challenge and gets Z extra points.
    (The values K, N, TTL, T, X, Y, Z are up to the student's will).
* getUserScore(nickUser): the server provides the **user score**. It is the sum of all the challenge points gained by the user in challenges.
* showRanking(nickUser): returns the ranking list of nickUser and his/her friends as JSON. This ranking list is calculated according to the user score of every user considered in the list.
## 2. Protocols and specifications
* The registration phase uses RMI.
* The login phase is the first phase to be performed after a TCP connection is established with the server. Over this TCP connection client and server will continue interact (requests/responses) after a successful login.
* Challenge requests are forwarded using UDP protocol from the server to player2.
* The server should be either multithreaded or implementing NIO multiplexing. 
* The server has a list of N italian words stored in a file. During the setup of a challenge it selects K randomly words from the list. Before starting the challenge but only after palyer2 accepts it, the server gets the translations of the words selected via an HTTP GET call to the third-party service **https://mymemory.translated.net/doc/spec.php​**. Translations are stored for the whole time of the challenge to verify player answers rightness.
* The end user interacts with WQ by means of a GUI or a simple command line interface.
* Registrations, friendship relations and scores for every user are stored in JSON files by the server.
## 3. Project execution and delivery method
The delivered material must include:
* the application code and optionally the tests suits;
* the report in pdf format containing:
    * a general motivated architectural description of the system;
    * a general schema of the threads activated by any component and of the data structures correlated with a description of the concurrence control strategies;
    * a brief description of the classes and precise indications about the execution; 
    * the instructions about compiling and executing the project (external libraries, arguments to the program, syntax of the commands of the operations...). This sections should be a simple user manual of the system.