#!/usr/bin/env bash

source ~/velodyne_ws/devel/setup.bash

echo "hello"

roslaunch velodyne_slam_simulation gazebo_turtlebot3.launch
