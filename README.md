# Project: Multi-Agent VRP optimiser with JADE and Java

This project implements a Vehicle Routing Problem (VRP) optimiser using two algorithms Greedy and Genetic Algorithm (GA)— and two execution modes: Local and Multi-Agent (JADE). The goal is to minimise total travel distance while maximising the number of delivered items under vehicle capacity and distance constraints.


# Features

- Greedy and Genetic Algorithms for VRP optimisation
- Local and Distributed (JADE-based) execution modes
- Real-time visualisation of delivery routes
- Modular agent communication via JADE ACL messages
- GUI for parameter setup, execution, and performance comparison


# System architecture

The system is built around three main agent types implemented in the JADE environment:

Agent type 1: Master Routing Agent (MRA) in file vrp.ManagerAgent and is the central coordinator running optimisation whether Greedy or GA 
Agent type 2: Delivery Agents (DAs) in file vrp.DeliveryAgent and represent vehicles executing assigned routes 
Agent type 3: GUI Agent (MasUI) in file  vrp.GuiAgent and provides the user interface and handles communication 

Agents interact through ACL messages with conversation IDs:
optimize-request → GUI → MRA (start optimisation)
route → MRA → DA (assign route)
log → DA → GUI (status update)
optimization-result → MRA → GUI (final results)


# Interface MasUI

The Java Swing-based interface is the file MasUI.java and allows configuring parameters, running experiments, and visualising routes

#  Parameters

- Number of Items — Number of delivery points generated or loaded
- Number of Delivery Agents (DAs) — Number of vehicles
- Capacity per DA — Maximum load each DA can handle
- Max Distance (dv) — Maximum travel distance per DA
- Seed — Random seed for reproducibility

# Optimisation buttons 

- Technique: Greedy or Genetic Algorithm (GA)
- Optimize (Local): Runs the selected algorithm locally inside the GUI
- Optimize (JADE): Sends a JSON payload to the ManagerAgent which coordinates optimisation through the JADE framework

# JADE buttons

- Launch MAS (JADE): Starts or connects to the JADE platform
- Restart JADE: Restarts the agent containers
- Shutdown JADE: Stops all running agents

# Output message sections
Displays logs and visual feedback:
- Coloured delivery routes for each DA
- Text log showing distances, deliveries, and execution times
- Real-time ACL message exchanges


# Installation and configuration

# Requirements
- Java 24 (or 17+)
- IntelliJ IDEA
- JADE 4.6.0
- Gson 2.10.1

# Step 1: Clone the Repository
Start by cloning the source code from GitHub:
git clone https://github.com/Sofiazugasti/devops-pipeline-SWE4006.git
Once cloned, open the project folder in IntelliJ IDEA and confirm that the IDE detects the Java SDK (Java 24 or 17)

# Step 2: Install JADE
Download JADE 4.6.0 from the official website:
https://jade.tilab.com/download/jade/license/jade-download/
Extract the downloaded file.
The JADE library is located in:
JADE-all-4.6.0/jade/lib/jade.jar
Keep this .jar file accessible for linking in IntelliJ.


# Step 3: Add JADE to IntelliJ
Go to File → Project Structure → Libraries
Click the icon → Java*
Select your local jade.jar file (e.g. JADE-bin-4.6.0/jade/lib/jade.jar)
Apply and close the window

Then verify the library appears under Modules → Dependencies as jade.jar (Compile)

# Step 4: Add Gson
 Add gson-2.10.1.jar (included in lib/ folder) the same way as JADE.


# Step 5: Run Configurations

MasUI (Local Mode)
Go to Run → Edit Configurations → + → Application.
Set:
Name: MasUI
Main class: vrp.MasUI
Use classpath of module: intelligent-systems
Program arguments: (leave empty)
Save and apply.
JADE Platform (Distributed Mode)
Go to Run → Edit Configurations → + → Application.
Set:
Name: JADE Platform
Main class: jade.Boot
Use classpath of module: intelligent-systems
Program arguments:
-gui -local-port 1200 -agents "MRA:vrp.ManagerAgent;DA1:vrp.DeliveryAgent;DA2:vrp.DeliveryAgent;Gui:vrp.GuiAgent"
Save and apply.


# Step 6: Run the Project
Run JADE Platform to start the agent environment.
You should see:
Agent MRA started
Agent DA1 started
Agent Gui started
Then run MasUI to open the GUI.
Configure parameters, select the algorithm (Greedy or GA), and click:
Optimize (Local) — runs within the GUI.
Optimize (JADE) — executes distributed coordination through JADE agents.


# License

For academic use under Swinburne University’s COS30018: Intelligent Systems.  
© 2025 — Multi-Agent VRP Optimisation Team.
