### 3. Multi set configs
Let add a new feature to app. 
Currently we have one set config but now we want to add multiple set configs which will run in sequence. Once first config completes, it will automatically start the next config. 
Example - I want to first 2 sets to of 1 minute works and 1 minutes rest after that, i need 2 sets with 2 mins work and 2 min rest, after that i need 1 set of 1.5 mins work and 1.5 min rest. 

In UI we can add a + icon, on clicking on this icon it will ask for no of sets, work time and rest time for second config.




### 2. Inputs
* It will have 3 inputs
    * no of sets
    * work time
    * rest time
* Based on the input, it will start the sets then rest then another set until the number of set completes

### 1. Sounds
* When we are in Get ready state before the timer start first time, play the sound
* When the work is remaining 5 second start timer and play until work become 0
* When the rest is remaining 5 second start timer and play until rest become 0

