ban=$(lsb_release -a)
echo "当前系统为:${ban}" 
#!/bin/bash
#原始ip
oip=$1
#ip
nip=$2
#原始网关
ogw=$3
#网关
ngw=$4
#原始子关掩码
onm=$5
#子关掩码
nnm=$6
#wifi
wifi=$7
system=$(lsb_release -i --short)
echo "$system"
if [ $system = "Ubuntu" ];
then
	echo "当前系统为:$system"
	if [ $wifi = "true" ];
	then 
		echo "正在开启wifi.."
		sudo ifconfig wlp3s0 up
		sleep 3
		sed -i 's/'$oip'/'$nip'/g' /etc/network/interfaces
		sed -i 's/'$ogw'/'$ngw'/g' /etc/network/interfaces
		sed -i 's/'$onm'/'$nnm'/g' /etc/network/interfaces
	else
		echo "正在关闭wifi.."
		sudo ifconfig wlp3s0 down
	fi
	echo "已修改,正在重启网络"
	sudo service networking restart
	#echo "已修改,正在重启程序"
	#/usr/tomcat/wincc/jfinal.sh stop
	#sleep 2
	#/usr/tomcat/wincc/jfinal.sh start

elif  [ $system = "CentOS" ];
then
	echo "当前系统为:$system"
	if [ $wifi = "true" ];
	then
		echo "正在开启wifi.."
		/sbin/ifconfig wlan0 up
		sleep 3
		sed -i 's/'$oip'/'$nip'/g' /etc/sysconfig/network-scripts/ifcfg-wlan0
		sed -i 's/'$ogw'/'$ngw'/g' /etc/sysconfig/network-scripts/ifcfg-wlan0
		sed -i 's/'$onm'/'$nnm'/g' /etc/sysconfig/network-scripts/ifcfg-wlan0
	else
		echo "正在关闭wifi.."
		/sbin/ifconfig wlan0 down
	fi
	echo "已修改,正在重启网络"
	sudo service network restart
	#echo "已修改,正在重启程序"
	#/usr/tomcat/wincc/jfinal.sh stop
	#sleep 2
	#/usr/tomcat/wincc/jfinal.sh start

else
	echo "未知系统"
fi
