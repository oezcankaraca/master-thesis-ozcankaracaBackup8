#!/bin/bash

printf "\nStarting Testbed.\n\n"

# run-testbed-automation.sh
export p2p_algorithm_used_values="false true"  # Sets an environment variable indicating if super peers are used
export number_of_peers_values="50"   # Sets an environment variable for the number of peers
export choice_of_pdf_mb_values="32"  # Sets an environment variable with different PDF file sizes

# Base directory path where all project-related files are located
export BASE_PATH="$HOME/Desktop"  # Sets an environment variable for the base path of project files

./configuration-testbed.sh  # Executes the configuration-testbed.sh script

printf "\nStopping Testbed.\n\n" 
