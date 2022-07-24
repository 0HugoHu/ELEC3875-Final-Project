#!/usr/bin/env bash

cd rviz_stream

scrot -b -z -q 75 'map_stream.jpg'

convert map_stream.jpg -crop 800x900+1000+150 map_stream.jpg


