# ELEC3875-Final-Project

My undergraduate final project: Parking Robot based on 3D LiDAR. ELEC3875 / XJEL3875

**Keywords: Automatic Parking, SLAM, 3D Navigation, Remote Control, ROS, RRT**

# Introduction

This paper proposes **SLAM+**, an improved algorithm based on Google Cartographer, by adding IMU factors adapted from a state-of-the-art method LIO-SAM. IMU pre-integration allows measurements from multiple sources, and the pose can be calculated more accurately from a cheap 3D LiDAR.

This paper also introduces an optimized **RRT real-time dynamic 3D navigation** method. Using the 2D planar map + height information obtained in real-time from LiDAR sensors, this algorithm can navigate more safely by recognizing, e.g., speed bumps, moving people and limited height gates, frequently seen in parking lots. 

Finally, a Client-Server-Vehicle three-layer architecture is built to test the performance of this system. The optimized algorithms are thoroughly tested and perform well in the virtual scene by implementing the Velodyne VLP-16 3D LiDAR simulation and building model maps of an outdoor and an indoor scenario. With real-time obstacles detection and dynamic path planning, the robot can safely pass speed bumps and arched (limited height) gates. 

**The project highlights that user can remotely view the vehicle's 3D map building results and navigation status in real-time via WIFI on an Android application and control the vehicle to move or send commands to promptly go to the specified map location**. 


# Poster

![](https://s1.ax1x.com/2022/07/24/jjEDRP.png)

See the *Poster.pdf* in main directory for details.

# Preview
This is an Android App for remote SLAM and navigation. Further information could be found in *Presentation.pdf*.

![](https://media.giphy.com/media/iY50broOLNmjthST9n/giphy.gif)

![](https://s1.ax1x.com/2022/07/24/jjVrk9.png)
![](https://s1.ax1x.com/2022/07/24/jjVyf1.png)
![](https://s1.ax1x.com/2022/07/24/jjVsYR.png)
![](https://s1.ax1x.com/2022/07/24/jjVcSx.png)


