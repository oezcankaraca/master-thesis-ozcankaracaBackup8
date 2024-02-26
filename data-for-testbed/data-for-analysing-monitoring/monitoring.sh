#!/bin/bash

# Set the monitoring interval
interval=1       

# Declare associative arrays to store CPU and memory usage sums and counts
declare -A cpu_usage_sum
declare -A mem_usage_sum
declare -A count

# Get the list of running Docker containers (names and IDs)
readarray -t containers < <(docker ps --format "{{.Names}} {{.ID}}")

# Initialize the usage and count arrays for each container
for container in "${containers[@]}"
do
    read name id <<< $container
    cpu_usage_sum[$name]=0
    mem_usage_sum[$name]=0
    count[$name]=0
done

# Perform measurements continuously
while true
do
    for container in "${containers[@]}"
    do
        read name id <<< $container

        # Check if the container is still running
        if ! docker ps -q | grep -qw $id; then
            echo "A container has stopped. Monitoring is being terminated."
            echo ""
            break 2  # Exit both loops
        fi

        # Retrieve and process the container stats
        stats=$(docker stats --no-stream --format "{{.CPUPerc}} {{.MemPerc}}" $id)

        read cpu mem <<< $stats
        cpu_usage=$(echo $cpu | tr -d '%' | sed 's/^$/0/')
        mem_usage=$(echo $mem | tr -d '%' | sed 's/^$/0/') 

        cpu_usage_sum[$name]=$(echo "${cpu_usage_sum[$name]} + $cpu_usage" | bc | awk '{printf "%.2f", $0}')
        mem_usage_sum[$name]=$(echo "${mem_usage_sum[$name]} + $mem_usage" | bc | awk '{printf "%.2f", $0}')
        count[$name]=$((count[$name] + 1))
    done

    sleep $interval
done

# Variables to store total average usage
total_avg_cpu=0
total_avg_mem=0

# Calculate and display the final average usage for each container
for container in "${containers[@]}"
do
    read name id <<< $container
    if [ ${count[$name]} -eq 0 ]; then
        continue 
    fi
    avg_cpu_usage=$(echo "scale=2; ${cpu_usage_sum[$name]} / ${count[$name]}" | bc | awk '{printf "%.2f", $0}')
    avg_mem_usage=$(echo "scale=2; ${mem_usage_sum[$name]} / ${count[$name]}" | bc | awk '{printf "%.2f", $0}')

    total_avg_cpu=$(echo "$total_avg_cpu + $avg_cpu_usage" | bc | awk '{printf "%.2f", $0}')
    total_avg_mem=$(echo "$total_avg_mem + $avg_mem_usage" | bc | awk '{printf "%.2f", $0}')

    echo "Container Name: $name"
    echo "Average CPU Usage: ${avg_cpu_usage} %"
    echo "Average Memory Usage: ${avg_mem_usage} %"
    echo ""
done

# Display total averages
echo "Total Average CPU Usage: $total_avg_cpu %"
echo "Total Average Memory Usage: $total_avg_mem %"
