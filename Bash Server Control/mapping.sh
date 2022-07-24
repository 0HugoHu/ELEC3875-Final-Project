#!/usr/bin/env bash

rm -f my_map.pgm

rm -f my_map.yaml

rm -f my_map.png

rosrun map_server map_saver -f ~/Desktop/my_map

mogrify -format png my_map.pgm




