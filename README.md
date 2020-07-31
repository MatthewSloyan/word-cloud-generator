## A Web Opinion Visualiser – Matthew Sloyan G00348036 

Overall, I wanted this project to be a learning experience, so I tried multiple search algorithms, 
heuristics, goal conditions and scoring options each with their own benefits. 
Throughout the process I made various decisions from brainstorming, writing it out on paper and trial 
and error to get the most accurate result efficiently. I also used the lecture notes and labs provided 
for guidance regarding design principles and patterns, loose-coupling, high cohesion, abstraction, 
encapsulation, composition, heuristics, and search algorithms. Some of the design decisions and a 
guide for each feature can be found below under their relevant headings. 

### Tested On
* 2 Window 10 PC's
* Linux VM
* Coded using Java 8

### How to run and deploy.
* Install a Tomcat 9 server and extract the zipped file.
* Place the wcloud.war file in the webapps folder.
* Once deployed run “startup.bat”, this will open a console and deploy the .war file.
* The application now will be available here http://localhost:8080/wcloud/
* On the process page, there is multiple options for the user to select which is described below.
	1. Search algorithms used - BFS, DFS, BS
	2. AI heuristic used - Fuzzy, Encog
	3. Use of frequency or Levenshtein distance in scoring.
	4. Goal condition type - Max nodes (80 Nodes) or Max words (Max Words works best with Fuzzy).
	5. Number of words to display in wordcloud – 10, 20, 30
* Enter the term(s) to search for and click submit. Once completed a word cloud will be displayed 
  of the top N number of words. Speed of search can greatly depend on internet speed 
  (More information on processing speed in Speed tests section).


## == HEURISTIC SEARCH ==
I wanted to learn more about different search algorithms, so I researched the different options and 
decided to implement a Best First Search, Recursive Depth First Search, and a Beam Search. 
All searches are threaded using a thread pool. More information about each search can be found below.

#### Best First Search
I started initially with the BFS which worked very well and is very fast and accurate, 
at an average of 4 seconds to search. The initial code for this was supplied by our lecturer 
but I heavily modified and abstracted it. It works by initially adding the first link to a 
queue if the score is high enough (>= 1). This first link is one of the results returned from 
the DuckDuckGo search. If the initial webpage does not score high or medium it is cut out, so 
that time is only spent searching relevant nodes. A process is then run which loops until the queue 
is empty, searching each node, scoring them, and adding the words to the Database if the score is 
high enough. I wanted to uphold the SRP with all the searches, so all database indexing and scoring 
are all handled in separate classes. I have used PriorityBlockingQueue with a comparator 
based on the score. This instance is used across all threads in the thread pool as it is 
passed in from the NodeParser class. So that any running instance of this search is always 
taking the highest scored node from queue the front of the queue. 

#### Recursive Depth First Search
This search works by recursively searching children of each node until the score is low or 
goal conditions are met. If the score is low, it moves back to the parent and goes down another route. 
This algorithm is the fastest and it very accurate, however accuracy is not always insured as it could 
go down unrelated paths. Unlike other searches it does not require any queues to pull nodes 
from, making it very fast. 

#### Beam Search 
It works by adding a highest scoring webpages (node) to the queue limited by a beam width specified 
in the Searchable interface. It is like the best first search, however each child is searched from 
each node and only the best three are taken and added to the queue. Whereas the BFS adds all suitable 
nodes to the queue. A LinkedList is used to manage the queue, which is sorted using a comparator 
after each iteration. When a node is searched all its children are added to an ArrayList, this is 
then sorted, and the best three children are taken and added to the queue. 
From testing it’s faster than a BFS and more accurate than the RDFS. 

#### Multiple Search Terms
I have implemented the ability to handle multiple search terms efficiently. I initially had it where 
each search term was looped over and added to the queue and indexed. However, multiples of the same 
nodes could get added if the score is high enough. So, I developed a check if it is worth scoring. 
If so index and add to queue at the end. For the BFS and BS it only adds the node with the highest 
score, as if the last search term scored badly the node would be placed at the back of the queue 
and might not be searched.

#### Scoring
Another area I looked at heavily was scoring each node, as I knew this would make a difference to 
speed and accuracy. I decided to include as much data as possible for each webpage and decided on 
Meta tags, title, headings, and the body to score each page. Each would have their own varying weight 
that was tested heavily with my Fuzzy rules and Encog NN. For each of the four types a frequency 
result is calculated using its string. The frequency can use two methods, which are determined by 
the user options (Frequency of word, or Levenshtein distance.) This is one of the options 
determined by the user. If option 1 is selected, the score is based on the number of times the word 
occurs in the string (Faster). If option 2 is selected, the score is based on if the Levenshtein 
distance between a word and the search term. I added the Levenshtein distance as an extra for search 
terms that are not as common as it finds similar words.

Using the frequency calculated for each type, pass into either Fuzzy or Encog heuristic and return 
the predicted score as WebPage object. I decided to use a WebPage object to return to each webpage 
it was breaking the SRP indexing the data in the Scoring class. Scoring now just scores and returns 
data to the search to be passed to the db if score is high enough. I also decided to implement an 
additional feature if the score is medium (1) just to use the words close to the search term in the 
body, as if the result is medium it might not be the most relevant page. If the score is high 
however all words are indexed as it is very relevant. I also did a lot of filtering of the words to 
ensure the best results are added to the database. This includes checks for empty words, if the 
word is not equal to the search term, if word is in ignore words set, and if the word is longer than 2 characters. 


## == FUZZY LOGIC ==
What I developed in the Scoring class made it very easy to implement Fuzzy Logic. 
The FuzzyHeuristic class just scores a webpage and returns the score (SRP) and it implements the 
Heuristicable which is used in Fuzzy, Encog and the custom NN classes.

#### Membership functions
I decided to use different related terms for each function to distinguish between each of the 
four types (meta = useful, title = relevant, headings = frequent, body = significant). 
With this I created a set of membership functions tailored to each type. For example, 
as Meta tags have the highest weight with a score of 100. When testing there would usually
be 1 or none found, so I decided to use just 'not_useful' & 'useful' to weight it. 
If 1 tag is found it is above 40, so it is useful and if 0 is found then it is not_useful. 
This one or the other type worked well for metas and the title.

#### Defuzzification method
I looked at two different methods. First using Mamdani (Center of Gravity) which provided a 
very accurate result but with significant computational burden. I also looked at Sugeno (COGS) 
which uses single spikes (fuzzy singletons). It also provided a very accurate result and is 
computationally more efficient, so I decided to use this method for my output score. 
I did a lot of testing for both with all search algorithms which helped my decision.

#### Inference rules
I wanted to include all possible outcomes that I deemed a low, medium, or high score. 
So, I wrote out 7 rules. As title, and meta tags have a high weight I wanted to ensure they 
would score high if included. The same can be said for headings and the body but to less extent. 
A lot of testing went into these rules to ensure that webpages would be scored low if not relevant, 
and subsequently not added to the database or queue. More information about each rule 
can be found in the “heuristic.fcl” file.


## == AI EXTRAS ==
#### Encog
I wanted to learn more about neural networks, as it will be a focus of my career. 
Initially I investigated implementing an Encog Neural Network. Using the labs provided I developed, 
trained and tested a simple ResilientPropagation Neural Network. On average it trains with a 96% 
accuracy and does it within a few seconds. I tested multiple activation functions, number of hidden 
layers and different Propagation types and this setup was the fastest and most accurate. However, 
I felt my training data was not the best as there is some nodes getting scored low that should be scored higher.
It would be something I would develop further if I had more time.

#### Custom Backpropagation Neural Network
I then began to look at using a custom Neural Network. I initially used code supplied by our lecturer 
to train the NN, this can be found in the ie.gmit.sw.ai.nn package. Again, I tested some of the other 
activation functions and hidden layers to come to the final result. On average it trains with a 100% 
accuracy instantly, so it was faster and more accurate than the Encog implementation. To save the neural 
network I used serialization which converts the object to a byte stream. This object is then read in by 
the prediction method. If I had more time I would have made this object available to all instances without loading to speed it up.


## == OTHER CLASSES ==
### ServiceHandler
It manages all aspects of the application, from setting user options, adding all files making search requests. 
Each of these processes is delegated to further classes to uphold SRP. So, all that’s contained in the 
ServiceHandler is the methods required for the web app. In the init method the file paths are added to an 
Files object which is a Singleton, so they are accessible to each search instance easily. The doGet method 
gets the users options, creates a NodeParser instance which starts the search. When the results are 
returned the are displayed to the user in a word cloud.

### NodeParser
Parses all links from duck duck go search and runs chosen search algorithms using an ExecutorService. 
Also it gets results from database to pass back to ServiceHandler. Multiple search terms are handled 
and added to an ArrayList here.

### Results Database
This class is also composed in the ResultsDatabaseProxy, so that the client or other classes will not 
be able to access it directly. Databaseable is implemented which could be used with other types of databases. 
I initially decided there should only be one instance of this to provide easy access to multiple classes. 
However, from testing would only allow one request to work at one time otherwise the results would merge into one. 
This is not ideal for a multi-user web application, so I decided to use a new instance for each request and pass 
the same instance into the classes required. To keep the coupling loose I used to a proxy design pattern, and 
only composed the proxy where it is needed. I wanted to uphold the SRP with this class, so all database functionality 
such as indexing words, checking against the ignore words file and sorting the final map are handled here rather 
than other classes.

I have used a ConcurrentHashMap for the local db as it's the fastest type (search, insert and delete functions are O(1)). 
Also, it allows for concurrency between multiple threads as a Thread Pool is used to process links.
The database also contains methods such as sortMapByValue, getResults and index which are described below.

* sortMapByValue – Sort map by value in descending order and return as a list to get number of words to display. 
  The top N number of words are displayed in a word cloud to the user. A comparator is used to sort by value, 
  which compares value 1 and 2.
* getResults – Gets sorted map as a list and returns array of the top N words.
* index - Method to index all strings found in a webpage if it was found that it was relevant. 
  Meta, title, headings, and body strings are passed in and added to the hashmap word by word. 
  It can also handle any number of params using variable arguments, so if more types were to be added.

### Options
Class that holds user information selected. This is used across all classes to determine the options to pick. 
Like the Database, it had to be unique to each user so a Singleton could not be used. 
Options are described in initial “How to run” section.

### Files
Class that holds file paths for the application. It is used by Heuristic searches and for loading 
the ignore words file. I wanted to have one place to add all the file paths, rather than passing them through 
classes or hard coding them. So, the service handler gets the full path for the machine it is running on and 
adds them all to an array list. Index 0 = ignorewords.txt, index 1 = heuristic.fcl and index 2 = model.eg. 
Also, more files could easily be added without having to add more parameters to each class. Lastly, I have 
implemented a Singleton design pattern for this so it can be easily accessed by any class that requires it. 
As these paths are the same for anyone using the application it made sense to have one instance.

### IgnoreWords 
Loads all words from the ignorewords.txt file into a HashSet. This is used by the ResultsDatabase to compare word. 
I have used a HashSet for the words for speed (search, insert and delete functions are O(1)). 
SRP upheld as abstracts functionality from ResultsDatabase class.

### Interfaces – Databasable, Heuristable and Searchable.
Used to promote reusability and OCP, so that new databases, heuristics, or search algorithms can be added easily.


### == DESIGN PATTERNS & PRICIPLES ==
#### Singleton
I have implemented a Singleton pattern in the Files as there should only be one instance of it, 
this would allow multiple servlets to access the same file paths easily. I originally had the results 
database as a Singleton but after testing it was not ideal for a multi-user web application. 

#### Proxy
I have decided to implement a proxy to access the results database, as this allows the developer to 
control the database service without the client knowing as the proxy is implemented as a layer of encapsulation 
in front of it. The proxy also works even if database is not ready. The database could be changed without 
affecting any of the classes as the proxy is composed in several classes.

#### Template
The template pattern is used in the ServiceHandler callback method init().

#### Single Responsibility Principle (SRP)
At all levels throughout the design SRP is upheld as I tried to give every class a specific purpose that 
delegates to other classes when required.

#### Open Close Principle (OCP)
Seen in the proxy implemented for the results database. You can introduce new proxies without changing 
the real database or client. You can also make changes to the database or add new databases without affecting 
the proxy or the client. The same can be said for the search algorithms implemented using the Searchable interface. 
Also the heuristics (Fuzzy and Encog) use the same approach.

#### Dependency Inversion Principle
No higher-level module depends on a low-level module. Both interfaces implemented are by classes at the same 
level and below the interface.

#### Interface Segregation Principle
No class implements interface methods that it does not use. For example, Searchable implements checkGoal 
as a default method which is used by all searches.

#### Law of Demeter
No single function knows the whole navigation structure of the system. I have tried to subjugate all 
functionality into multiple methods. 


## == SPEED TESTS ==
Speed can be heavily determined by internet speed as it was tested on a lower speed than I have myself 
and it was considerably slower. Also, speed can depend on number of search terms and goal conditions. 
If max words are selected it can take longer for uncommon search terms as there might be less suitable nodes.
BFS – 4 seconds on average.
RDFS – 2.5 seconds on average.
BS – 3.5 seconds on average.


## == TESTS == 
From testing it was found that all search algorithms produce accurate results. However, 
if multiple search terms are used it can be slower. Below is a sample test of the top 10 words returned in order of frequency. 
Please note the encog and custom heuristics can take some time when max words goal codition is selected, as it can take a while to
meet max words.

If “Java” is searched using a BFS, Fuzzy COG, and max goal nodes.
Results: learn, oracle, html, sun, reference, xml, classes, programming, se, examples

If “Lance Armstrong” is searched using a RDFS, Fuzzy COG, and max goal nodes.
Results: tour, de, france, st, team, doping, cycling, sports, times, race


## == ADDITIONAL EXTRAS (All documented in full above) == 
* Ability to handle multiple search terms efficiently and accurately.
* Multiple user options to customise search (Search algorithm, goal conditions, heuristics, scoring and word cloud number).
* Multiple Search Algorithms implemented (BFS, RDFS and BS).
* Frequency, getting close words and Levenshtein distance added to scoring web nodes.
* Design patterns such as Singletons and a Proxy implemented to improve overall design and promote reuse.
* Commented to JavaDoc standard.
* AI extras such as Encog and Custom NN as described in “AI Extras” section above.
* Errors handled, such as no results found or if user has been locked out of DuckDuckGo.
* Thread pools and concurrency implemented.


