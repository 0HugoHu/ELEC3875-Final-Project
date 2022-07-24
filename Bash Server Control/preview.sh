#!/usr/bin/env bash

wmctrl -a Gazebo

scrot -b -z -o -q 75 'gazebo_rviz_preview.jpg'

mogrify -resize 50% gazebo_rviz_preview.jpg




