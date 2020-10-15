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
system=$(lsb_release -i --short)
echo "$system"
if [ $system = "Ubuntu" ];
then
	echo "当前系统为:$system"
	
	sed -i 's/'$oip'/'$nip'/g' /etc/network/interfaces
	sed -i 's/'$ogw'/'$ngw'/g' /etc/network/interfaces
	sed -i 's/'$onm'/'$nnm'/g' /etc/network/interfaces
	echo "已修改,正在重启网络.."
	sudo service networking restart
	#echo "已修改,正在重启程序"
	#/usr/tomcat/wincc/jfinal.sh stop
	#sleep 2
	#/usr/tomcat/wincc/jfinal.sh start
elif  [ $system = "CentOS" ];
then
	echo "当前系统为:$system"

	sed -i 's/'$oip'/'$nip'/g' /etc/sysconfig/network-scripts/ifcfg-eth0
	sed -i 's/'$ogw'/'$ngw'/g' /etc/sysconfig/network-scripts/ifcfg-eth0
	sed -i 's/'$onm'/'$nnm'/g' /etc/sysconfig/network-scripts/ifcfg-eth0
	echo "已修改,正在重启网络.."
	service network restart
	#echo "已修改,正在重启程序"
	#/usr/tomcat/wincc/jfinal.sh stop
	#sleep 2
	#/usr/tomcat/wincc/jfinal.sh start
else
	echo "未知系统"
fi
